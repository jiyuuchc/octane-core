package edu.uchc.octane.core.drift;

import org.junit.Test;
import static org.junit.Assert.*;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.drift.Basdi;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;

public class BasdiTest {

	@Test
	public void testExyf2() {
		System.out.println("Basdi test: exyf2 function");
		RectangularDoubleImage logtheta = new RectangularDoubleImage(new double[64*64], 64);
		logtheta.setValueAtCoordinate(31, 31, 1);
		logtheta.setValueAtCoordinate(31, 30, 1);
		logtheta.setValueAtCoordinate(31, 29, 1);		
		int [][] locs = new int[][] { {31,32}, {31,32}};
		Basdi basdi = new Basdi();
		basdi.maxShift = 4;
		RectangularDoubleImage result = basdi.exyf2(logtheta, locs);
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
		RectangularDoubleImage mpk = new RectangularDoubleImage(new double[81], 9, -4 , -4);
		mpk.setValueAtCoordinate(0, 0, 1.0);
		mpk.setValueAtCoordinate(-1, 0, 1.0);
		mpk.setValueAtCoordinate(0, -1, 1.0);
		RectangularDoubleImage newTheta = new RectangularDoubleImage(new double[64*64], 64);
		
		basdi.updateThetaOneFrame(newTheta, mpk, locs);	
		//take subimage
		RectangularDoubleImage result = new RectangularDoubleImage(newTheta, 31, 32, 3, 3);

		assertNotNull(result);
		double [] expected = { 0, 1, 0, 1, 2, 0, 1, 1, 0 };
		assertArrayEquals(expected, result.getValueVector(), 0.001);
	}

    @Test
    public void testBasdiSimple() {
        
        double [] farray = {1, 1, 2, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double [] xarray = {0, 10, 1, 11, 2, 13, 4, 15, 2, 12, 0, 10};
        double [] yarray = {0, 10, 10, 10, 0, 10, 0, 10, 0, 10, 0, 10};
        double [][] data = {xarray, yarray, farray};
        String [] header = {"x",  "y", "frame"};
        
        OctaneDataFile odf = new OctaneDataFile(data, header);
        Basdi basdi = new Basdi(10, 1);
        
        basdi.estimate(odf, 10);
        
        double [][] results = basdi.drifts;
        
        for (int i = 0; i < 10; i ++) {
            System.out.println("Frame " + i + "- x:" + results[0][i] + " y:" + results[1][i]);            
        }
        
        double [] expected = {0, 0, 1, 2, 3, 4, 2, 1, 0, 0};
        assertArrayEquals(expected, results[0], 1.0);
    }
}
