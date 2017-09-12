package edu.uchc.octanecore.frameanalysis;

import edu.uchc.octanecore.datasource.RectangularImage;

/* 
 * Simple deflation based analysis:
 * 1. Find maximum intensity in the image. 
 * 2. Returns a ROI to the callback function for centroid fitting.
 * 3. Deflate the image based on fitted the result
 * 4. Go back to step 1 
 */

public class SimpleDeflation {
	
	public int ROISize;
	public double threshold;
	
	public interface CallBackFunctions {
	
		// provide an ROI for centroid fitting. Should return fitted value (without offset) for image deflation.
		public boolean fitAndDeflate(RectangularImage ROI, int x, int y); 
	
	}
	
	public SimpleDeflation (double threshold) {
		this(threshold, 11);
	}

	public SimpleDeflation(double threshold, int size) {
		this.ROISize = size;
		this.threshold = threshold;
	}
	
	public void processFrames(RectangularImage data, SimpleDeflation.CallBackFunctions callback) {
		
		while (true) {
			double max = Double.MIN_VALUE;
			int ii = 0;

			for (int i = 0; i < data.getLength(); i++) {
				if ( max < data.getValue(i) ) {
					max = data.getValue(i);
					ii = i;
				}
			}

			if (max < threshold) {
				break;
			}

			int x = data.getXCordinate(ii);
			int y = data.getYCordinate(ii);

			RectangularImage subImage = new RectangularImage(data, x - ROISize / 2, y - ROISize / 2, ROISize, ROISize, true);
			if (callback.fitAndDeflate(subImage, x, y) == false) {
				break;
			}

			data.copyFrom(subImage); // copy deflated subimage back to orig image.
		}
	}
}
