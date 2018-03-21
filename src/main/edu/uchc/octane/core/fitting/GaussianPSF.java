package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

public class GaussianPSF implements PSFFittingFunction {

	AbstractDoubleImage data;
	boolean fixSigma, fixOffset;

	public static class Params {
    	public static final int X = 0;
    	public static final int Y = 1;
    	public static final int INTENSITY = 2;
    	public static final int SIGMA = 3;
    	public static final int OFFSET = 4;
    	public static final int PARAMS_LENGTH = 5;
    }

	public GaussianPSF() {
		this(false, false);
	}

	public GaussianPSF(boolean fixSigma, boolean fixOffset) {
		data = null;
		this.fixOffset = fixOffset;
		this.fixSigma = fixSigma;
	}

	double sq(double d) {return d*d;}

	//internerally p(4)^2 + p(2)^2 * exp(-(dx/p(3))^2-(dy/p(3))^2)
	@Override
	public MultivariateVectorFunction getValueFunction() {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] point) throws IllegalArgumentException {
				double[] retVal = new double[data.getLength()];
				for(int i = 0; i < data.getLength(); i++) {
					double dx = data.getXCordinate(i) - point[Params.X];
					double dy = data.getYCordinate(i) - point[Params.Y];
					retVal[i] = sq(point[Params.OFFSET]) + sq(point[Params.INTENSITY])* FastMath.exp(-(sq(dx)+sq(dy))/sq(point[Params.SIGMA]));
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
                double sigma3 = point[Params.SIGMA] * point[Params.SIGMA] * point[Params.SIGMA];
                double[][] retVal = new double[data.getLength()][Params.PARAMS_LENGTH];
                for (int i = 0; i < data.getLength(); i++) {
					double dx = data.getXCordinate(i) - point[Params.X];
					double dy = data.getYCordinate(i) - point[Params.Y];
					double expterm = FastMath.exp(-(sq(dx) + sq(dy))/sq(point[Params.SIGMA]));
					retVal[i][Params.X] = 2 * sq(point[Params.INTENSITY]) * expterm * dx / sq(point[Params.SIGMA]);
					retVal[i][Params.Y] = 2 * sq(point[Params.INTENSITY]) * expterm * dy / sq(point[Params.SIGMA]);
					retVal[i][Params.INTENSITY] = 2 * point[Params.INTENSITY] * expterm;
					if (fixSigma) {
						retVal[i][Params.SIGMA] = 0;
					} else {
						retVal[i][Params.SIGMA] = 2 * sq(point[Params.INTENSITY]) * expterm * (sq(dx) + sq(dy)) / sigma3;
					}
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
	public double[] pointToParameters(double[] point) {
    	double [] params = point.clone();
        params[Params.SIGMA] = FastMath.abs(point[Params.SIGMA] / FastMath.sqrt(2));
        params[Params.OFFSET] = point[Params.OFFSET] * point[Params.OFFSET];
    	params[Params.INTENSITY] = sq(point[Params.INTENSITY]) * FastMath.PI * sq(point[Params.SIGMA]);
        return params;
    }


    @Override
	public double[] parametersToPoint(double[] parameters) {

    	double [] point = parameters.clone();

        point[Params.SIGMA] = parameters[Params.SIGMA] * FastMath.sqrt(2);
        point[Params.OFFSET] = FastMath.sqrt(parameters[Params.OFFSET]);
    	point[Params.INTENSITY] =
    			FastMath.sqrt(parameters[Params.INTENSITY] / FastMath.PI / sq(point[Params.SIGMA]));

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
		if (fixSigma) {dof --;}
		return dof;
	}
}
