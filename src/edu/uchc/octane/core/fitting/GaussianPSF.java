package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.ImageData;

public class GaussianPSF implements PSFFittingFunction {

	ImageData data;
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

	@Override
	public MultivariateVectorFunction getValueFunction() {

		return new MultivariateVectorFunction() {

			@Override
			public double[] value(double[] point) throws IllegalArgumentException {

				double[] retVal = new double[data.getLength()];

				double[] params = pointToParameters(point);
				double twoSigmaSquared = params[Params.SIGMA] * params[Params.SIGMA] * 2;

				for(int i = 0; i < data.getLength(); i++) {

					retVal[i] = params[Params.OFFSET] + params[Params.INTENSITY] / (twoSigmaSquared * FastMath.PI)
							* FastMath.exp(-((data.getXCordinate(i) - params[Params.X]) * (data.getXCordinate(i) - params[Params.X]) + (data.getYCordinate(i) - params[Params.Y]) * (data.getYCordinate(i) - params[Params.Y])) / twoSigmaSquared);

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

        		double[] params = pointToParameters(point);
                double sigma = params[Params.SIGMA];
                double sigmaSquared = sigma * sigma;
                double[][] retVal = new double[data.getLength()][Params.PARAMS_LENGTH];

                for (int i = 0; i < data.getLength(); i++) {
                    //d()/dIntensity
                    double xd = (data.getXCordinate(i) - params[Params.X]);
                    double yd = (data.getYCordinate(i) - params[Params.Y]);
                    double upper = -(xd * xd + yd * yd) / (2 * sigmaSquared);
                    double expVal = FastMath.exp(upper);
                    double expValDivPISigmaSquared = expVal / (sigmaSquared * FastMath.PI);
                    double expValDivPISigmaPowEight = expValDivPISigmaSquared / sigmaSquared;
                    retVal[i][Params.INTENSITY] = point[Params.INTENSITY] * expValDivPISigmaSquared;
                    //d()/dx
                    retVal[i][Params.X] = params[Params.INTENSITY] * xd * expValDivPISigmaPowEight * 0.5;
                    //d()/dy
                    retVal[i][Params.Y] = params[Params.INTENSITY] * yd * expValDivPISigmaPowEight * 0.5;
                    //d()/dsigma
                    if (fixSigma) {
                    	retVal[i][Params.SIGMA] = 0;
                    } else {
                    	retVal[i][Params.SIGMA] = params[Params.INTENSITY] * expValDivPISigmaPowEight / point[Params.SIGMA] * (xd * xd + yd * yd - 2 * sigmaSquared);
                    }
                    //d()/dbkg
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

    	double [] transformed = point.clone();

    	transformed[Params.INTENSITY] = point[Params.INTENSITY] * point[Params.INTENSITY];
        transformed[Params.SIGMA] = point[Params.SIGMA] * point[Params.SIGMA];
        transformed[Params.OFFSET] = point[Params.OFFSET] * point[Params.OFFSET];

        return transformed;
    }


    @Override
	public double[] parametersToPoint(double[] parameters) {

    	double [] transformed = parameters.clone();

    	transformed[Params.INTENSITY] = FastMath.sqrt(parameters[Params.INTENSITY]);
        transformed[Params.SIGMA] = FastMath.sqrt(parameters[Params.SIGMA]);
        transformed[Params.OFFSET] = FastMath.sqrt(parameters[Params.OFFSET]);

        return transformed;
    }

	@Override
	public void setFittingData(ImageData data) {
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
