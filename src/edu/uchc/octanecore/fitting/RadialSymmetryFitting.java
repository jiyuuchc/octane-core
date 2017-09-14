package edu.uchc.octanecore.fitting;

import edu.uchc.octanecore.datasource.RectangularImage;

/**
 * Ref:
 * "Rapid, accurate particle tracking by calculation of radial symmetry centers"
 * by Raghuveer Parthasarathy
 *
 */

public class RadialSymmetryFitting {

	public RectangularImage data;

	public static class Params {
		public static final int X = 0;
		public static final int Y = 1;
		public static final int PARAMS_LENGTH = 2;
	}

	public RadialSymmetryFitting() {

	}

	public double [] fit(RectangularImage data) {

		this.data = data;
		int [] size = {data.width, data.height};

		float[] dIdu = computeGradientImage(false);
		float[] dIdv = computeGradientImage(true);

		smooth(dIdu, size[0] - 1);
		smooth(dIdv, size[0] - 1);

		float[] m = calculateSlope(dIdu, dIdv);
		float[] xMesh = createMesh(size[0], true);
		float[] yMesh = createMesh(size[1], false);

		float[] yInterceptB = calculateYIntercept(xMesh, yMesh, m);

		float[] weights = calculateWeights(dIdu, dIdv, xMesh, yMesh);

		double [] result = lsRadialCenterFit(m, yInterceptB, weights);

		result[Params.X] += data.getXCordinate(0) + size[0] / 2.0 - 0.5;
		result[Params.Y] += data.getYCordinate(0) + size[1] / 2.0 - 0.5;

		return result;
	}

	/**
	 * Computes gradient image along 45-degree shifted coordinates.
	 *
	 * @param img
	 * @param mainDiagonalDirection specifies the direction in which the
	 * gradient will be computed, true for main diagonal and false for
	 * antidiagonal
	 * @return
	 */
	private float[] computeGradientImage(boolean mainDiagonalDirection) {

		int [] size = {data.width, data.height};
		float[] dI = new float[(size[0] - 1) * (size[1] - 1)];

		int idx1 = mainDiagonalDirection ? 0 : 1;
		int idx2 = mainDiagonalDirection ? size[0] + 1 : size[0];

		int resIdx = 0;
		for(int i = 0; i < size[1] - 1; i++) {
			for(int j = 0; j < size[0] - 1; j++) {
				dI[resIdx++] = (float) (data.getValue(idx1++) - data.getValue(idx2++));
			}
			idx1++;
			idx2++;
		}

		return dI;
	}

	/**
	 * smoothing by 3*3 box filter
	 */
	private void smooth(float[] dIdu, int size) {

		float [] tmp = new float[(size + 2) * (size + 2)];

		//copy into padded array
		for (int i = 0; i < size ; i ++ ) {
			System.arraycopy(dIdu, i * size, tmp, size + 3 + i * (size + 2), size);
		}

		int idx = 0;
		int idx2 = size + 3;
		for (int i = 0; i < size ; i ++ ) {
			for (int j = 0; j < size; j ++) {
				dIdu[idx ++ ]
						= tmp[idx2] + tmp[idx2-1] + tmp[idx2+1]
						+ tmp[idx2 - size - 2] + tmp[idx2 - size - 1] + tmp[idx2 - size - 3]
						+ tmp[idx2 + size + 2] + tmp[idx2 + size + 1] + tmp[idx2 + size + 3];
				idx2 ++;
			}

			idx2 += 2;
		}
	}

	private float[] calculateSlope(float[] dIdu, float[] dIdv) {
		float[] m = new float[dIdu.length];

		for(int i = 0; i < m.length; i++) {
			float val = -(dIdu[i] + dIdv[i]) / (dIdu[i] - dIdv[i]);
			val = Float.isNaN(val) ? 0 : val;
			val = Float.isInfinite(val) ? Float.MAX_VALUE / 1e5f : val; //replace inf by some big value - Not max_value because it could overflow to infinity in next step
			m[i] = val;
		}
		return m;
	}

	private float[] createMesh(int size, boolean xMesh) {
		float[] mesh = new float[(size - 1) * (size - 1)];
		int smallSize = (size - 1) / 2;

		int idx = 0;
		for(int i = 0; i < size - 1; i++) {
			float iVal = -smallSize + 0.5f + i;
			for(int j = 0; j < size - 1; j++) {
				float jVal = -smallSize + 0.5f + j;
				mesh[idx++] = xMesh ? jVal : iVal;
			}
		}
		return mesh;
	}

	/**
	 * b in original matlab implementation
	 */
	private float[] calculateYIntercept(float[] xMesh, float[] yMesh, float[] m) {
		float[] intercept = new float[m.length];
		for(int i = 0; i < intercept.length; i++) {
			intercept[i] = yMesh[i] - m[i] * xMesh[i];
		}
		return intercept;
	}

	/**
	 * weight by square of gradient magnitude and inverse distance to gradient
	 * intensity centroid.
	 */
	private float[] calculateWeights(float[] dIdu, float[] dIdv, float[] xMesh, float[] yMesh) {
		float gradientMagnitudeSum = 0;
		float xCentroid = 0;
		float yCentroid = 0;
		for(int i = 0; i < dIdu.length; i++) {
			float gradientMagnitude = dIdu[i] * dIdu[i] + dIdv[i] * dIdv[i];
			gradientMagnitudeSum += gradientMagnitude;

			xCentroid += xMesh[i] * gradientMagnitude;
			yCentroid += yMesh[i] * gradientMagnitude;
		}
		xCentroid /= gradientMagnitudeSum;
		yCentroid /= gradientMagnitudeSum;

		float[] weights = new float[dIdu.length];

		for(int i = 0; i < weights.length; i++) {
			float gradientMagnitude = dIdu[i] * dIdu[i] + dIdv[i] * dIdv[i];
			double distanceToCentroid = Math.sqrt((xMesh[i] - xCentroid) * (xMesh[i] - xCentroid) + (yMesh[i] - yCentroid) * (yMesh[i] - yCentroid));
			weights[i] = (float) (gradientMagnitude / distanceToCentroid);
		}
		return weights;
	}

	/**
	 * least-squares minimization to determine the translated coordinate system
	 * origin (xc, yc) such that lines y = mx+b have the minimal total
	 * distance^2 to the origin:
	 */
	private double[] lsRadialCenterFit(float[] m, float[] b, float[] weights) {
		double sw = 0;
		double smmw = 0;
		double smw = 0;
		double smbw = 0;
		double sbw = 0;

		for(int i = 0; i < m.length; i++) {
			double weighted = weights[i] / (m[i] * m[i] + 1); //wm2p1
			sw += weighted;
			double mw = weighted * m[i];
			smw += mw;
			smmw += mw * m[i];
			smbw += mw * b[i];
			sbw += weighted * b[i];
		}
		double det = smw * smw - smmw * sw;
		double xc = (smbw * sw - smw * sbw) / det;    // relative to image center
		double yc = (smbw * smw - smmw * sbw) / det; // relative to image center

		return new double[]{xc, yc};
	}
}
