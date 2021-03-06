package edu.uchc.octane.core.pixelimage;

/**
 * A rectangular pixel image with a double array (row-major or Y-major order) as its internal representation.
 */
public class RectangularDoubleImage extends RectangularImage {

	double [] data;
	
	/**
	 * A zero-filled rectangular image
	 * @param width
	 * @param height
	 */
	public RectangularDoubleImage(int width, int height) {
		this(width,  height, 0, 0);
	}
	
	/**
	 * A zero-filled rectangular image with offset 
	 * @param width
	 * @param height
	 * @param x0 X coordinate of top-left corner 
	 * @param y0 Y coordinate of top-left corner
	 */
	public RectangularDoubleImage(int width, int height, int x0, int y0) {
		this(new double[width * height], width, x0, y0);
	}

	/**
	 * A rectangular image with supplied pixel data.
	 * @param data First element corresponds to (0,0)
	 * @param imageWidth
	 */
	public RectangularDoubleImage(double [] data, int imageWidth) {
		this(data, imageWidth, 0, 0);
	}

	/**
	 * A rectangular image with offset using supplied pixel data.
	 * @param data
	 * @param imageWidth
	 * @param x0
	 * @param y0
	 */
	public RectangularDoubleImage(double [] data, int imageWidth, int x0, int y0) {
		this.data = data;
		this.x0 = x0;
		this.y0 = y0;
		width = imageWidth;
		height = data.length / width;

		if (width <= 0 || height * width != data.length) {
			throw new IllegalArgumentException("Image data length is not multiple of imagewidth.");
		}
	}

	/**
	 * Creates a rectangular sub-image from a rectangular image. Pixel data are copied.
	 * @param origData original image
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param fixBounding If true, fix illegal coordinates by shrinking them, otherwise, throw an exception
	 */
	public RectangularDoubleImage(RectangularDoubleImage origData, int x0, int y0, int width, int height, boolean fixBounding ) {

		fixBounds(origData, x0, y0, width, height, fixBounding);

		this.data = new double[width * height];
		int origIdx = (this.y0 - origData.y0) * origData.width + this.x0 - origData.x0;
		for (int i = 0; i < this.height; i ++) {
			System.arraycopy(origData.data, origIdx , this.data, i * this.width, this.width);
			origIdx += origData.width;
		}
	}

	/**
	 * Creates a rectangular subimage from a rectangular image. Pixel data are copied.
	 * @param origData
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 */
	public RectangularDoubleImage(RectangularDoubleImage origData, int x0, int y0, int width, int height) {
		this(origData, x0, y0, width, height, false);
	}

	/**
	 *Create a clone. Pixel data are duplicated.
	 */
	@Override
	public RectangularDoubleImage clone() {
		return new RectangularDoubleImage(data.clone(), width, x0, y0);
	}

	@Override
	public double[] getValueVector() {
		return data.clone();
	}

	@Override
	public void setValueVector(double[] values) {
		if (values.length != data.length) {
			throw(new IllegalArgumentException("Incorrect input array length: " + values.length));
		}
		data = values.clone();
	}

	@Override
	public double getValue(int idx) {
		return data[idx];
	}

	@Override
	public void setValue(int idx, double v) {
		data[idx] = v;
	}
}

