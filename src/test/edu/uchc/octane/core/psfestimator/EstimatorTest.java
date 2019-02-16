package edu.uchc.octane.core.psfestimator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;

public class EstimatorTest {

	@Test
	public void testEstimatorSimple() {
		int f = 2;
		int roiWidth = 3;
		double [] psf = {0,0,0,0,1,0,0,0,0};
		double [] centerXs= {0,0,.5,.5};
		double [] centerYs= {0,.5,0,.5};
		double bg = 0;
		double [] intensities = {1,1,1,1};		

		RectangularDoubleImage [] data = constructData(
				psf, centerXs, centerYs, bg, intensities, roiWidth, f); 
		double [] bgs = new double[data.length];
		Arrays.fill(bgs, bg);
		Estimator estimator = new Estimator(f, data, centerXs, centerYs, bgs, intensities);
		RectangularDoubleImage psfResult = estimator.estimate(3, 2.0);
		
		assertEquals(psfResult.getValue(0), 0, 1e-6);
		assertEquals(psfResult.getValue(4), 1.0, 1e-6);
	}
	
	@Test
	public void testEstimatorElaborate() {
		int f = 3;
		int roiWidth = 5;

		double [] psf = {
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0.25, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0.25, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0.5, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0			
		};

		double [] centerXs = {94.21,91.42,43.94,81.15,29.98,34.11,90.25,61.48,40.34,55.94,16.27,36.03,84.61,17.51,29.99,57.36,72.91,78.99};
		double [] centerYs = {5.46,66.54,36.85,33.42,17.12,25.73,15.36,82.75,2.01,40.71,30.06,46.81,69.16,89.05,59.59,48.97,78.29,74.83};

		double bg = 1;
		double [] intensities = {3,2,3,1,3,1,2,3,4,1,4,4,2,2,2,2,2,3};
		
		RectangularDoubleImage [] data = constructData(
				psf, centerXs, centerYs, bg, intensities, roiWidth, f); 
		double [] bgs = new double[data.length];
		Arrays.fill(bgs, bg);
		Estimator estimator = new Estimator(f, data, centerXs, centerYs, bgs, intensities);
		RectangularDoubleImage psfResult = estimator.estimate(7, 2.0);
		
		assertEquals(psfResult.getValue(0), 0, 1e-6);
		assertEquals(psfResult.getValue(8), 0.25, 1e-6);
	}

	RectangularDoubleImage [] constructData(double [] psf, double [] centerXs, double [] centerYs, double bg, double [] intensities, int roiWidth, int f) {
		RectangularDoubleImage [] data = new RectangularDoubleImage[centerXs.length];
		int psfWidth = (int) (FastMath.sqrt(psf.length));
		int psfHalfWidth = (psfWidth - 1) /2 ;
		int roiHalfWidth = (roiWidth - 1) / 2 ;
		
		for (int i = 0; i < data.length; i++) {
			RectangularDoubleImage img = new RectangularDoubleImage(
					roiWidth * f, 
					roiWidth * f, 
					((int)centerXs[i]-roiHalfWidth)*f, 
					((int)centerYs[i]-roiHalfWidth)*f);
			RectangularDoubleImage psfImage = new RectangularDoubleImage(
					psf,
					psfWidth,
					(int) (centerXs[i] * f - psfHalfWidth), 
					(int) (centerYs[i] * f - psfHalfWidth));
			img.copyFrom(psfImage);
			double [] imgValues = img.getValueVector();
			for (int k = 0; k < img.getLength(); k++) {
				imgValues[k] = imgValues[k] * intensities[i] + bg; 
			}
			data[i] = binImage(img, f);
		}
		
		return data;
	}
	
	RectangularDoubleImage binImage(RectangularDoubleImage img, int f) {
		RectangularDoubleImage newImg = new RectangularDoubleImage(img.width / f, img.height / f, img.x0 / f, img.y0 / f);
		
		for (int i = 0; i < newImg.getLength(); i++) {
			int x = newImg.getXCordinate(i) * f;
			int y = newImg.getYCordinate(i) * f;
			double s  = 0;
			for (int xi = x; xi < x + f; xi++) {
				for (int yi = y; yi < y + f; yi++) {
					s += img.getValueAtCoordinate(xi,  yi);
				}
			}
			newImg.setValue(i, s);
		}

		return newImg;
	}
	
	RectangularDoubleImage shiftImage(RectangularDoubleImage img, int dx, int dy) {
		
		RectangularDoubleImage newImg = img.clone();
		newImg.x0 = img.x0 + dx;
		newImg.y0 = img.y0 + dy;
		
		return newImg;
	}
}
