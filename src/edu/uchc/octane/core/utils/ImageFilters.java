package edu.uchc.octane.core.utils;

import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.util.FastMath;

public class ImageFilters {

	public static double [] makeGaussianFilter(double sigma, int size) {
		return makeGaussianFilter(sigma, size, false);
	}

	public static double [] makeGaussianFilter(double sigma, int size, boolean useErf) {

		double [] filter = new double[size];

		double s = 0;
		for (int i = 0; i < size; i ++) {

			double x = -(double)(size - 1)/2.0 + i;
			if (useErf) {
				filter[i] = (Erf.erf((x + 0.5) / sigma / Math.sqrt(2)) - Erf.erf((x - 0.5) / sigma / Math.sqrt(2)));
			} else {
				filter[i] = FastMath.exp((- x * x) / (2 * sigma * sigma));
			}
			s += filter[i];

		}

		for (int i = 0; i < size; i ++) {

			filter[i] /= s;

		}

		return filter;
	}

	// filter a with linear filter h column and row wise.
	// useful for separatable 2d filtering such as symmetric gaussian filter
	public static double[][] symmetricFilter(double[] h, double[][] a) {

		double[][] result = new double[a.length][a[0].length];
		double[][] result2 = new double[a.length][a[0].length];

		int halfSizeL, halfSizeR;

		halfSizeL = h.length / 2;
		halfSizeR = h.length - halfSizeL;

		//row-wise
		double s;
		//double sa = 0;
		for (int i = 0; i < a.length; i ++) {
			for (int j = 0; j < a[0].length; j++) {

				s = 0;
				for (int k = FastMath.max(0, j - halfSizeL); k < FastMath.min(a[0].length, j + halfSizeR); k++) {
					s += a[i][k] * h[halfSizeL + k - j];
				}
				result[i][j] = s;

				// sa += a[i][j]; //also calculate sum(a(:))

			}
		}

		//column-wise
		for (int j = 0; j < a[0].length; j ++) {
			for (int i = 0; i < a.length; i++) {

				s = 0;
				for (int k = FastMath.max(0, i - halfSizeL); k < FastMath.min(a.length, i + halfSizeR); k++) {
					s += result[k][j] * h[halfSizeL + k - i];
				}
				result2[i][j] = s;
			}
		}

		return result2;
	}

	public static double[] symmetricFilter(double[] h, double[] a, int width) {

		double[] result = new double[a.length];
		double[] result2 = new double[a.length];

		int halfSizeL, halfSizeR;

		halfSizeL = h.length / 2;
		halfSizeR = h.length - halfSizeL;

		int height = a.length / width;

		//row-wise
		double s;
		//double sa = 0;
		for (int i = 0; i < height; i ++) {
			for (int j = 0; j < width; j++) {

				s = 0;
				for (int k = FastMath.max(0, j - halfSizeL); k < FastMath.min(width, j + halfSizeR); k++) {
					s += a[i * width + k] * h[halfSizeL + k - j];
				}
				result[i * width + j] = s;

				// sa += a[i][j]; //also calculate sum(a(:))

			}
		}

		//column-wise
		for (int j = 0; j < width; j ++) {
			for (int i = 0; i < height; i++) {

				s = 0;
				for (int k = FastMath.max(0, i - halfSizeL); k < FastMath.min(height, i + halfSizeR); k++) {
					s += result[k * width + j] * h[halfSizeL + k - i];
				}
				result2[i * width + j] = s;
			}
		}

		return result2;
	}

}
