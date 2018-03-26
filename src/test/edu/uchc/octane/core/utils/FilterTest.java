package edu.uchc.octane.core.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import edu.uchc.octane.core.utils.ImageFilters;

public class FilterTest {

    //final Logger logger = LoggerFactory.getLogger(getClass());
    
	@Test
	public void testSymmetricGaussianFilter() {
		double [][] img = new double[8][8];

		img[3][3] = 1.0;

		img = ImageFilters.symmetricFilter(ImageFilters.makeGaussianFilter(1.5, 7), img);
//		for (int i = 0; i < img.length; i ++) {
//			for (int j = 0 ; j < img[i].length; j++) {
//				System.out.print(img[i][j]);
//				System.out.print(", ");
//			}
//			System.out.println();
//		}

		assertEquals(0.07327, img[3][3], 0.001);

		double [] img2 = new double[64];
		img2[27] = 1.0;
		img2 = ImageFilters.symmetricFilter(ImageFilters.makeGaussianFilter(1.5, 7), img2, 8);
		assertEquals(0.07327, img2[27], 0.0001);
	}
	
//    @Test
//	public void testFFTFilter() {
//        int size = 6;
//        int imgSize = 8; 
//	    double [] f = ImageFilters.makeGaussianFilter(1.5, size);
//	    double [] f2 = new double[size * size];
//	    for (int i = 0; i < size; i ++ ) {
//	        for (int j = 0; j < size; j ++) {
//	            f2[i * size + j] = f[i] * f[j];
//	        }
//	    }
//	    double [] t2 = new double[imgSize * imgSize];
//	    t2[2*imgSize+2] = 1.0;
//	    RectangularDoubleImage filter = new RectangularDoubleImage(f2, size);
//	    RectangularDoubleImage target = new RectangularDoubleImage(t2, imgSize);
//	    RectangularDoubleImage ret = ImageFilters.filterByFFT(filter, target);
//	    
//	    for (int i = 0; i < imgSize; i ++) {
//	        for (int j = 0 ; j < imgSize; j++) {
//	            System.out.print(ret.getValueAtCoordinate(i, j));
//	            System.out.print(", ");
//	        }
//	        System.out.println();
//	    }
//	}
}
