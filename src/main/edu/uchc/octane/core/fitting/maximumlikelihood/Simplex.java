package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class Simplex implements Fitter {

	final Logger logger = LoggerFactory.getLogger(Simplex.class);

	SimplexOptimizer optimizer; 
	
	LikelihoodModel func;
	PointValuePair result;

	public Simplex(LikelihoodModel func) {
		this.func = func;
		optimizer = new SimplexOptimizer (1e-8, 1e-8);		
	}

	@Override
	public double[] fit(PixelImageBase data, double[] start) {
		double [] guess = func.setData(data);
		if (start == null) {
			start = guess;
		}
		try {
			result = optimizer.optimize(
					func.getObjectiveFunction(),
					new NelderMeadSimplex(guess.length),
					GoalType.MAXIMIZE,
					new MaxIter(500),
					MaxEval.unlimited(),
					new InitialGuess(start) ); 
		} catch(TooManyIterationsException e) {
    	    logger.error("Iterations exceded limit.");
    		logger.error(e.getMessage());
    		result = null;
    		return null;
		}

		return getResult();	}

	double[] getResult() {
		if (result != null) {
			return result.getPoint();
		} else {
			return null;
		}
	}

	@Override
	public String[] getHeaders() {
		return func.getHeaders();
	}
}
