package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import edu.uchc.octane.core.datasource.ImageData;

public interface PSFFittingFunction {

	MultivariateVectorFunction getValueFunction();
	MultivariateMatrixFunction getJacobian();
	double[] pointToParameters(double[] point);
	double[] parametersToPoint(double[] parameters);
	void setFittingData(ImageData data);
	int getDoF();
}
