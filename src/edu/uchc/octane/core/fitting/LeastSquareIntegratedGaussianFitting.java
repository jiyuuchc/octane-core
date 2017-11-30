package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.ImageData;

public class LeastSquareIntegratedGaussianFitting {

    public final static int MAX_ITERATIONS = 1000;
    public final static double CONVERGENCE_DELTA = 1e-6;

    public ImageData data;
    public LeastSquaresOptimizer.Optimum optimum;

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

    public LeastSquareIntegratedGaussianFitting() {
        this(false, false, MAX_ITERATIONS);
    }

    public LeastSquareIntegratedGaussianFitting(boolean fixSigma, boolean fixOffset, int maxIter) {

        this.maxIter = maxIter;
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

    protected MultivariateVectorFunction getValueFunction() {

        return new MultivariateVectorFunction() {

        	@Override
            public double[] value(double[] point) throws IllegalArgumentException {

                double[] params = pointToParameters(point);
                double sigma = params[Params.SIGMA];
                double sqrt2s = FastMath.sqrt(2.0) * sigma;

                double[] retVal = new double[data.getLength()];

                //FIXME cache values
                for (int i = 0; i < data.getLength(); i++) {

                	double dx = data.getXCordinate(i) - params[Params.X];
                	double dy = data.getYCordinate(i) - params[Params.Y];
                	double errX = erf((dx + 0.5) / sqrt2s) - erf((dx - 0.5) / sqrt2s);
                	double errY = erf((dy + 0.5) / sqrt2s) - erf((dy - 0.5) / sqrt2s);
                	retVal[i] = params[Params.OFFSET] + 0.25 * params[Params.INTENSITY] * errX * errY;

                }

                return retVal;
        	}
        };

    }

    protected MultivariateMatrixFunction getJacobian() {

    	return new MultivariateMatrixFunction() {

        	@Override
            public double[][] value(double[] point) throws IllegalArgumentException {

                double[] params = pointToParameters(point);
                double sigma = params[Params.SIGMA];
                double sigmaSquared = sigma * sigma;
                double sqrt2s = FastMath.sqrt(2) * sigma;

                double[][] retVal = new double[data.getLength()][params.length];

                //FIXME cache values
                for (int i = 0; i < data.getLength(); i++) {

                	double dy = data.getYCordinate(i) - params[Params.Y];
                	double dx = data.getXCordinate(i) - params[Params.X];
                	double errY = erf((dy + 0.5) / sqrt2s) - erf((dy - 0.5) / sqrt2s);
                	double errX = erf((dx + 0.5) / sqrt2s) - erf((dx - 0.5) / sqrt2s);

                	double yp = FastMath.sqrt(2 / FastMath.PI) * FastMath.exp( -(dy + .5) * (dy + .5) / ( 2 * sigmaSquared) );
                	double ym = FastMath.sqrt(2 / FastMath.PI) * FastMath.exp( -(dy - .5) * (dy - .5) / ( 2 * sigmaSquared) );
                	double expDeltaY = (ym - yp) / sigma;

                	double xp = FastMath.sqrt(2 / FastMath.PI) * FastMath.exp( -(dx + .5) * (dx + .5) / ( 2 * sigmaSquared) );
                	double xm = FastMath.sqrt(2 / FastMath.PI) * FastMath.exp( -(dx - .5) * (dx - .5)/ ( 2 * sigmaSquared) );
                	double expDeltaX = (xm - xp) / sigma;

                	retVal[i][Params.INTENSITY] = 0.5 * point[Params.INTENSITY] * errX * errY;

                    if (fixSigma) {
                    	retVal[i][Params.SIGMA] = 0;
                    } else {
                    	double expDeltaSY =(ym * (dy - 0.5) - yp * (dy + 0.5)) / sigmaSquared;
                    	double expDeltaSX =(xm * (dx - 0.5) - xp * (dx + 0.5)) / sigmaSquared;
                    	retVal[i][Params.SIGMA] = 0.25 * params[Params.INTENSITY] * (errX * expDeltaSY + errY * expDeltaSX);
                    }

                    retVal[i][Params.X] = 0.25 * params[Params.INTENSITY] * errY * expDeltaX;
                    retVal[i][Params.Y] = 0.25 * params[Params.INTENSITY] * errX * expDeltaY;

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
        transformed[Params.OFFSET] = point[Params.OFFSET] * point[Params.OFFSET];

        return transformed;
    }

    protected double[] parametersToPoint(double[] parameters) {

    	double [] transformed = parameters.clone();

    	transformed[Params.INTENSITY] = FastMath.sqrt(parameters[Params.INTENSITY]);
        transformed[Params.OFFSET] = FastMath.sqrt(parameters[Params.OFFSET]);

        return transformed;
    }

    private double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * FastMath.exp(-z * z - 1.26551223
                + t * (1.00002368
                + t * (0.37409196
                + t * (0.09678418
                + t * (-0.18628806
                + t * (0.27886807
                + t * (-1.13520398
                + t * (1.48851587
                + t * (-0.82215223
                + t * (0.17087277))))))))));
        if(z >= 0) {
            return ans;
        } else {
            return -ans;
        }
    }
}
