package edu.uchc.octane.core.drift;

public class Basdi {

//	static final double DEFAULT_DRIFT_RESOLUTION = 32;
//	static final double DEFAULT_MAX_DRIFT = 1600;
//	static final double DEFAULT_LOCALIZATION_SIGMA = 50/2.35;
//	static final double DEFAULT_DRIFT_SIGMA = 3;
//	static final double DEFAULT_CREEP_PROBABILITY = 0.01;
//	static final int MAX_ITERATIONS = 10;
//	static final double CONVERGENCE_EPS = 0.01;
//
//	static final double BG = 0.1;
//
//	int maxIterations;
//	int maxShift;
//	int imgWidth, imgHeight;
//	double creepProbability;
//	double driftResolution;
//	double driftSigma, localizationSigma;
//
//	double[] gaussianFilter1; // for localization uncertainty
//	double[] gaussianFilter2; // for drift probability
//
//	double [][] theta;
//	SimpleLocalization [][] data;
//	double [][][] localProb;
//	double [][][] marginalProb;
//	double [] avgDriftX, avgDriftY;
//
//	public class SimpleLocalization {
//		int x;
//		int y;
//	}
//
//	public Basdi() {
//		this(	DEFAULT_MAX_DRIFT,
//				DEFAULT_LOCALIZATION_SIGMA,
//				DEFAULT_DRIFT_SIGMA,
//				DEFAULT_DRIFT_RESOLUTION,
//				DEFAULT_CREEP_PROBABILITY,
//				MAX_ITERATIONS);
//	}
//
//	public Basdi(double maxShift, double localizationSigma, double driftSigma, double driftResolution, double creepProbability, int maxIterations) {
//
//		this.maxIterations = maxIterations;
//
//		this.maxShift = (int) FastMath.round(maxShift / driftResolution);
//		this.localizationSigma = localizationSigma/ driftResolution;
//		this.driftSigma = driftSigma / driftResolution;
//
//		if ( this.maxShift <= 0 || localizationSigma <= 0 || maxIterations <= 0) {
//			throw new IllegalArgumentException("Illegal negative parameters");
//		}
//
//		this.creepProbability = creepProbability;
//	}
//
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
//
//	protected void doAnalysis(LocalizationDataset locData, int nTimePoints) {
//
//		int stepSize = (int) FastMath.ceil((double)data.length / nTimePoints);
//		gaussianFilter1 = makeGaussianFilter(localizationSigma, FastMath.min(3, (int)(localizationSigma * 6)));
//		gaussianFilter2 = makeGaussianFilter(driftSigma, FastMath.min(3, (int)(driftSigma * FastMath.sqrt(stepSize)* 6)));
//
//		data = new SimpleLocalization[nTimePoints][];
//
//		for (int i = 0 ; i <= nTimePoints; i ++) {
//
//			int cnt = 0;
//			for (int f = i * stepSize + 1; f <= i * stepSize + stepSize; f++) {
//
//				Localization[] list = locData.getAllLocalizationsAtFrame(f);
//				if (list == null || list.length == 0) {
//					continue;
//				}
//
//				cnt += list.length;
//
//			}
//
//			data[i] = new SimpleLocalization[cnt];
//			int idx = 0;
//			for (int f = i * stepSize + 1; f <= i * stepSize + stepSize; f++) {
//
//				Localization[] list = locData.getAllLocalizationsAtFrame(f);
//				if (list == null) {
//					continue;
//				}
//
//				for (int m = 0; m < list.length; i++) {
//					data[i][idx].x = (int)(list[m].x / driftResolution);
//					data[i][idx].y = (int)(list[m].y / driftResolution);
//					idx ++;
//				}
//
//			}
//
//		}
//
//		if (theta == null ) {
//
//			theta = new double[imgHeight][imgWidth];
//
//			for (int i = 0; i < data.length; i++) {
//				for (int j = 0; j < data[i].length; j++) {
//					theta[data[i][j].y][data[i][j].x] += 1.0;
//				}
//			}
//		}
//
//		avgDriftX = new double[nTimePoints];
//		avgDriftY = new double[nTimePoints];
//
//		boolean converged = false;
//		int iter = 0;
//
//		do {
//
//			iterate();
//			double [] oldDriftX = avgDriftX.clone();
//			double [] oldDriftY = avgDriftY.clone();
//			calculateAvgDrift();
//			converged = testConvergence(oldDriftX, oldDriftY);
//
//		} while (! converged && iter ++ < maxIterations);
//	}
//
//	protected boolean testConvergence(double [] x, double [] y) {
//
//		return (meanDifference(x, avgDriftX) < CONVERGENCE_EPS
//				&& meanDifference(y, avgDriftY) < CONVERGENCE_EPS );
//	}
//
//	protected void calculateAvgDrift() {
//
//		for (int k = 0; k < marginalProb.length; k++) {
//
//			double [][] gn = marginalProb[k];
//			double cx = 0, cy = 0, s = 0;
//
//			for (int i = 0; i < maxShift * 2 + 1; i ++) {
//				for (int j = 0; j < maxShift * 2 + 1; j ++) {
//					cx += gn[i][j] * j;
//					cy += gn[i][j] * i;
//				}
//			}
//
//			avgDriftX[k] = -cx / s;
//			avgDriftY[k] = -cy / s;
//		}
//	}
//
//	protected void iterate() {
//
//		// E step
//		theta = ofsFilter2(gaussianFilter1, theta, 0);
//		exy();
//		forBack();
//
//		// M step
//		updateTheta();
//	}
//
//	protected void updateTheta() {
//
//		double[][] gn = marginalProb[0];
//
//		double cx = 0, cy = 0, s = 0;
//		for (int i = 0; i < maxShift * 2 + 1; i ++) {
//			for (int j = 0; j < maxShift * 2 + 1; j ++) {
//				cx += gn[i][j] * j;
//				cy += gn[i][j] * i;
//				s += gn[i][j];
//			}
//		}
//		cx /= s;
//		cy /= s;
//
//		double [][] theta0 = new double[imgHeight + maxShift][imgWidth + maxShift];
//
//		for (int k = 0; k < marginalProb.length; k ++) {
//			for (int idx = 0; idx < data[k].length; idx ++) {
//				for (int y = 0; y < maxShift; y ++) {
//					for (int x = 0; x < maxShift; x ++) {
//						theta0[data[k][idx].y + y][data[k][idx].x + x] += marginalProb[k][maxShift - y - 1][maxShift - x - 1];
//					}
//				}
//			}
//		}
//
//		int x0 = maxShift + (int) FastMath.round(cx);
//		int y0 = maxShift + (int) FastMath.round(cy);
//
//		for (int i = 0; i < imgHeight; i ++) {
//			System.arraycopy(theta0[i + y0], x0, theta[i], 0, imgWidth);
//		}
//
//	}
//
//	/*
//		compute P(dx,dy|theta,O) for each individual frame as a function of drift d
//	 */
//	protected void exy() {
//
//		// initialize
//		double[][] logTheta = new double[imgHeight][imgWidth];
//		for (int row = 0; row < imgHeight; row ++) {
//			for (int col = 0; col < imgWidth; col ++) {
//				logTheta[row][col] = FastMath.log(theta[row][col] + BG); // add a small chance for bg noise
//			}
//		}
//
//		for (int k = 0; k < data.length; k++) {
//
//			SimpleLocalization [] o = data[k];
//
//			if (o == null || o.length == 0) {
//				continue;
//			}
//
//			localProb[k] = exyf2(logTheta, o);
//		}
//
//	}
//
//
//	/*
//			compute e(dx, dy) = P(dx,dy|theta,o)
//	 */
//	protected double[][] exyf2(double[][] logTheta, SimpleLocalization [] o) {
//
//		double[][] e0 = new double[maxShift * 2 + 1][maxShift * 2 + 1];
//
//		// e = theta cov image(o)
//		double maxLog = Double.MIN_VALUE;
//		for (int drow = -maxShift; drow <= maxShift ; drow++ ) {
//			for (int dcol = -maxShift; dcol <= maxShift ; dcol++ ) {
//
//				for (int idx = 0; idx < o.length; idx ++) {
//					int col = o[idx].x + dcol;
//					int row = o[idx].y + drow;
//					if (col > 0 && row > 0 && col < imgWidth && row < imgHeight) {
//						e0[maxShift + drow][maxShift + dcol] += logTheta[row][col];
//					}
//				}
//
//				if (e0[maxShift + drow][maxShift + dcol] > maxLog) {
//					maxLog = e0[maxShift + drow][maxShift + dcol];
//				}
//			}
//		}
//
//		//convert back to linear scale
//		double sum = 0;
//		for (int  row = 0; row <= maxShift * 2 + 1 ; row++ ) {
//			for (int col = 0; col <= maxShift * 2 + 1 ; col++ ) {
//				e0[row][col] = FastMath.exp(e0[row][col] - maxLog);
//				sum += e0[row][col];
//			}
//		}
//
//		//normalization
//		for (int  row = 0; row <= maxShift * 2 + 1 ; row++ ) {
//			for (int col = 0; col <= maxShift * 2 + 1 ; col++ ) {
//				e0[row][col] /= sum;
//			}
//		}
//
//		return e0;
//	}
//
//	/*
//	% function [g, g_s] = for_back(e, p)
//	% forward_backward alogorithm for computing marginal probability
//	% of Markovian process
//	 */
//	protected void forBack() {
//
//		//forward
//		double [][][] a = new double[localProb.length][maxShift * 2 + 1][maxShift * 2 + 1];
//		double [][][] b = new double[localProb.length][maxShift * 2 + 1][maxShift * 2 + 1];
//
//		// copy exy[0][][] to a[0][][]
//		for (int i = 0; i < maxShift * 2 + 1; i ++) {
//			System.arraycopy(localProb[0][i], 0, a[0][i], 0, localProb[0][1].length);
//		}
//		for (int k = 1; k < localProb.length; k ++) {
//			double [][] at = ofsFilter2(gaussianFilter2, a[k-1], creepProbability / imgWidth / imgHeight);
//			for (int row = 0; row < maxShift * 2 + 1; row ++) {
//				for (int col = 0; col < maxShift * 2 + 1; col ++) {
//					a[k][row][col] = at[row][col] * localProb[k][row][col];
//				}
//			}
//		}
//
//		//backward
//		for (int i = 0; i < maxShift * 2 + 1; i++) {
//			Arrays.fill(b[b.length - 1][i], 1.0);
//		}
//		for (int k = b.length - 2; k >=0; k--) {
//			double [][] bt = ofsFilter2(gaussianFilter2, b[k+1], creepProbability / imgWidth / imgHeight);
//			for (int row = 0; row < maxShift * 2 + 1; row ++) {
//				for (int col = 0; col < maxShift * 2 + 1; col ++) {
//					b[k][row][col] = bt[row][col] * localProb[k][row][col];
//				}
//			}
//		}
//
//		//combine
//		double sum;
//		for (int k = 0; k < a.length; k ++) {
//
//			sum = 0;
//			for (int row = 0; row < maxShift * 2 + 1; row ++) {
//				for (int col = 0; col < maxShift * 2 + 1; col ++) {
//					marginalProb[k][row][col] *= b[k][row][col];
//					sum += marginalProb[k][row][col];
//				}
//			}
//
//			for (int row = 0; row < maxShift * 2 + 1; row ++) {
//				for (int col = 0; col < maxShift * 2 + 1; col ++) {
//					marginalProb[k][row][col] /= sum;
//				}
//			}
//
//		}
//	}
//
//	// filter a with linear filter h column and row wise.
//	// useful for separatable 2d filtering such as gaussian filter
//	// result = Filter(H + eps, a)
//	protected double[][] ofsFilter2(double[] h, double[][] a, double eps) {
//
//		double[][] result = new double[a.length][a[0].length];
//		double[][] result2 = new double[a.length][a[0].length];
//
//		int halfSizeL, halfSizeR;
//
//		halfSizeL = h.length / 2;
//		halfSizeR = h.length - halfSizeL;
//
//		//row-wise
//		double s, sa = 0;
//		for (int i = 0; i < a.length; i ++) {
//			for (int j = 0; j < a[0].length; j++) {
//
//				s = 0;
//				for (int k = FastMath.max(0, j - halfSizeL); k < FastMath.min(a[0].length, j + halfSizeR); k++) {
//					s += a[i][k] * h[halfSizeL + k - j];
//				}
//				result[i][j] = s;
//
//				sa += a[i][j]; //also calculate sum(a(:))
//
//			}
//		}
//
//		//column-wise
//		for (int j = 0; j < a[0].length; j ++) {
//			for (int i = 0; i < a.length; i++) {
//
//				s = 0;
//				for (int k = FastMath.max(0, i - halfSizeL); k < FastMath.min(a.length, i + halfSizeR); k++) {
//					s += result[k][j] * h[halfSizeL + k - i];
//				}
//				result2[i][j] = s;
//
//				result2[i][j] += sa * eps;
//
//			}
//		}
//
//		return result2;
//	}
//
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
