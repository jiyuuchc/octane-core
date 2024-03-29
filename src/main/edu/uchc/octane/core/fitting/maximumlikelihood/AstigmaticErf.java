package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class AstigmaticErf implements LikelihoodModel {

	final static double sqrt2pi = FastMath.sqrt(2 * FastMath.PI);
	final static double sqrt2 = FastMath.sqrt(2);

	final static double scale = 1.65;

	double sigma0;
	double p0;
	double p1;
	double p2;
	double gamma;
	double cntsPerPhoton;

	// double x0, y0, z0, bg0, in0;
	PixelImageBase data;
	
	public AstigmaticErf(double sigma0, double p0, double p1, double p2, double gamma) {
		this.gamma = gamma;
		this.sigma0 = sigma0;
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
	}
	
	final String [] headers = {"x","y","z","intensity","offset"};

	@Override
	public String [] getHeaders() {
		return headers;
	}

	@Override
	public void setData(PixelImageBase data) {
		this.data = data;
	}

    public ObjectiveFunction getObjectiveFunction() {
    	return new ObjectiveFunction(new MultivariateFunction() {

			@Override
			public double value(double[] point) {
				double x0 = point[0];
				double y0 = point[1];
				double z0 = point[2];
				double in0 = point[3]; 
				double bg0 = point[4];
				
				double zm = z0 - gamma;
				double zp = z0 + gamma;
				double sx = sigma0 * FastMath.sqrt(1 + p0 * zm * zm + p1 * zm * zm * zm + p2 * zm * zm * zm * zm);
				double sy = sigma0 * FastMath.sqrt(1 + p0 * zp * zp + p1 * zp * zp * zp + p2 * zp * zp * zp * zp);
				
				double f = 0;
				for (int k = 0; k < data.getLength(); k ++) {
					double x = (double) data.getXCordinate(k);
					double y = (double) data.getYCordinate(k);				
					double xp = (x - x0 + 0.5) / sqrt2 / sx;
					double xm = (x - x0 - 0.5) / sqrt2 / sx;
					double yp = (y - y0 + 0.5) / sqrt2 / sy;
					double ym = (y - y0 - 0.5) / sqrt2 / sy;
					double dex = 0.5 * erf(xm, xp);
					double dey = 0.5 * erf(ym, yp);
					double mu = in0 * dex * dey + bg0;
					f += data.getValue(k) / scale * FastMath.log(mu) - mu;
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
				double z0 = point[2];
				double in0 = point[3]; 
				double bg0 = point[4];
				
				double zm = z0 - gamma;
				double zp = z0 + gamma;
				double sx = sigma0 * FastMath.sqrt(1 + p0 * zm * zm + p1 * zm * zm * zm + p2 * zm * zm * zm * zm);
				double sy = sigma0 * FastMath.sqrt(1 + p0 * zp * zp + p1 * zp * zp * zp + p2 * zp * zp * zp * zp);
				double dSxZ0 = sigma0 * sigma0 / sx / 2 * (2 * p0 * zm + 3 * p1 * zm * zm + 4 * p2 * zm * zm * zm); 
				double dSyZ0 = sigma0 * sigma0 / sy / 2 * (2 * p0 * zp + 3 * p1 * zp * zp + 4 * p2 * zp * zp * zp);

				double [] g = new double[5];

				for (int k = 0; k < data.getLength(); k++) {
					double x = (double) data.getXCordinate(k);
					double y = (double) data.getYCordinate(k);
					double xp = (x - x0 + 0.5) / sqrt2 / sx;
					double xm = (x - x0 - 0.5) / sqrt2 / sx;
					double yp = (y - y0 + 0.5) / sqrt2 / sy;
					double ym = (y - y0 - 0.5) / sqrt2 / sy;
					double dex = 0.5 * erf(xm, xp);
					double dey = 0.5 * erf(ym, yp);
					double mu = in0 * dex * dey + bg0;
					double dFMu = data.getValue(k) / mu - 1;
					double dMuX0 = -in0*dey/sx/sqrt2pi*(FastMath.exp(-xp*xp)-FastMath.exp(-xm*xm)); 
					double dMuY0 = -in0*dex/sy/sqrt2pi*(FastMath.exp(-yp*yp)-FastMath.exp(-ym*ym));
					double dMuSx = in0*dey/sx/sx/sqrt2pi*((x-x0-0.5)*FastMath.exp(-xm*xm)-(x-x0+0.5)*FastMath.exp(-xp*xp));
					double dMuSy = in0*dex/sy/sy/sqrt2pi*((y-y0-0.5)*FastMath.exp(-ym*ym)-(y-y0+0.5)*FastMath.exp(-yp*yp));					
					g[4] += dFMu;
					g[3] += dex * dey * dFMu;
					g[0] += dFMu * dMuX0;
					g[1] += dFMu * dMuY0;
					g[2] += dFMu *(dMuSx*dSxZ0 + dMuSy*dSyZ0);
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

	@Override
	public double[] guessInit() {
		if (data == null) {
			return null;
		}
		double [] guess = new double[5];
		int idxCenter = data.getLength() / 2;
		guess[0] = data.getXCordinate(idxCenter);
		guess[1] = data.getYCordinate(idxCenter);
		guess[2] = 0;
		guess[4] = data.getValue(0);
		guess[3] = (data.getValue(idxCenter) - guess[4]) * 10;
		return guess;		
	}

    @Override
    public MultivariateVectorFunction getCrbFunction() {
        // TODO Auto-generated method stub
        return null;
    }
}
