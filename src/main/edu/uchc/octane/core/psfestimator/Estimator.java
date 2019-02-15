package edu.uchc.octane.core.psfestimator;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;

public class Estimator {

	public final static int MAX_ITERATIONS = 1000;
	public final static double CONVERGENCE_DELTA = 1e-7;

	int scaleFactor;
	int [][][][] cachedAssignments = null;
	RectangularDoubleImage[] data;
	RectangularDoubleImage psf;
	int [] centerXs;
	int [] centerYs;
	double [] initBgs;
	double [] initIntensities;
	int psfHalfWidth;

	public Estimator(int scaleFactor, RectangularDoubleImage[] data, int [] centerXs, int [] centerYs, double [] bgs, double [] intensities ) {
		cachedAssignments = new int[scaleFactor][scaleFactor][][];
		this.scaleFactor = scaleFactor;
		this.data = data;
		this.centerXs = centerXs;
		this.centerYs = centerYs;
		this.initBgs = bgs;
		this.initIntensities = intensities;

		// int imgWidth = data[0].width;
		// psfHalfSize = (imgWidth - 1) / 2 * scaleFactor; // assuming imgWidth is odd
	}

	int [][] getAssignment(RectangularDoubleImage img, RectangularDoubleImage psf, int centerX, int centerY) {

		int offsetX = centerX % scaleFactor;
		int offsetY = centerY % scaleFactor;

		if (cachedAssignments[offsetX][offsetY] == null) {
			cachedAssignments[offsetX][offsetY] = makePixelAssignment(img, psf, centerX, centerY); 
		}

		return cachedAssignments[offsetX][offsetY];
	}

	int [][] makePixelAssignment(RectangularDoubleImage img, RectangularDoubleImage psf, int centerX, int centerY) {

		// psf has to have a (0,0) coordinate at the center
		// data has to be centered at (centerX / scaleFactor, centerY/scaleFactor) 

		int[][] assignment = new int[img.getLength()][];
		int [] tmp = new int[scaleFactor * scaleFactor];

		for (int i = 0; i < img.getLength(); i++) {

			int x = img.getXCordinate(i) * scaleFactor - centerX;
			int y = img.getYCordinate(i) * scaleFactor - centerY;

			int idx = 0;
			for (int yi = y; yi < y + scaleFactor; yi ++ ) {
				for (int xi = x; xi < x + scaleFactor; xi ++) {
					if (psf.isCoordinateValid(xi, yi)) {
						tmp[idx++] = psf.getIndexOfCoordinate(xi, yi);
					}
				}
			}

			assignment[i] = new int[idx];
			System.arraycopy(tmp, 0, assignment[i], 0, idx);
		}

		return assignment;
	}

	class Jacobian implements MultivariateJacobianFunction {

		@Override
		public Pair<RealVector, RealMatrix> value(RealVector point) {

			int imgSize = data[0].getLength();
			int psfLength = (psfHalfWidth * 2 + 1) * (psfHalfWidth * 2 + 1);
			RectangularDoubleImage psf = new RectangularDoubleImage(
					point.getSubVector(0, psfLength).toArray(),
					psfHalfWidth * 2 + 1,
					-psfHalfWidth, -psfHalfWidth);

			double [] values = new double[imgSize * data.length];
			BlockRealMatrix jacobian = new BlockRealMatrix(values.length, point.getDimension());

			for (int i = 0; i < data.length; i ++) {
				RectangularDoubleImage img = data[i];

				int [][] assignment = getAssignment(img, psf, centerXs[i], centerYs[i]);
				double bg = point.getEntry(psfLength + i);
				double intensity = point.getEntry(psfLength + data.length + i);					

				for (int j = 0; j < img.getLength(); j++)  {

					double s = 0;
					for (int k = 0; k < assignment[j].length; k++) {
						s += psf.getValue(assignment[j][k]);
						jacobian.setEntry(i * imgSize + j, assignment[j][k], intensity);
					}
					values[i * imgSize + j] = s * intensity + bg - img.getValue(j);

					jacobian.setEntry(i * imgSize + j, psfLength + i, 1);
					
					if (i != data.length -1) { // the last intensity point is fixed
						jacobian.setEntry(i * imgSize + j, psfLength + data.length + i, s);
					}
				}
			}

			return new Pair<RealVector, RealMatrix>(new ArrayRealVector(values), jacobian);
		} // value
	} // class Jacobian

	public RectangularDoubleImage estimate(int psfWidth, double sigma) {

		int imgSize = data[0].getLength();
		// int psfSize = (psfHalfSize * 2 + 1) * (psfHalfSize * 2 + 1);
		int psfLength = psfWidth * psfWidth;
		psfHalfWidth = (psfWidth -1) /2;
		double [] psfArray = new double[psfLength];
		RectangularDoubleImage psf = new RectangularDoubleImage(psfArray, psfWidth, -psfHalfWidth, -psfHalfWidth);
		
		double sigmaSqx2 = sigma * sigma * 2;
		for (int i = 0; i < psf.getLength(); i++) {
			int x = psf.getXCordinate(i);
			int y = psf.getYCordinate(i);
			psf.setValue(i, FastMath.exp(-(x * x + y * y)/sigmaSqx2) / sigmaSqx2 / FastMath.PI);
		}

		LeastSquaresBuilder builder = new LeastSquaresBuilder();

		builder.model(new Jacobian());
		
		double [] start = new double[psfLength + 2 * data.length]; 
		System.arraycopy(psfArray, 0, start, 0, psfLength);
		System.arraycopy(initBgs, 0, start, psfLength, initBgs.length);
		System.arraycopy(initIntensities, 0, start, psfLength + initBgs.length, initIntensities.length);
		builder.start(start);

		builder.target(new double[imgSize * data.length]);

		builder.maxEvaluations(MAX_ITERATIONS);
		builder.maxIterations(MAX_ITERATIONS);
		builder.checkerPair(new SimpleVectorValueChecker(CONVERGENCE_DELTA, CONVERGENCE_DELTA));

		LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
		Optimum opt = optimizer.optimize(builder.build());

		double [] result = opt.getPoint().toArray();
		System.arraycopy(result, 0, psfArray, 0, psfLength);

		return psf;
	}
}
