package edu.uchc.octane.core.threshold;

public class Otsu2DThresholder {

	int bins;

	public Otsu2DThresholder() {
		this(256);
	}
	
	public Otsu2DThresholder(int bins) {
		this.bins = bins;
	}
	
	public double getThreshold(char [] image, int width) {
		
		// calculate neighbor average
		int height = image.length / width;
		int [] naImage = new int[image.length];
		
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		int naMin = Integer.MAX_VALUE, naMax = Integer.MIN_VALUE;

		int idx = width + 1;
		for (int row = 1; row < height - 1; row ++ ) {
			for (int col = 1; col < width - 1; col ++) {
				naImage[idx] = image[idx - 1] + image[idx + 1] 
						+ image[idx - width - 1] + image[idx - width] + image[idx - width + 1]
						+ image[idx + width - 1] + image[idx + width] + image[idx + width + 1];
				
				if (min > image[idx]) {
					min = image[idx];
				}
				if (max < image[idx]) {
					max = image[idx];
				}
				
				if (naMin > naImage[idx]) {
					naMin = naImage[idx];
				}
				if (naMax < naImage[idx]) {
					naMax = naImage[idx];
				}
				idx ++;
			}
			idx += 2;
		}
		
		// make histogram
		double binSpacing = (double)(max + 1 - min) / bins;
		double naBinSpacing = (double)(naMax + 1 - naMin) / bins;
		double [] hists = new double[bins * bins];

		idx = width + 1;
		for (int row = 1; row < height - 1; row ++ ) {
			for (int col = 1; col < width - 1; col ++) {

				hists[(int) (Math.floor(image[idx] / binSpacing) * bins + Math.floor(naImage[idx] / naBinSpacing)) ] += 1.0 / (width - 2) / (height - 2);
			}
		}
		
		return calculateThreshold(hists);
		
	}

	//translated from matlab implementation
	protected double calculateThreshold(double [] hists) { //row major, hists.length = bins * bins

		double maximum = 0.0;
		double threshold = 0;

		double mu_t0 = 0;
		double mu_t1 = 0;
		int idx = 0;
		for (int row = 0; row < bins; row++) {
			for (int col = 0; col < bins; col++) {
				mu_t0 += hists[idx] * row;
				mu_t1 += hists[idx] * col;
				idx ++;
			}
		}
		
		double [][] p_0 = new double[bins+1][bins+1];
		double [][] mu_i = new double[bins+1][bins+1];
		double [][] mu_j = new double[bins+1][bins+1];

		idx = 0;
		for (int row = 1; row < bins + 1 ; row ++) {
			for (int col = 1; col < bins + 1; col ++) {

				p_0[row][col] = p_0[row][col-1] + p_0[row-1][col] - p_0[row-1][col-1] + hists[idx];
				mu_i[row][col] = mu_i[row][col-1] + mu_i[row-1][col] - mu_i[row-1][col-1]  + row * hists[idx];
				mu_j[row][col] = mu_j[row][col-1] + mu_j[row-1][col] - mu_j[row-1][col-1]  + col * hists[idx];
				
				if (p_0[row][col] == 0) {
					continue;
				}

				double tr = ((mu_i[row][col] - p_0[row][col] * mu_t0) * (mu_i[row][col] - p_0[row][col] * mu_t0) + (mu_j[row][col] - p_0[row][col] * mu_t1) * (mu_j[row][col] - p_0[row][col] * mu_t1)) 
						/ (p_0[row][col] * (1 - p_0[row][col]));

				if ( tr >= maximum ) {
					threshold = row;
					maximum = tr;
				}
				
				idx ++;
			}
			idx ++;
		}

		return threshold;
	}

}
