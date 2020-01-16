package edu.uchc.octane.core.fitting;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

/**
 * Interface for single particle fitting classes.
 */
public interface Fitter {

	/**
	 * Perform a local fitting 
	 * @param data A small image to be fitted
	 * @param start Initial guess of the parameters. can be null.
	 * @return Fitted parameters.
	 */
	public double [] fit(PixelImageBase data, double [] start);

	/**
	 * @return The fitting result, assuming function 'fit' was called previously.
	 */
	public double [] getResult();
	
	/**
	 * @return Names of the parameters.
	 */
	public String [] getHeaders();
}
