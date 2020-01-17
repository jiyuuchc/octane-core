package edu.uchc.octane.core.fitting.leastsquare;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

/**
 * PSFs for LeastSquare fitter all have this interface  
 * @author Ji Yu
 */
public interface PSFFittingFunction extends Cloneable {

	MultivariateVectorFunction getValueFunction();
	MultivariateMatrixFunction getJacobian();

	/**
	 * @param internal representation of the fitting Parameters
	 * @return external representation
	 */
	double[] convertParametersInternalToExternal(double[] internalParameters);

	/**
	 * @param external representation of the fitting parameters
	 * @return internal representation
	 */
	double[] convertParametersExternalToInternal(double[] externalParameters);
	
	/**
	 * @param data image to be fitted
	 * @return initial guess of the parameters (external representation)
	 */
	double [] setFittingData(PixelImageBase data);

	/**
	 * @return degree of freedom of the PSF
	 */
	int getDoF();
	
	/**
	 * @return names of the parameters fitted
	 */
	String [] getHeaders();
}
