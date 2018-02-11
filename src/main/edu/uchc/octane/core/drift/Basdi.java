package edu.uchc.octane.core.drift;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.Localizations;
import edu.uchc.octane.core.datasource.RectangularImage;
import edu.uchc.octane.core.utils.ImageFilters;

public class Basdi {

	static final double DEFAULT_DRIFT_RESOLUTION = 32; // nm
	static final double DEFAULT_MAX_DRIFT = 1500; // nm
	static final double DEFAULT_LOCALIZATION_SIGMA = 50/2.35; //nm
	static final double DEFAULT_DRIFT_SIGMA = 3; //nm 
	static final double DEFAULT_CREEP_PROBABILITY = 0.01; // per frame
	static final int DEFAULT_MAX_ITERATIONS = 5;
	static final double DEFAULT_CONVERGENCE_EPS = 0.01; // relative (1%)

	static final double DARK_NOISE = 0.01; // per resolution pixel per frame
	static final double ANNEAL_SCALE_START = 2.4;
	static final double ANNEAL_SCALE_END = 1.2;
	static final double ANNEAL_SCALE_STEP = 0.4;

	public int maxIterations;
	public int maxShift;
	public double creepProbability;
	public double driftResolution;
	public double driftSigma, localizationSigma;

	int [][][] data;
	int [] intFrames;
	double[] annealingFilter; // for annealing
	double[] driftFilter; // for drift probability

	// all results
	public RectangularImage theta;
	public RectangularImage [] marginals;
	public double [][] drifts;
	
	public Basdi() {
		this(	DEFAULT_MAX_DRIFT,
				DEFAULT_LOCALIZATION_SIGMA,
				DEFAULT_DRIFT_SIGMA,
				DEFAULT_DRIFT_RESOLUTION,
				DEFAULT_CREEP_PROBABILITY,
				DEFAULT_MAX_ITERATIONS);
	}

	public Basdi(double maxShift, double localizationSigma, double driftSigma, double driftResolution, double creepProbability, int maxIterations) {

		this.maxIterations = maxIterations;
		this.driftResolution = driftResolution;
		this.maxShift = (int) FastMath.round(maxShift / driftResolution);
		this.localizationSigma = localizationSigma/ driftResolution;
		this.driftSigma = driftSigma / driftResolution;

		if ( this.maxShift <= 0 || this.localizationSigma <= 0 || this.maxIterations <= 0 || this.driftResolution <=0 || this.maxIterations <=0) {
			throw new IllegalArgumentException("Illegal negative parameters");
		}

		this.creepProbability = creepProbability;
	}


//	public void analyze(LocalizationDataset locData, double[] imgSize, int nTimePoints) {
//
//		imgWidth = (int) FastMath.ceil(imgSize[1] / driftResolution);
//		imgHeight = (int) FastMath.ceil(imgSize[0] / driftResolution);
//
//		theta = null;
//
//		doAnalysis(locData, nTimePoints);
//	}
//
//	public void analyze(LocalizationDataset locData, double [][] startingTheta, int nTimePoints) {
//
//		theta = startingTheta;
//
//		imgWidth = theta.length;
//		imgHeight = theta[0].length;
//
//		doAnalysis(locData, nTimePoints);
//	}

	protected void prepareData(Localizations locs, int numOfKeyFrames) {
		// data_ = new double [2][];
		double [] rawFrames = locs.getData(locs.frameCol);
		intFrames = new int[locs.getNumLocalizations()];
		int [] cnts = new int[numOfKeyFrames];
		
		//precounting;
		int maxFrame = (int) locs.getSummaryStatitics(locs.frameCol).getMax();
		int minFrame = (int) locs.getSummaryStatitics(locs.frameCol).getMin();
		int frameGroupSize = (maxFrame - minFrame + 1) / numOfKeyFrames;
		for (int i = 0; i < locs.getNumLocalizations(); i++) {
			double frame = rawFrames[i];
			int k = ((int) frame - minFrame) / frameGroupSize;
			if (k >= numOfKeyFrames) {
				k = numOfKeyFrames - 1; 
			}
			intFrames[i] = k;
			cnts[k] ++;
		}
		
		// longest dimension should always be the last index
		data = new int[numOfKeyFrames][][];
		for (int i = 0; i < numOfKeyFrames; i++) {
			data[i] = new int[2][cnts[i]];
		}
		Arrays.fill(cnts, 0);
		
		double [] xcol = locs.getData(locs.xCol);
		double [] ycol = locs.getData(locs.yCol);
		for (int i = 0; i < locs.getNumLocalizations(); i++) {
			int k = intFrames[i];
			data[k][0][cnts[k]] = (int) (xcol[i] / driftResolution);
			data[k][1][cnts[k]] = (int) (ycol[i] / driftResolution);
			cnts[k] ++;
		}
	}

	// needs this.intFrame, this.drift
	protected void correctDrift(Localizations locs) {
		double [] xcol = locs.getData(locs.xCol);
		double [] ycol = locs.getData(locs.yCol);
		
		for (int i = 0; i < locs.getNumLocalizations(); i++) {
			int k = intFrames[i];
			xcol[i] += driftResolution * drifts[0][k];
			ycol[i] += driftResolution * drifts[1][k];
		}
	}

	// needs this.theta, this.data; modified this.theta, this.marginals, this.drifts
	public void optimizeWithAnnealing() {
		double scale = ANNEAL_SCALE_START;
		int round = 0;
		while (scale >= ANNEAL_SCALE_END ) {
			round ++;
			int fs = (int) FastMath.round(FastMath.exp(scale));
			annealingFilter = new double[fs];
			Arrays.fill(annealingFilter, 1.0);
			System.out.println("Round - " + round);
			optimizeWithEM();
			scale -= ANNEAL_SCALE_STEP;
		}
	}
	
	// needs this.theta, this.data; modified this.theta, this.marginals, this.drifts
	protected void optimizeWithEM() {
		boolean converged = false;
		int iter = 0;

		do {
			iter ++;
			
			System.out.println("E Step - " + iter);
			theta = ImageFilters.symmetricFilter(annealingFilter, theta);
			RectangularImage[] probs = exy(theta, data);
			marginals = forBack(probs);
			
			System.out.println("M Step - " + iter);
			updateTheta(theta, marginals, data);
			
			double [][] oldDrifts = drifts;
			drifts = calculateAvgDrift(marginals);
			converged = testConvergence(oldDrifts, drifts);

		} while (! converged && iter ++ < maxIterations);
	}		
	
	protected boolean testConvergence(double [][] oldDrifts, double [][] newDrifts) {
		double cvge2 = DEFAULT_CONVERGENCE_EPS * DEFAULT_CONVERGENCE_EPS;
		for (int d = 0; d < oldDrifts.length; d ++) {
			double [] oldDrift = oldDrifts[d];
			double [] newDrift = newDrifts[d];
			int sum = 0, sum0 = 0;
			for (int i = 0; i < oldDrift.length; i ++) {
				sum += (oldDrift[i] - newDrift[i]) * (oldDrift[i] - newDrift[i]);
				sum0 += oldDrift[i] * oldDrift[i];
			}
			if (sum / sum0 > cvge2) {
				return false;
			}
		}
		return true;
	}

	protected double [][] calculateAvgDrift(RectangularImage[] marginalProb) {

		double [] avgDriftX = new double[marginalProb.length];
		double [] avgDriftY = new double[marginalProb.length];
		for (int k = 0; k < marginalProb.length; k++) {

			RectangularImage gn = marginalProb[k];
			double cx = 0, cy = 0;

			for (int idx = 0; idx < gn.getLength(); idx ++) {
				cx += gn.getValueVector()[idx] * gn.getXCordinate(idx);
				cy += gn.getValueVector()[idx] * gn.getYCordinate(idx);
			}

			avgDriftX[k] = -cx;
			avgDriftY[k] = -cy;
		}
		double [][] avgDrifts = new double[2][];
		avgDrifts[0] = avgDriftX;
		avgDrifts[1] = avgDriftY;
		return avgDrifts;
	}

	protected void updateThetaOneFrame(RectangularImage theta, RectangularImage mpk, int [][] dk) {
		double [] thetaValues = theta.getValueVector();
		double[] mpkv = mpk.getValueVector();
		int [] dkx = dk[0];
		int [] dky = dk[1];
		for (int idx = 0; idx < mpkv.length; idx ++) {
			int xpre = mpk.getXCordinate(idx);
			int ypre = mpk.getYCordinate(idx);
			for (int c = 0; c < dkx.length; c ++) {
				int xpost = xpre + dkx[c];
				int ypost = ypre + dky[c];
				int thetaIdx = theta.getIndexOfCoordinate(xpost, ypost);
				thetaValues[thetaIdx] += mpkv[idx];
			}
		}
	}

	protected void updateTheta(RectangularImage theta, RectangularImage[] marginalProb, int [][][] data) {

		RectangularImage gn = marginalProb[0];

		double cx = 0, cy = 0;
		for (int idx = 0; idx < gn.getLength(); idx ++) {
			cx += gn.getValueVector()[idx] * gn.getXCordinate(idx);
			cy += gn.getValueVector()[idx] * gn.getYCordinate(idx);
		}
		int icx = (int) FastMath.round(cx);
		int icy = (int) FastMath.round(cy);

		// expand the image by 2 x maxShift, so that the convolution will not be out of bound
		double [] values = new double[(theta.height + 2 * maxShift) * (theta.width + 2 * maxShift)];
		RectangularImage theta0 = new RectangularImage(values, theta.width + 2 * maxShift, theta.x0 - maxShift, theta.y0 - maxShift);
		IntStream.range(0,data.length).parallel().forEach(k -> {
			RectangularImage img = new RectangularImage(new double[theta0.getLength()], theta0.width, theta0.x0, theta0.y0); 
			updateThetaOneFrame(img, marginalProb[k], data[k]); 
			synchronized(theta0) {
				for (int idx = 0; idx < img.getLength(); idx ++) {
					theta0.setValue(idx, theta0.getValue(idx) + img.getValue(idx));
				}
			}
		});

		//crop the image back to original size
		RectangularImage thetaNew = new RectangularImage(theta0, theta0.x0 + icx, theta0.y0 + icy, theta.width, theta.height);
		theta.setValueVector(thetaNew.getValueVector());
	}

	/*
		compute P(dx,dy|theta,O) for each individual frame as a function of drift d
	 */
	protected RectangularImage [] exy(RectangularImage theta, int [][][] data) {

		// initialize
		RectangularImage logTheta = theta.clone();
		double [] values = logTheta.getValueVector();
		for (int i = 0; i < values.length; i++) {
			values[i] = FastMath.log(values[i] + DARK_NOISE);
		}

		RectangularImage [] localProb = new RectangularImage[data.length];
		IntStream.range(0, data.length).parallel().forEach( f -> {
			localProb[f] = exyf2(logTheta, data[f]);
		});
		
		return localProb;
	}

	/*
			compute e(dx, dy) = P(dx,dy|theta,o)
	 */
	protected RectangularImage exyf2(RectangularImage logTheta, int [][] o) {

		int matrixSize = maxShift * 2 + 1;
		RectangularImage e0 = new RectangularImage(
				new double[matrixSize * matrixSize], matrixSize, -maxShift, -maxShift);

		if (o != null && o[0] != null && o[0].length != 0) {
		
			// e = theta cov image(o)
			double maxLog = Double.MIN_VALUE;
			int dataLen = o[0].length;
			int [] ox = o[0];
			int [] oy = o[1];
			for (int drow = -maxShift; drow <= maxShift; drow++ ) {
				for (int dcol = -maxShift; dcol <= maxShift ; dcol++ ) {
					int e0Idx = e0.getIndexOfCoordinate(dcol, drow);
					double curValue = 0;
					for (int idx = 0; idx < dataLen; idx ++) {
						int col = (int) ox[idx] + dcol;
						int row = (int) oy[idx] + drow;
						if (logTheta.isCoordinateValid(col, row)) {
							curValue += logTheta.getValueAtCoordinate(col, row);
						}
					}
					e0.setValue(e0Idx, curValue);
					if (curValue > maxLog) {
						maxLog = curValue;
					}
				}
			}

			//convert back to linear scale
			double sum = 0;
			for (int idx = 0; idx < e0.getLength(); idx ++) {
				double linearValue = FastMath.exp(e0.getValue(idx) - maxLog);
				e0.getValueVector()[idx] = linearValue;
				sum += linearValue;
			}

			//normalization
			for (int idx = 0; idx < e0.getLength(); idx ++) {
				e0.getValueVector()[idx] /= sum;
			}
		} else {
			double eps = 1.0 / matrixSize / matrixSize;
			Arrays.fill(e0.getValueVector(), eps);
		}
		return e0;
	}

	/*
	% forward_backward alogorithm for computing marginal probability
	% of Markovian process
	 */
	protected RectangularImage[] forBack(RectangularImage[] localProb) {
		//forward
		RectangularImage [] a = new RectangularImage[localProb.length];
		RectangularImage [] b = new RectangularImage[localProb.length];
		//double [] as = new double[localProb.length];
		//double [] bs = new double[localProb.length];

		double eps = creepProbability / a[0].getLength(); 
		// copy exy[0][][] to a[0][][]
		a[0] = localProb[0].clone();
		for (int k = 1; k < localProb.length; k ++) {
			a[k] = ofsFilter2(driftFilter, a[k-1], eps);
			double [] akv = a[k].getValueVector();
			double max = Double.MIN_VALUE;
			for (int idx = 0 ; idx < akv.length; idx ++) {
				akv[idx] *= localProb[k].getValue(idx);
				if (akv[idx] > max) {
					max = akv[idx];
				}
			}
			for (int idx = 0 ; idx < akv.length; idx ++) {
				akv[idx] /= max;
			}
			//as[k] = as[k-1] + FastMath.log(max);
		}

		//backward
		double [] bv = new double[a[0].getLength()];
		Arrays.fill(bv, 1.0);
		b[b.length-1] = new RectangularImage(bv, a[0].width);
		for (int k = b.length - 2; k >=0; k--) {
			b[k] = b[k+1].clone();
			double [] bkv = b[k].getValueVector();
			for (int idx = 0 ; idx < bkv.length; idx ++) {
				bkv[idx] *= localProb[k].getValue(idx);
			}
			b[k] = ofsFilter2(driftFilter, b[k], eps);
			double max = Double.MIN_VALUE;			
			for (int idx = 0 ; idx < bkv.length; idx ++) {
				if (bkv[idx] > max) {
					max = bkv[idx];
				}
			}
			for (int idx = 0 ; idx < bkv.length; idx ++) {
				bkv[idx] /= max;
			}
			//bs[k] = bs[k+1] + FastMath.log(max);
		}

		//combine
		double sum;
		for (int k = 0; k < a.length; k ++) {
			sum = 0;
			double [] akv = a[k].getValueVector();
			double [] bkv = b[k].getValueVector();
			for (int idx = 0; idx < akv.length; idx ++) {
				akv[idx] *= bkv[idx];
				sum += akv[idx];
			}
			for (int idx = 0; idx < akv.length; idx ++) {
				akv[idx] /= sum;
			}
		}
		return a;
	}

	// result = Filter(H + eps, a)	
	protected RectangularImage ofsFilter2(double [] h, RectangularImage a, double eps) {
		
		RectangularImage result = ImageFilters.symmetricFilter(h, a);
		
		if (eps != 0) { 
			for (int idx = 0; idx < result.getLength(); idx ++) {
				result.getValueVector()[idx] += eps;
			}
		}
		return result;
	}

//	protected double [] makeGaussianFilter(double sigma, int size) {
//
//		double [] filter = new double[size];
//
//		double s = 0;
//		for (int i = 0; i < size; i ++) {
//
//			double x = -(double)(size - 1)/2.0 + i;
//			filter[i] = FastMath.exp((- x * x) / (2 * sigma * sigma));
//			s += filter[i];
//
//		}
//
//		for (int i = 0; i < size; i ++) {
//
//			filter[i] /= s;
//
//		}
//
//		return filter;
//	}
 }
