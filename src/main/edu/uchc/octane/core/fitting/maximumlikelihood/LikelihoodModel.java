package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public interface LikelihoodModel {

	public ObjectiveFunction getObjectiveFunction();
	public ObjectiveFunctionGradient getObjectiveFunctionGradient();

	//returns a guess for the fitting parameters. useful initial values for fitting  
	public void setData(PixelImageBase data);
	public double[] guessInit();
	// returns descriptions of parameters
	public String [] getHeaders();
    MultivariateVectorFunction getCrbFunction();
}
