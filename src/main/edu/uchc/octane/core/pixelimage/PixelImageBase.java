package edu.uchc.octane.core.pixelimage;

/**
 * Base class for 2d pixel image.
 * Image is defined as a dictionary set of pixel values (always double), keyed by a 2d coordinate pair (x,y) (integers)
 * Image is also a vector of pixel values indexed by 0-based indices.
 * The class defines a conversion between indices with (x,y) keys.    
 */
public abstract class PixelImageBase {

	/**
	 * Get a copy of all pixel values
	 * @return
	 */
	public double [] getValueVector() {
		double [] vector = new double[getLength()];
		for (int i = 0; i < getLength(); i++) {
			vector[i] = getValue(i);
		}
		return vector;
	};

	/**
	 * Set pixel values of all pixels in the image
	 * @param values
	 */
	public void setValueVector(double [] values) {
		if (values.length != getLength()) {
			throw(new IllegalArgumentException("Incorrect input array length: " + values.length));
		}
		for (int i = 0 ; i < values.length; i++) {
			setValue(i, values[i]);
		}
	};

	/**
	 * @param idx index
	 * @return x 
	 */
	public abstract int getXCordinate(int idx);
	/**
	 * @param idx index
	 * @return y
	 */
	public abstract int getYCordinate(int idx);

	/**
	 * Get pixel value by index
	 * @param idx index
	 * @return pixel value
	 */
	public abstract double getValue(int idx);

	/**
	 * Set pixel value by index
	 * @param idx index
	 * @param v value
	 */
	public abstract void setValue(int idx, double v);

	/**
	 * Convert 2D coordinates to index
	 * @param x
	 * @param y
	 * @return index
	 */
	public abstract int getIndexOfCoordinate(int x, int y);

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public abstract boolean isCoordinateValid(int x, int y);

	public abstract int getLength(); 

	/**
	 * Get pixel value by coordinates
	 * @param x
	 * @param y
	 * @return pixel value
	 */
	public double getValueAtCoordinate(int x, int y) {
		
		return getValue(getIndexOfCoordinate(x,y));

	}

	/**
	 * Set pixel value by coordinates
	 * @param x
	 * @param y
	 * @param v pixel value
	 */
	public void setValueAtCoordinate(int x, int y, double v) {

		setValue(getIndexOfCoordinate(x,y), v);

	}

	/**
	 * Copy pixels values from another 2d image, if coordinates match.
	 * @param data2 another 2d image
	 */
	public void copyFrom(PixelImageBase data2) {

		for (int i = 0; i < data2.getLength(); i++) {
			setValueAtCoordinate(data2.getXCordinate(i), data2.getYCordinate(i), data2.getValue(i));
		}

	}

	/**
	 * @param idx index
	 * @return array of indices that are neighbors (up to 8)
	 */
	public int [] getNeighboursIndices(int idx) {

		int [] idx1 = new int[8];
		int cnt = 0;
		int x = getXCordinate(idx);
		int y = getYCordinate(idx);

		final int [] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
		final int [] dy = {-1, -1, -1, 0, 0, 1, 1, 1};

		for (int i = 0; i < dx.length; i ++) {
			if (isCoordinateValid(x + dx[i], y + dy[i])) {
				idx1[cnt++] = this.getIndexOfCoordinate(x + dx[i], y + dy[i]);
			}
		}

		int [] idx2 = new int[cnt];
		System.arraycopy(idx1, 0, idx2, 0, cnt);
		return idx2;
	}
}
