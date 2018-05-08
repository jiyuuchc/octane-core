package edu.uchc.octane.core.drift;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jtransforms.fft.FloatFFT_2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.localizationimage.RasterizedLocalizationImage;

public class CorrelationEstimator {

	static final double smoothingBandWidth = 0.25;
	final Logger logger = LoggerFactory.getLogger(getClass());
	double maxDistance;
	double pixelSize;

    public PolynomialSplineFunction xFunction;
    public PolynomialSplineFunction yFunction;

	public CorrelationEstimator() {
		this(1600.0, 16.0);
	}

	public CorrelationEstimator(double maxDistance, double pixelSize) {
		this.maxDistance = maxDistance;
		this.pixelSize = pixelSize;
	}

	public void correct(OctaneDataFile data) {

		double [][] raw = data.data;
		for (int i = 0; i < raw[0].length; i++) {
			raw[1][i] += xFunction.value(raw[0][i]);
			raw[2][i] += yFunction.value(raw[0][i]);
		}
	}

	public void estimate(OctaneDataFile data, Rectangle roi, int numOfKeyFrames) {
		RasterizedLocalizationImage img = new RasterizedLocalizationImage(data, pixelSize);
		img.setRoi(roi);
		RasterizedLocalizationImage newImg;
		double [] driftX = new double[numOfKeyFrames];
		double [] driftY = new double[numOfKeyFrames];
		double [] keyFrames = new double[numOfKeyFrames];

		// assuming frame_col = 0;
		DoubleSummaryStatistics stat =  Arrays.stream(data.data[0]).summaryStatistics();
		int maxFrameNum = (int) stat.getMax();
		int framegroupSize = maxFrameNum / numOfKeyFrames;
		img.addViewFilter(0, new double[] {1, framegroupSize });
		keyFrames[0] = (1 + framegroupSize) / 2.0;
		for (int i = 1; i < numOfKeyFrames; i++) {
			logger.info("Processing key frame: " + i);
			double min = i * framegroupSize + 1;
			double max =  min + framegroupSize - 1;

			if ( i == numOfKeyFrames - 1 ) { max = maxFrameNum ;}
			newImg = new RasterizedLocalizationImage(data, pixelSize);
			newImg.setRoi(roi);
			newImg.addViewFilter(0, new double[] {min, max});

			keyFrames[i] = (min + max) / 2.0;
			int [] drift =  estimateDrift(img, newImg);
			driftX[i] = pixelSize * drift[0];
			driftY[i] = pixelSize * drift[1];
			// img = newImg;
		}

		logger.info("Interpolating... ");
        if(numOfKeyFrames < 4) {
            LinearInterpolator interpolator = new LinearInterpolator();
            xFunction = addLinearExtrapolationToBorders(interpolator.interpolate(keyFrames, driftX), (int) stat.getMin(), maxFrameNum);
            yFunction = addLinearExtrapolationToBorders(interpolator.interpolate(keyFrames, driftY), (int) stat.getMin(), maxFrameNum);
        } else {
            ModifiedLoess interpolator = new ModifiedLoess(smoothingBandWidth, 2);
            xFunction = addLinearExtrapolationToBorders(interpolator.interpolate(keyFrames, driftX), (int) stat.getMin(), maxFrameNum);
            yFunction = addLinearExtrapolationToBorders(interpolator.interpolate(keyFrames, driftY), (int) stat.getMin(), maxFrameNum);
        }
        logger.info("Interpolating: done ");
	}

	protected int [] estimateDrift(RasterizedLocalizationImage img1, RasterizedLocalizationImage img2) {
		short [] pixels1 = img1.getRendered();
		short [] pixels2 = img2.getRendered();
		int [] drift = new int[2];
		float [] rPixels1 = new float[pixels1.length * 2];
		float [] rPixels2 = new float[pixels2.length * 2];
		for (int i = 0 ; i < pixels1.length; i ++) {
			rPixels1[i] = pixels1[i];
			rPixels2[i] = pixels2[i];
		}

		FloatFFT_2D fft = new FloatFFT_2D(img1.getDimY(), img1.getDimX());
		fft.realForwardFull(rPixels1);
		fft.realForwardFull(rPixels2);
		for (int i = 0 ; i < rPixels1.length; i += 2) {
			float r = rPixels1[i] * rPixels2[i] + rPixels1[i+1] * rPixels2[i+1];
			float c = - rPixels1[i] * rPixels2[i+1] + rPixels1[i+1] * rPixels2[i];
			rPixels1[i] = r;
			rPixels1[i+1] = c;
			//rPixels1[i+1] = 0;
		}

		fft.complexInverse(rPixels1, false);
		float max = Float.MIN_VALUE;
		int idx = 0;
		for (int i = 0; i < rPixels1.length; i += 2) {
			if (max < rPixels1[i]) {
				max = rPixels1[i];
				idx = i;
			}
		}
		drift[0] = (idx / 2) % img1.getDimX();
		if (drift[0] >= img1.getDimX() / 2) { drift[0] -= img1.getDimX(); }
		drift[1] = (idx / 2) / img1.getDimX();
		if (drift[1] >= img1.getDimY() / 2) { drift[1] -= img1.getDimY(); }
		System.out.println("Drift: " + drift[0] + " - " + drift[1]);
		return drift;
	}

	public static PolynomialSplineFunction addLinearExtrapolationToBorders(PolynomialSplineFunction spline, int minFrame, int maxFrame) {
        PolynomialFunction[] polynomials = spline.getPolynomials();
        double[] knots = spline.getKnots();

        boolean addToBeginning = knots[0] != minFrame;
        boolean addToEnd = knots[knots.length - 1] != maxFrame;
        int sizeIncrease = 0 + (addToBeginning ? 1 : 0) + (addToEnd ? 1 : 0);
        if(!addToBeginning && !addToEnd) {
            return spline; //do nothing
        }

        //construct new knots and polynomial arrays
        double[] newKnots = new double[knots.length + sizeIncrease];
        PolynomialFunction[] newPolynomials = new PolynomialFunction[polynomials.length + sizeIncrease];
        //add to beginning
        if(addToBeginning) {
            //add knot
            newKnots[0] = minFrame;
            System.arraycopy(knots, 0, newKnots, 1, knots.length);
            //add function
            double derivativeAtFirstKnot = polynomials[0].derivative().value(0);
            double valueAtFirstKnot = spline.value(knots[0]);
            PolynomialFunction beginningFunction = new PolynomialFunction(new double[]{valueAtFirstKnot - (knots[0] - minFrame) * derivativeAtFirstKnot, derivativeAtFirstKnot});
            newPolynomials[0] = beginningFunction;
            System.arraycopy(polynomials, 0, newPolynomials, 1, polynomials.length);
        } else {
            System.arraycopy(knots, 0, newKnots, 0, knots.length);
            System.arraycopy(polynomials, 0, newPolynomials, 0, polynomials.length);
        }
        //add to end
        if(addToEnd) {
            //add knot
            newKnots[newKnots.length - 1] = maxFrame;
            //add function
            double derivativeAtLastKnot = polynomials[polynomials.length - 1].polynomialDerivative().value(knots[knots.length - 1] - knots[knots.length - 2]);
            double valueAtLastKnot = spline.value(knots[knots.length - 1]);
            PolynomialFunction endFunction = new PolynomialFunction(new double[]{valueAtLastKnot, derivativeAtLastKnot});
            newPolynomials[newPolynomials.length - 1] = endFunction;
        }

        return new PolynomialSplineFunction(newKnots, newPolynomials);

    }
}
