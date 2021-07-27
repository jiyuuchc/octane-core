package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class SymmetricGaussian implements LikelihoodModel {
	final static double sqrt2pi = FastMath.sqrt(2 * FastMath.PI);

	PixelImageBase data;
	final String [] headers = {"x","y","sigma","intensity","bg"};	

	@Override
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
					double dex = FastMath.exp(-(x-x0)*(x-x0)/2/s0/s0)/sqrt2pi/s0;
					double dey = FastMath.exp(-(y-y0)*(y-y0)/2/s0/s0)/sqrt2pi/s0;
					double mu = in0 * dex * dey + bg0;
					f += data.getValue(k) * FastMath.log(mu) - mu;
				} 
				return f;
			}

		});
	}

	@Override
	public ObjectiveFunctionGradient getObjectiveFunctionGradient() {
		return new ObjectiveFunctionGradient(new MultivariateVectorFunction() {

			@Override
			public double[] value(double[] point) throws IllegalArgumentException {
				double x0 = point[0];
				double y0 = point[1];
				double s0 = point[2];
				double in0 = point[3]; 
				double bg0 = point[4];
				
				double [] g = new double[5];
				for (int k = 0; k < data.getLength(); k ++) {
					double x = (double) data.getXCordinate(k);
					double y = (double) data.getYCordinate(k);
					double dex = FastMath.exp(-(x-x0)*(x-x0)/2/s0/s0)/sqrt2pi/s0;
					double dey = FastMath.exp(-(y-y0)*(y-y0)/2/s0/s0)/sqrt2pi/s0;
					double lambda = in0 * dex * dey;
					double kappa = data.getValue(k) / (lambda + bg0) - 1.0;
					g[4] += kappa;
					g[3] += kappa * dex * dey;
					g[0] += kappa * lambda * (x-x0)/s0/s0/sqrt2pi/s0;
					g[1] += kappa * lambda * (y-x0)/s0/s0/sqrt2pi/s0;
					g[2] += kappa * lambda/FastMath.PI/s0/s0/s0*(((x-x0)*(x-x0)+(y-y0)*(y-y0))/s0/s0-1.0);
				}
				return g;
			}
			
		});
	}

	@Override
	public void setData(PixelImageBase data) {
		this.data = data;
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

	@Override
	public String[] getHeaders() {
		return headers;
	}
}
