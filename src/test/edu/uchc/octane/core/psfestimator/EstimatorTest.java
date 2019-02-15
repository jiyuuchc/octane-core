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
		int [] centerXs= {0,0,1,1};
		int [] centerYs= {0,1,0,1};
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
		int f = 2;
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

		int [] centerXs = {75,25,50,69,89,95,54,13,14,25,84,25,81,24,92,34,19,25,61};
		int [] centerYs = {7,7,53,77,93,12,56,46,11,33,16,79,31,52,16,60,26,65,68};

		double bg = 1;
		double [] intensities = {3,2,3,1,3,1,2,3,4,1,4,4,2,2,2,2,2,3,3};
		
		RectangularDoubleImage [] data = constructData(
				psf, centerXs, centerYs, bg, intensities, roiWidth, f); 
		double [] bgs = new double[data.length];
		Arrays.fill(bgs, bg);
		Estimator estimator = new Estimator(f, data, centerXs, centerYs, bgs, intensities);
		RectangularDoubleImage psfResult = estimator.estimate(7, 2.0);
		
		assertEquals(psfResult.getValue(0), 0, 1e-6);
		assertEquals(psfResult.getValue(8), 0.25, 1e-6);
	}

	RectangularDoubleImage [] constructData(double [] psf, int [] centerXs, int [] centerYs, double bg, double [] intensities, int roiWidth, int f) {
		RectangularDoubleImage [] data = new RectangularDoubleImage[centerXs.length];
		int psfWidth = (int) (FastMath.sqrt(psf.length));
		int psfHalfWidth = (psfWidth - 1) /2 ;
		int roiHalfWidth = (roiWidth - 1) / 2 ;
		
		for (int i = 0; i < data.length; i++) {
			RectangularDoubleImage img = new RectangularDoubleImage(
					roiWidth * f, 
					roiWidth * f, 
					(centerXs[i]/f-roiHalfWidth)*f, 
					(centerYs[i]/f-roiHalfWidth)*f);
			RectangularDoubleImage psfImage = new RectangularDoubleImage(
					psf,
					psfWidth,
					centerXs[i] - psfHalfWidth, 
					centerYs[i] - psfHalfWidth);
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
