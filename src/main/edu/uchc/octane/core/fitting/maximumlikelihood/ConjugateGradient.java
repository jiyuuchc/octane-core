package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

public class ConjugateGradient implements Fitter {

	final Logger logger = LoggerFactory.getLogger(ConjugateGradient.class);

	// public final static int MAX_ITERATIONS = 100;
	// public final static double CONVERGENCE_DELTA = 1e-4;

	// double x0, y0, z0, in0, bg0;
	NonLinearConjugateGradientOptimizer optimizer; 
	
	LikelihoodModel func;
	PointValuePair result;

	public ConjugateGradient(LikelihoodModel func) {
		this.func = func;
		optimizer = new NonLinearConjugateGradientOptimizer (
				NonLinearConjugateGradientOptimizer.Formula.FLETCHER_REEVES,
				new SimpleValueChecker(1e-8, 1e-8) );		
	}

	@Override
	public double[] fit(AbstractDoubleImage data, double[] start) {
		
		func.setData(data);
		
		try {
			result = optimizer.optimize(
					func.getObjectiveFunction(),
					func.getObjectiveFunctionGradient(),
					GoalType.MAXIMIZE,
					MaxEval.unlimited(),
					new MaxIter(1000),
					new InitialGuess(start) );
		} catch (TooManyEvaluationsException e) {
    	    logger.error("Evaluations exceded limit.");
    		logger.error(e.getMessage());
    		result = null;
		}
		return getResult();
	}

	@Override
	public double[] getResult() {
		if (result != null) {
			return result.getPoint();
		} else {
			return null;
		}
	}
}
