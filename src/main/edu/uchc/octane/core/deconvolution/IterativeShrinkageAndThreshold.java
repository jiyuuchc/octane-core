package edu.uchc.octane.core.deconvolution;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.PixelImageBase;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import edu.uchc.octane.core.utils.ImageFilters;

public class IterativeShrinkageAndThreshold {

    public final static int MAX_ITERATIONS = 50;
    public final static double CONVERGENCE_DELTA = 1e-6;

    public int maxIters;
    public PixelImageBase data;
    public double psfSigma;
    public int scaleFactor;
    public double ts, lambda;

    protected int imgWidth, imgHeight;
    protected double [] x;
    protected double [] y;
    protected double [] h;
    protected double t;

    public double[][] fittedParameters;

    double l2, l1, l0;
    static public class Params {
    	static final int X = 0;
    	static final int Y = 1;
    	static final int PARAMS_LENGTH = 2;
    }

    public IterativeShrinkageAndThreshold(double lambda) {
    	this(lambda, 1.00 / 2 / FastMath.PI,  500);
    }

    public IterativeShrinkageAndThreshold(double lambda, double ts, int maxIters) {
    	this.lambda = lambda;
    	this.ts = ts;
    	this.maxIters = maxIters;
    }

    public double[] fit(RectangularDoubleImage image, int scale, double sigma) {

    	this.data = image;
    	imgWidth = image.width * scale;
    	imgHeight = image.height * scale;
    	scaleFactor = scale;
    	psfSigma = sigma;

    	x = new double[imgWidth * imgHeight];

//    	if (hint.length > 0) {
//    		for (int i = 0; i < hint.length; i += 2) {
//    			x[hint[i+1] * scale * imgWidth + hint[i] * scale] = 1;
//    		}
//
//    	}

    	y = x.clone();

    	int filterSize = (int)(psfSigma * scaleFactor * 3 + 0.5) * 2 + 1;
    	filterSize = FastMath.min(FastMath.min(imgWidth, imgHeight), filterSize);
    	h = ImageFilters.makeGaussianFilter(psfSigma * scaleFactor, filterSize, true);

    	t = 1.0;
    	for (int i = 0; i < maxIters; i++) {

			iterate();

		}

    	return x;
    }

	protected void iterate() {

		double [] x0 = x.clone();
		// Ay
		double [] tmp = ImageFilters.symmetricFilter(h, y, imgWidth);

		// tmp2 = Ay - b
		//l2 = 0; l1=0; l0 = 0;
		for (int i = 0; i < imgHeight; i ++) {
			for (int j = 0; j < imgWidth; j ++) {
				tmp[i * imgWidth + j] -= data.getValueAtCoordinate(j / scaleFactor, i / scaleFactor) / scaleFactor / scaleFactor;
//				l2 += tmp[i * imgWidth + j] * tmp[i * imgWidth + j] * scaleFactor * scaleFactor;
//				if (x[i * imgWidth + j] != 0.0) {
//					l1 += lambda * x[i * imgWidth + j];
//					l0 += 1;
//				}
			}
		}

		// tmp = A^T(Ay - b)
		tmp = ImageFilters.symmetricFilter(h, tmp, imgWidth);

		// x = pL()
		for (int i = 0; i < tmp.length; i ++) {
			x[i] = y[i] - 2.0 * ts * tmp[i];
		}

		// soft threshold
//		System.out.println("X");
		for (int i = 0; i < x.length; i ++) {

			double tt = FastMath.abs(x[i]) - ts * lambda / scaleFactor / scaleFactor;

			if (tt <= 0.0){

				x[i] = 0;

			}
			else {

				x[i] = FastMath.signum(x[i]) * tt;

			}

//			if (i % imgWidth == 0) {
//				System.out.println();
//			}
//			System.out.print(x[i]);
//			System.out.print(",");
		}

		// update
		double t0 = t;

		t = (1 + FastMath.sqrt(1 + 4 * t0 * t0))/2;
//		System.out.println("Y");
		for (int i = 0; i < y.length; i ++) {

			y[i] = x[i] + (t0 - 1) / t * (x[i] - x0[i]);

//			if (i % imgWidth == 0) {
//				System.out.println();
//			}
//			System.out.print(y[i]);
//			System.out.print(",");

		}
	}
}
