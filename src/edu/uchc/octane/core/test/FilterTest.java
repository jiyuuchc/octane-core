package edu.uchc.octane.core.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.uchc.octane.core.utils.ImageFilters;

public class FilterTest {

	@Test
	public void testSymmetricGaussianFilter() {
		double [][] img = new double[8][8];

		img[3][3] = 1.0;

		img = ImageFilters.symmetricFilter(ImageFilters.makeGaussianFilter(1.5, 7), img);
		for (int i = 0; i < img.length; i ++) {
			for (int j = 0 ; j < img[i].length; j++) {
				System.out.print(img[i][j]);
				System.out.print(", ");
			}
			System.out.println();
		}

		assertEquals(0.07327, img[3][3], 0.001);

		double [] img2 = new double[64];
		img2[27] = 1.0;
		img2 = ImageFilters.symmetricFilter(ImageFilters.makeGaussianFilter(1.5, 7), img2, 8);
		assertEquals(0.07327, img2[27], 0.0001);
	}
}
