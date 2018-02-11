package edu.uchc.octane.core.drift;

import org.junit.Test;
import static org.junit.Assert.*;

import edu.uchc.octane.core.datasource.RectangularImage;
import edu.uchc.octane.core.drift.Basdi;

public class BasdiTest {

	@Test
	public void testExyf2() {
		System.out.println("Basdi test: exyf2 function");
		RectangularImage logtheta = new RectangularImage(new double[64*64], 64);
		logtheta.setValueAtCoordinate(31, 31, 1);
		logtheta.setValueAtCoordinate(31, 30, 1);
		logtheta.setValueAtCoordinate(31, 29, 1);		
		int [][] locs = new int[][] { {31,32}, {31,32}};
		Basdi basdi = new Basdi();
		basdi.maxShift = 4;
		RectangularImage result = basdi.exyf2(logtheta, locs);
		double [] expected = { 
				0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.02978, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.02978, 0.02978, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.02978, 0.02978, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.01095, 0.02978, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095,
				0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.0109,
				0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095, 0.01095
		};
		assertNotNull(result);
		assertArrayEquals(expected, result.getValueVector(), 0.0001);
	}
	
	@Test
	public void testUpdateThetaOneFrame() {
		System.out.println("Basdi test: updateThetaOneFrame");
		int [][] locs = new int [][] { {32,32}, {33, 34}};
		Basdi basdi = new Basdi();
		basdi.maxShift = 4;
		RectangularImage mpk = new RectangularImage(new double[81], 9, -4 , -4);
		mpk.setValueAtCoordinate(0, 0, 1.0);
		mpk.setValueAtCoordinate(-1, 0, 1.0);
		mpk.setValueAtCoordinate(0, -1, 1.0);
		RectangularImage result = new RectangularImage(new double[64*64], 64);
		basdi.updateThetaOneFrame(result, mpk, locs);
		RectangularImage result1 = new RectangularImage(result, 31, 32, 3, 3);
		assertNotNull(result1);
		double [] expected = { 0, 1, 0, 1, 2, 0, 1, 1, 0 };
		assertArrayEquals(expected, result1.getValueVector(), 0.001);
	}
}
