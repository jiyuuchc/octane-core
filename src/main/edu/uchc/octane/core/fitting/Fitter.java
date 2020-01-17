package edu.uchc.octane.core.fitting;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

/**
 * Interface for single particle fitting classes.
 */
public interface Fitter extends Cloneable {

	/**
	 * Perform a local fitting 
	 * @param data A small image to be fitted
	 * @param start Initial guess of the parameters. can be null.
	 * @return Fitted parameters for one particle.
	 */
	public double [] fit(PixelImageBase data, double [] start);

	// public double [] getResult();
	
	/**
	 * @return Names of the parameters.
	 */
	public String [] getHeaders();
}
