package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class PoissonLogLikelihoodSymmetric implements LikelihoodModel {

	final static double sqrt2pi = FastMath.sqrt(2 * FastMath.PI);
	final static double sqrt2 = FastMath.sqrt(2);

	// double x0, y0, z0, bg0, in0;
	PixelImageBase data;
	final String [] headers = {"x","y","sigma","intensity","offset"};

	public PoissonLogLikelihoodSymmetric() {
	}
	
	public void setData(PixelImageBase data) {
		this.data = data;
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

				double f = 0;
				for (int k = 0; k < data.getLength(); k ++) {
					double x = (double) data.getXCordinate(k);
					double y = (double) data.getYCordinate(k);				
					double xp = (x - x0 + 0.5) / sqrt2 / s0;
					double xm = (x - x0 - 0.5) / sqrt2 / s0;
					double yp = (y - y0 + 0.5) / sqrt2 / s0;
					double ym = (y - y0 - 0.5) / sqrt2 / s0;
					double dex = 0.5 * erf(xm, xp);
					double dey = 0.5 * erf(ym, yp);
					double mu = in0 * dex * dey + bg0;
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
				
				double [] g = new double[5];

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
					double dFMu = data.getValue(k) / mu - 1;
					double dMuX0 = - in0*dey/s0/sqrt2pi*(FastMath.exp(-xp*xp)-FastMath.exp(-xm*xm));
					double dMuY0 = - in0*dex/s0/sqrt2pi*(FastMath.exp(-yp*yp)-FastMath.exp(-ym*ym));
					double dDeyS0 = (y-y0-0.5)/s0/s0/sqrt2pi*FastMath.exp(-ym*ym) - (y-y0+0.5)/s0/s0/sqrt2pi*FastMath.exp(-yp*yp);
					double dDexS0 = (x-x0-0.5)/s0/s0/sqrt2pi*FastMath.exp(-xm*xm) - (x-x0+0.5)/s0/s0/sqrt2pi*FastMath.exp(-xp*xp);
					g[4] += dFMu;
					g[3] += dex * dey * dFMu;
					g[0] += dFMu * dMuX0;
					g[1] += dFMu * dMuY0;
					g[2] += dFMu *in0 * (dex * dDeyS0 + dey * dDexS0); 
				}

				return g;
			}
    	});
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
}
