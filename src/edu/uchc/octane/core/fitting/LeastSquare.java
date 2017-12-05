package edu.uchc.octane.core.fitting;

import java.util.Arrays;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;

import edu.uchc.octane.core.datasource.ImageData;

public class LeastSquare{

    public final static int MAX_ITERATIONS = 1000;
    public final static double CONVERGENCE_DELTA = 1e-6;

    public boolean useWeighting;
    public PSFFittingFunction psf;
    private int maxIter;    // after `maxIter` iterations the algorithm converges
    public LeastSquaresOptimizer.Optimum optimum;

    public LeastSquare(PSFFittingFunction psf) {
    	this(psf,false);
    }

    public LeastSquare(PSFFittingFunction psf, boolean useWeighting) {
        this(psf, useWeighting, MAX_ITERATIONS);
    }

    public LeastSquare(PSFFittingFunction psf, boolean useWeighting, int maxIter) {
        this.psf = psf;
        this.maxIter = maxIter;
        this.useWeighting = useWeighting;
    }

    protected double[] calcWeights(ImageData data) {
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

    public double [] fit(ImageData data, double [] start) {
    	psf.setFittingData(data);
    	LeastSquaresProblem lsp = LeastSquaresFactory.create(
    			LeastSquaresFactory.model(psf.getValueFunction(), psf.getJacobian()),
    			new ArrayRealVector(data.getValueVector()),
    			new ArrayRealVector(psf.parametersToPoint(start)),
    			LeastSquaresFactory.evaluationChecker(new SimpleVectorValueChecker(CONVERGENCE_DELTA, CONVERGENCE_DELTA)),
    			maxIter,
    			maxIter);

    	if ( useWeighting ) {
    		lsp = LeastSquaresFactory.weightDiagonal(lsp, new ArrayRealVector(calcWeights(data)));
    	}

    	LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

    	optimum = optimizer.optimize(lsp);

    	return getResult();
    }

    public double [] getResult() {
    	return psf.pointToParameters(optimum.getPoint().toArray());
    }
}
