package edu.uchc.octane.core.fitting;

import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class NotFitter implements Fitter {

	static final String [] headers = {"x", "y", "sigma", "intensity"};

	@Override
	public double[] fit(PixelImageBase data, double[] start) {
		if (data == null) {
			return null;
		}
		double x0 = 0;
		double y0 = 0;
		double m = 1e8;
		double ss = 0;
		for (int k = 0; k < data.getLength(); k++) {
			x0 += data.getXCordinate(k) * data.getValue(k);
			y0 += data.getYCordinate(k) * data.getValue(k);
			if (m > data.getValue(k)) {
				m = data.getValue(k);
			}
			ss += data.getValue(k);
		}
		double [] guess = new double[4];
		guess[0] = x0 / ss;
		guess[1] = y0 / ss;
		guess[2] = 1.65;
		guess[3] = ss - m * data.getLength();
		return guess;		
	}

	@Override
	public String[] getHeaders() {
		return headers;
	}

}
