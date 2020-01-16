package edu.uchc.octane.core.fitting.maximumlikelihood;

import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public interface LikelihoodModel {

	public ObjectiveFunction getObjectiveFunction();
	public ObjectiveFunctionGradient getObjectiveFunctionGradient();
	public void setData(PixelImageBase data);
}
