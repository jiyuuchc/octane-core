package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

public class AsymmetricGaussianPSF implements PSFFittingFunction {

	AbstractDoubleImage data;
	boolean fixOffset;

	public static class Params {
    	public static final int X = 0;
    	public static final int Y = 1;
    	public static final int INTENSITY = 2;
    	public static final int SIGMAX = 3;
    	public static final int SIGMAY = 4;
    	public static final int OFFSET = 5;
    	public static final int PARAMS_LENGTH = 6;
    }

	public AsymmetricGaussianPSF() {
		this(false);
	}

	public AsymmetricGaussianPSF(boolean fixOffset) {
		data = null;
		this.fixOffset = fixOffset;
	}

	double sq(double d) { return d*d;}

	// Internal representation is p(5)^2 + p(2)^2 * exp(-[(p(0)-x)/p(3)]^2 - [(p(1)-y)/p(4)]^2)
	@Override
	public MultivariateVectorFunction getValueFunction() {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] point) throws IllegalArgumentException {

				double[] retVal = new double[data.getLength()];
				for(int i = 0; i < data.getLength(); i++) {
					double dx = data.getXCordinate(i) - point[Params.X];
					double dy = data.getYCordinate(i) - point[Params.Y];
					retVal[i] = sq(point[Params.OFFSET]) + sq(point[Params.INTENSITY]) *
							FastMath.exp(-sq(dx/point[Params.SIGMAX]) - sq(dy/point[Params.SIGMAY]));
				}
				return retVal;
			}
		};
	}

	@Override
	public MultivariateMatrixFunction getJacobian() {
    	return new MultivariateMatrixFunction() {
        	@Override
            public double[][] value(double[] point) throws IllegalArgumentException {

                double[][] retVal = new double[data.getLength()][Params.PARAMS_LENGTH];
                double sigmaX3 = point[Params.SIGMAX] * point[Params.SIGMAX] * point[Params.SIGMAX];
                double sigmaY3 = point[Params.SIGMAY] * point[Params.SIGMAY] * point[Params.SIGMAY];
                for (int i = 0; i < data.getLength(); i++) {
					double dx = data.getXCordinate(i) - point[Params.X];
					double dy = data.getYCordinate(i) - point[Params.Y];
					double expterm = FastMath.exp(-sq(dx/point[Params.SIGMAX]) - sq(dy/point[Params.SIGMAY]));

					retVal[i][Params.X] = 2 * sq(point[Params.INTENSITY]) * expterm * dx / sq(point[Params.SIGMAX]);
					retVal[i][Params.Y] = 2 * sq(point[Params.INTENSITY]) * expterm * dy / sq(point[Params.SIGMAY]);
					retVal[i][Params.INTENSITY] = 2 * point[Params.INTENSITY] * expterm;

					retVal[i][Params.SIGMAX] = 2 * sq(point[Params.INTENSITY]) * expterm * sq(dx) / sigmaX3;
					retVal[i][Params.SIGMAY] = 2 * sq(point[Params.INTENSITY]) * expterm * sq(dy) / sigmaY3;

                    if (fixOffset) {
                    	retVal[i][Params.OFFSET] = 0;
                    } else {
                    	retVal[i][Params.OFFSET] = 2 * point[Params.OFFSET];
                    }
                }
                return retVal;
        	}
        };
	}

	@Override
	public double[] convertParametersInternalToExternal(double[] internalParameters) {

	    if (!data.isCoordinateValid(
                (int) FastMath.floor(internalParameters[Params.X]), 
                (int) FastMath.floor(internalParameters[Params.Y]))) {
            return null;
        }

	    if (internalParameters[Params.SIGMAX] <=0 || internalParameters[Params.SIGMAY] <= 0) {
	        return null;
	    }
	    
    	double [] params = internalParameters.clone();

    	params[Params.SIGMAX] = internalParameters[Params.SIGMAX] / FastMath.sqrt(2);
        params[Params.SIGMAY] = internalParameters[Params.SIGMAY] / FastMath.sqrt(2);
        params[Params.OFFSET] = sq(internalParameters[Params.OFFSET]);
    	params[Params.INTENSITY] = sq(internalParameters[Params.INTENSITY]) * FastMath.PI * internalParameters[Params.SIGMAX] * internalParameters[Params.SIGMAY];
        return params;
    }

    @Override
	public double[] convertParametersExternalToInternal(double[] externalParameters) {

    	double [] point = externalParameters.clone();

        point[Params.SIGMAX] = externalParameters[Params.SIGMAX] * FastMath.sqrt(2);
        point[Params.SIGMAY] = externalParameters[Params.SIGMAY] * FastMath.sqrt(2);
        point[Params.OFFSET] = FastMath.sqrt(externalParameters[Params.OFFSET]);
    	point[Params.INTENSITY] =
    			FastMath.sqrt(externalParameters[Params.INTENSITY] / FastMath.PI / point[Params.SIGMAX] / point[Params.SIGMAY]);

        return point;
    }

	@Override
	public void setFittingData(AbstractDoubleImage data) {
		this.data = data;
	}

	@Override
	public int getDoF() {
		int dof = Params.PARAMS_LENGTH;
		if (fixOffset) {dof --;}
		return dof;
	}
}
