package edu.uchc.octanecore.fitting;

import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octanecore.datasource.ImageData;

public class LeastSquareGaussianFitting {

    public final static int MAX_ITERATIONS = 1000;
    public final static double CONVERGENCE_DELTA = 1e-6;

    public ImageData data;
    public LeastSquaresOptimizer.Optimum optimum;
    public boolean useWeighting;

    protected int maxIter;    // after `maxIter` iterations the algorithm converges

    public double[] fittedParameters;
    public boolean fixSigma, fixOffset;

    public static class Params {
    	public static final int X = 0;
    	public static final int Y = 1;
    	public static final int INTENSITY = 2;
    	public static final int SIGMA = 3;
    	public static final int OFFSET = 4;
    	public static final int PARAMS_LENGTH = 5;
    }

    public LeastSquareGaussianFitting() {
        this(false, false, false, MAX_ITERATIONS);
    }

    public LeastSquareGaussianFitting(boolean useWeighting, boolean fixSigma, boolean fixOffset, int maxIter) {

        this.maxIter = maxIter;
        this.useWeighting = useWeighting;
        this.fixSigma = fixSigma;
        this.fixOffset = fixOffset;

    }

    public double [] fit(ImageData data, double [] start) {

    	this.data = data;

    	LeastSquaresProblem lsp = LeastSquaresFactory.create(
    			LeastSquaresFactory.model(getValueFunction(), getJacobian()),
    			new ArrayRealVector(data.getValueVector()),
    			new ArrayRealVector(parametersToPoint(start)),
    			LeastSquaresFactory.evaluationChecker(new SimpleVectorValueChecker(CONVERGENCE_DELTA, CONVERGENCE_DELTA)),
    			maxIter,
    			maxIter);

    	if ( useWeighting ) {
    		lsp = LeastSquaresFactory.weightDiagonal(lsp, new ArrayRealVector(calcWeights()));
    	}

    	LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

    	optimum = optimizer.optimize(lsp);
    	fittedParameters = pointToParameters(optimum.getPoint().toArray());

//    	//Deflation: change orignal data to remove fitted value
//    	double [] v = data.getValueVector();
//    	for (int i = 0; i < data.getLength(); i++) {
//    		v[i] = optimum.getResiduals().getEntry(i) + fittedParameters[Params.OFFSET];
//    	}

    	return fittedParameters;
    }

    protected double[] calcWeights() {

    	double[] weights = new double[data.getLength()];

    	if(!useWeighting){

    		Arrays.fill(weights, 1);

    	} else {

    		double minWeight = 1e6;

            for (int i = 0; i < data.getLength(); i++) {

            	weights[i] = 1 / data.getValue(i);

            	if (weights[i] < minWeight) {
            		minWeight = weights[i];
            	}

            }

            for (int i = 0; i < data.getLength(); i++) {

            	if (Double.isInfinite(weights[i]) || Double.isNaN(weights[i]) || weights[i] > 1000 * minWeight) {

            		weights[i] = 1000 * minWeight;

            	}

            }

    	}

    	return weights;
    }

    protected MultivariateVectorFunction getValueFunction() {

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

    protected MultivariateMatrixFunction getJacobian() {

    	return new MultivariateMatrixFunction() {

        	@Override
            //Copied from thunderstorm code
            //d(b^2 + ((J*J)/2/PI/(s*s)/(s*s)) * e^( -( ((x0-x)^2)/(2*s*s*s*s) + (((y0-y)^2)/(2*s*s*s*s)))))/dx
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

    protected double[] pointToParameters(double[] point) {

    	double [] transformed = point.clone();

    	transformed[Params.INTENSITY] = point[Params.INTENSITY] * point[Params.INTENSITY];
        transformed[Params.SIGMA] = point[Params.SIGMA] * point[Params.SIGMA];
        transformed[Params.OFFSET] = point[Params.OFFSET] * point[Params.OFFSET];

        return transformed;
    }

    protected double[] parametersToPoint(double[] parameters) {

    	double [] transformed = parameters.clone();

    	transformed[Params.INTENSITY] = FastMath.sqrt(parameters[Params.INTENSITY]);
        transformed[Params.SIGMA] = FastMath.sqrt(parameters[Params.SIGMA]);
        transformed[Params.OFFSET] = FastMath.sqrt(parameters[Params.OFFSET]);

        return transformed;
    }

}
