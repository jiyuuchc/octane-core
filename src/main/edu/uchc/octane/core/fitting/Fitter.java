package edu.uchc.octane.core.fitting;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public interface Fitter {

	public double [] fit(PixelImageBase data, double [] start);
	public double [] getResult();

}
