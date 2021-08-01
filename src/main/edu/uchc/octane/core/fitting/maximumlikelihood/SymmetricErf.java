package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class SymmetricErf implements LikelihoodModel {

	final static double sqrt2pi = FastMath.sqrt(2 * FastMath.PI);
	final static double sqrt2 = FastMath.sqrt(2);

	// double x0, y0, z0, bg0, in0;
	final String [] headers = {"x","y","sigma","intensity","bg"};

	PixelImageBase data;
	int xmin, xmax, ymin, ymax;

	public SymmetricErf() {
	}
	
	public void setData(PixelImageBase data) {
		this.data = data;
		xmin = Integer.MAX_VALUE;
		ymin = Integer.MAX_VALUE;
		xmax = Integer.MIN_VALUE;
		ymax = Integer.MIN_VALUE;
		for (int k = 0; k < data.getLength(); k++) {
			int x = data.getXCordinate(k);
			int y = data.getYCordinate(k);
			xmax = FastMath.max(xmax, x);
			xmin = FastMath.min(xmin, x);
			ymax = FastMath.max(ymax, y);
			ymin = FastMath.min(ymin, y);
		}
	}

	@Override
	public String [] getHeaders() {
		return headers;
	}

    public ObjectiveFunction getObjectiveFunction() {
    	return new ObjectiveFunction(new MultivariateFunction() {

			@Override
			public double value(double[] point) {
				double x0 = point[0];
				double y0 = point[1];
				double s0 = point[2];
				double in0 = point[3]; 
				double bg0 = point[4];
				
				double [] dex = new double[xmax-xmin+1];
				double [] dey = new double[ymax-ymin+1];

				double f = 0;
				for (int k = 0; k < data.getLength(); k ++) {
					int x = data.getXCordinate(k);
					int y = data.getYCordinate(k);
					
					// cache some values for speed.
					if (dex[x-xmin] == 0) {
						double xp = ((double)x - x0 + 0.5) / sqrt2 / s0;
						double xm = ((double)x - x0 - 0.5) / sqrt2 / s0;
						dex[x-xmin] = 0.5 * erf(xm, xp);
					}
					if (dey[y-ymin] == 0) {
						double yp = (y - y0 + 0.5) / sqrt2 / s0;
						double ym = (y - y0 - 0.5) / sqrt2 / s0;
						dey[y-ymin] = 0.5 * erf(ym, yp);    		
					}
					double mu = in0 * dex[x-xmin] * dey[y-ymin] + bg0;
					f += data.getValue(k) * FastMath.log(mu) - mu;
				}

				return f;
			}
    	});
    }
    
    public ObjectiveFunctionGradient getObjectiveFunctionGradient() {
    	return new ObjectiveFunctionGradient( new MultivariateVectorFunction() {

			@Override
			public double[] value(double[] point) throws IllegalArgumentException {

				double x0 = point[0];
				double y0 = point[1];
				double s0 = point[2];
				double in0 = point[3]; 
				double bg0 = point[4];

				double [] dex = new double[xmax-xmin+1];
				double [] dey = new double[ymax-ymin+1];
				double [] e_xm_sq = new double[xmax-xmin+1];
				double [] e_xp_sq = new double[xmax-xmin+1];
				double [] e_ym_sq = new double[ymax-ymin+1];
				double [] e_yp_sq = new double[ymax-ymin+1];

				double [] g = new double[5];

				for (int k = 0; k < data.getLength(); k++) {
					int x = data.getXCordinate(k);
					int y = data.getYCordinate(k);
					if (dex[x-xmin] == 0) {
						double xp = ((double)x - x0 + 0.5) / sqrt2 / s0;
						double xm = ((double)x - x0 - 0.5) / sqrt2 / s0;
						dex[x-xmin] = 0.5 * erf(xm, xp);
						e_xp_sq[x-xmin] = 1.0/s0/sqrt2pi*FastMath.exp(-xp*xp);
						e_xm_sq[x-xmin] = 1.0/s0/sqrt2pi*FastMath.exp(-xm*xm);
					}
					if (dey[y-ymin] == 0) {
						double yp = ((double)y - y0 + 0.5) / sqrt2 / s0;
						double ym = ((double)y - y0 - 0.5) / sqrt2 / s0;
						dey[y-ymin] = 0.5 * erf(ym, yp);
						e_yp_sq[y-ymin] = 1.0/s0/sqrt2pi*FastMath.exp(-yp*yp);
						e_ym_sq[y-ymin] = 1.0/s0/sqrt2pi*FastMath.exp(-ym*ym);
					}
//					double xp = (x - x0 + 0.5) / sqrt2 / s0;
//					double xm = (x - x0 - 0.5) / sqrt2 / s0;
//					double yp = (y - y0 + 0.5) / sqrt2 / s0;
//					double ym = (y - y0 - 0.5) / sqrt2 / s0;
//					double dex = 0.5 * erf(xm, xp);
//					double dey = 0.5 * erf(ym, yp);
					double mu = in0 * dex[x-xmin] * dey[y-ymin] + bg0;
					double dFMu = data.getValue(k) / mu - 1;
//					double dMuX0 = - in0*dey/s0/sqrt2pi*(FastMath.exp(-xp*xp)-FastMath.exp(-xm*xm));
//					double dMuY0 = - in0*dex/s0/sqrt2pi*(FastMath.exp(-yp*yp)-FastMath.exp(-ym*ym));
//					double dDeyS0 = (y-y0-0.5)/s0/s0/sqrt2pi*FastMath.exp(-ym*ym) - (y-y0+0.5)/s0/s0/sqrt2pi*FastMath.exp(-yp*yp);
//					double dDexS0 = (x-x0-0.5)/s0/s0/sqrt2pi*FastMath.exp(-xm*xm) - (x-x0+0.5)/s0/s0/sqrt2pi*FastMath.exp(-xp*xp);
					double dMuX0 = - in0*dey[y-ymin]*(e_xp_sq[x-xmin]-e_xm_sq[x-xmin]);
					double dMuY0 = - in0*dex[x-xmin]*(e_yp_sq[y-ymin]-e_ym_sq[y-ymin]);
					double dDeyS0 = ((double)y-y0-0.5)/s0*e_ym_sq[y-ymin] - ((double)y-y0+0.5)/s0*e_yp_sq[y-ymin];
					double dDexS0 = ((double)x-x0-0.5)/s0*e_xm_sq[x-xmin] - ((double)x-x0+0.5)/s0*e_xp_sq[x-xmin];
					g[4] += dFMu;
					g[3] += dex[x-xmin] * dey[y-ymin] * dFMu;
					g[0] += dFMu * dMuX0;
					g[1] += dFMu * dMuY0;
					g[2] += dFMu *in0 * (dex[x-xmin] * dDeyS0 + dey[y-ymin] * dDexS0); 
				}

				return g;
			}
    	});
    }

    @Override
    public MultivariateVectorFunction getCrbFunction() {
        return new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] point) throws IllegalArgumentException {
                double x0 = point[0];
                double y0 = point[1];
                double s0 = point[2];
                double in0 = point[3];
                double bg0 = point[4];
    
                double [] g = new double[2];
    
                for (int k = 0; k < data.getLength(); k++) {
                    double x = (double) data.getXCordinate(k);
                    double y = (double) data.getYCordinate(k);
                    double xp = (x - x0 + 0.5) / sqrt2 / s0;
                    double xm = (x - x0 - 0.5) / sqrt2 / s0;
                    double yp = (y - y0 + 0.5) / sqrt2 / s0;
                    double ym = (y - y0 - 0.5) / sqrt2 / s0;
                    double dex = 0.5 * erf(xm, xp);
                    double dey = 0.5 * erf(ym, yp);
                    double mu = in0 * dex * dey + bg0;
                    double dMuX0 = - in0*dey/s0/sqrt2pi*(FastMath.exp(-xp*xp)-FastMath.exp(-xm*xm));
                    double dMuY0 = - in0*dex/s0/sqrt2pi*(FastMath.exp(-yp*yp)-FastMath.exp(-ym*ym));
                    g[0] += dMuX0 * dMuX0 / mu;
                    g[1] += dMuY0 * dMuY0 / mu;
                }
                g[0] = FastMath.sqrt(1.0 / g[0]);
                g[1] = FastMath.sqrt(1.0 / g[1]);
                return g;                
            }
        };
	}

    private double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * FastMath.exp(-z * z - 1.26551223
                + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807
                        + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
        if (z >= 0) {
            return ans;
        } else {
            return -ans;
        }
    }
    
    private double erf(double z1, double z2) {
    	return erf(z2) - erf(z1);
    }

	@Override
	public double[] guessInit() {
		if (data == null) {
			return null;
		}
		double [] guess = new double[5];
		int idxCenter = data.getLength() / 2;
		guess[0] = data.getXCordinate(idxCenter);
		guess[1] = data.getYCordinate(idxCenter);
		guess[2] = 2.0;
		guess[4] = data.getValue(0);
		guess[3] = (data.getValue(idxCenter) - guess[4]) * 10;
		return guess;
	}
}
