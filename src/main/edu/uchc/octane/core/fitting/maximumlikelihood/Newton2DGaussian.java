package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class Newton2DGaussian implements Fitter {

	final Logger logger = LoggerFactory.getLogger(Newton2DGaussian.class);
	
	final String [] headers = {"x","y", "sigma", "intensity","bg", "err_x", "err_y", "logL"};
	static final double absTol = 1e-6;
	static final int maxIters = 500;

	PixelImageBase data;

	boolean iterate(RealVector p) {
		RealVector g = getDelta(p);
		p.add(g);
		boolean stop = true;
		for (int i = 0; i < 5; i+=1) {
			if (g.getEntry(i) > absTol) {
				stop = false;
				break;
			}
		}
		return stop;
	}

	double [] getCrb(RealVector p) {
		assert(p!= null);
		double h = p.getEntry(0);
		double b = p.getEntry(1);
		double x0 = p.getEntry(2);
		double y0 = p.getEntry(3);
		double s = p.getEntry(4);
		
		double I_xx = 0;
		double I_yy = 0;
		for (int k = 0; k < data.getLength(); k++) {
			double x = (double) data.getXCordinate(k);
			double y = (double) data.getYCordinate(k);
			double mu = FastMath.exp(-(x-x0)*(x-x0)/2/s/s)*FastMath.exp(-(y-y0)*(y-y0)/2/s/s);
			double lambda = h * mu + b;			
			double sx = (x - x0)/s;
			double sy = (y - y0)/s;
			I_xx += 1.0/lambda*h*mu*sx/s*h*mu*sx/s;
			I_yy += 1.0/lambda*h*mu*sy/s*h*mu*sy/s;
		}
		
		double [] results = new double[2];
		results[0] = 1.0/FastMath.sqrt(I_xx);
		results[1] = 1.0/FastMath.sqrt(I_yy);
		return results;
	}

	double getLogL(RealVector p) {
		assert(p!= null);
		double h = p.getEntry(0);
		double b = p.getEntry(1);
		double x0 = p.getEntry(2);
		double y0 = p.getEntry(3);
		double s = p.getEntry(4);
		
		double l = 0;
		for (int k = 0; k < data.getLength(); k++) {
			double x = (double) data.getXCordinate(k);
			double y = (double) data.getYCordinate(k);
			double mu = FastMath.exp(-(x-x0)*(x-x0)/2/s/s)*FastMath.exp(-(y-y0)*(y-y0)/2/s/s);
			double lambda = h * mu + b;			
			l += data.getValue(k) * FastMath.log(lambda) - lambda;
		}
		return l;
	}

	RealVector getDelta(RealVector p) { //p : h, b, x0, y0, s
		//double [] gr = new double [5];
		assert(p!= null);
		double h = p.getEntry(0);
		double b = p.getEntry(1);
		double x0 = p.getEntry(2);
		double y0 = p.getEntry(3);
		double s = p.getEntry(4);
		RealVector gr = MatrixUtils.createRealVector(new double[5]);
		RealMatrix he = MatrixUtils.createRealMatrix(5, 5);

		for (int k = 0; k < data.getLength(); k++) {
			double x = (double) data.getXCordinate(k);
			double y = (double) data.getYCordinate(k);
			double mu = FastMath.exp(-(x-x0)*(x-x0)/2/s/s)*FastMath.exp(-(y-y0)*(y-y0)/2/s/s);
			double lambda = h * mu + b;
			double sx = (x - x0)/s;
			double sy = (y - y0)/s;
			double ss = sx*sx + sy*sy;
			double kappa = data.getValue(k) / lambda - 1;
			gr.addToEntry(0, kappa * mu);
			gr.addToEntry(1, kappa);
			gr.addToEntry(2, kappa * h * mu / s * sx);
			gr.addToEntry(3, kappa * h * mu / s * sy);
			gr.addToEntry(4, kappa * h * mu / s * ss);
			
			double alpha = data.getValue(k) / lambda / lambda; 
			he.addToEntry(0, 0, alpha*mu*mu);
			he.addToEntry(1, 0, alpha*mu);
			he.addToEntry(1, 1, alpha);
			he.addToEntry(2, 0, alpha*h*mu/s*sx*mu - kappa*mu/s*sx);
			he.addToEntry(2, 1, alpha*h*mu/s*sx);
			he.addToEntry(2, 2, alpha*h*mu/s*sx*h*mu/s*sx - kappa*h*mu/s/s*(sx*sx-1));
			he.addToEntry(3, 0, alpha*h*mu/s*sy*mu - kappa*mu/s*sy);
			he.addToEntry(3, 1, alpha*h*mu/s*sy);
			he.addToEntry(3, 2, alpha*h*mu/s*sx*h*mu/s*sy - kappa*h*mu/s/s*sx*sy);
			he.addToEntry(3, 3, alpha*h*mu/s*sy*h*mu/s*sy - kappa*h*mu/s/s*(sy*sy-1));
			he.addToEntry(4, 0, alpha*h*mu/s*ss*mu - kappa*mu/s*ss);
			he.addToEntry(4, 1, alpha*h*mu/s*ss);
			he.addToEntry(4, 2, alpha*h*mu/s*ss*h*mu/s*sx - kappa*h*mu/s/s*(sx*ss-2*sx));
			he.addToEntry(4, 3, alpha*h*mu/s*ss*h*mu/s*sy - kappa*h*mu/s/s*(sy*ss-2*sy));
			he.addToEntry(4, 4, alpha*h*mu/s*ss*h*mu/s*ss - kappa*h*mu/s/s*(ss*ss-3*ss));
		}
		
		for (int i = 0; i < 5; i++) {
			for (int j = i + 1; j < 5; j++) {
				he.setEntry(i, j, he.getEntry(j, i));
			}
		}
		
		DecompositionSolver solver = new LUDecomposition(he).getSolver();
		return solver.solve(gr);
	}

	double [] guess() {
		if (data == null) {
			return null;
		}
		double x0 = 0;
		double y0 = 0;
		double mi = 1e8;
		double ma = -1;
		double ss = 0;
		for (int k = 0; k < data.getLength(); k++) {
			x0 += data.getXCordinate(k) * data.getValue(k);
			y0 += data.getYCordinate(k) * data.getValue(k);
			mi = FastMath.min(mi, data.getValue(k));
			ma = FastMath.max(ma, data.getValue(k));
			ss += data.getValue(k);
		}
		double [] guess = new double[5];
		guess[0] = x0 / ss;
		guess[1] = y0 / ss;
		guess[2] = 1.6;
		guess[4] = mi;
		guess[3] = ma-mi;
		return guess;		
	}

	@Override
	public double[] fit(PixelImageBase data, double[] start) { // start: x0, y0, s, h, b
		this.data = data;
		if (start == null) {
			start = guess();
		} else {
			if (start.length != 5) {
				throw new IllegalArgumentException("start vector size should be 5");
			}
		}
		RealVector p = new ArrayRealVector(5);
		p.setEntry(0, start[3]);
		p.setEntry(1, start[4]);
		p.setEntry(2, start[0]);
		p.setEntry(3, start[1]);
		p.setEntry(4, start[2]);

		int nIters = 0;
		while (nIters < maxIters) {
			boolean stop = true;			
			try {
				RealVector g = getDelta(p);
				p = p.add(g);

				for (int i = 0; i < 5; i+=1) {
					if (g.getEntry(i) > absTol) {
						stop = false;
						break;
					}
				}				
			} catch (MathIllegalArgumentException e) {
				logger.error("Fitting failed: " + e.getLocalizedMessage());
				return null;
			}

			if (stop) {
				break;
			}
			nIters ++;
		}
		if (nIters >= maxIters) {
			logger.error("Iterations exceeded limit: " + maxIters);
			return null;
		}

		double [] result = new double[headers.length];
		result[0] = p.getEntry(2);
		result[1] = p.getEntry(3);
		result[2] = p.getEntry(4);
		result[3] = p.getEntry(0);
		result[4] = p.getEntry(1);
		double [] errs = getCrb(p);
		result[5] = errs[0];
		result[6] = errs[1];
		result[7] = getLogL(p);

		return result; 
	}

	@Override
	public String[] getHeaders() {
		return headers;
	}

}
