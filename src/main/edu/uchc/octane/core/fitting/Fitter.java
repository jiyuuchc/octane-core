package edu.uchc.octane.core.fitting;

import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

public interface Fitter {

	public double [] fit(AbstractDoubleImage data, double [] start);
	public double [] getResult();

}
