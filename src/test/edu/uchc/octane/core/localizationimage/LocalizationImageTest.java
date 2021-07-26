package edu.uchc.octane.core.localizationimage;

import static org.junit.Assert.assertArrayEquals;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import edu.uchc.octane.core.data.LocalizationData;

public class LocalizationImageTest {

    @Test
    public void testRotation() {
        String [] header = {"x", "y"};
        double [][] data = {
                {1,2,3},
                {4,5,6}
        };

        LocalizationImage img = new LocalizationImage(new LocalizationData(data, header));
        double theta = -FastMath.PI / 2; 
        img.rotate(theta, 0, 0);
        
        double [] expected = {4,5,6};
        assertArrayEquals(data[0], expected, 0.001);
    }
}
