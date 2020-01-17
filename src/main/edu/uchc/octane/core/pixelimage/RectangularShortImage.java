package edu.uchc.octane.core.pixelimage;

/**
 * A rectangular pixel image with a short array as its internal representation.
 */
public class RectangularShortImage extends RectangularImage {

	short [] data;

	/**
	 * A zero-filled rectangular image
	 * @param width
	 * @param height
	 */
	public RectangularShortImage(int width, int height) {
		this(width,  height, 0, 0);
	}
	
	/**
	 * A zero-filled rectangular image with offset 
	 * @param width
	 * @param height
	 * @param x0 X coordinate of top-left corner 
	 * @param y0 Y coordinate of top-left corner
	 */
	public RectangularShortImage(int width, int height, int x0, int y0) {
		this(new short[width * height], width, x0, y0);
	}

	/**
	 * A rectangular image with supplied pixel data.
	 * @param data First element corresponds to (0,0)
	 * @param imageWidth
	 */
	public RectangularShortImage(short [] data, int imageWidth) {
		this(data, imageWidth, 0, 0);
	}

	/**
	 * A rectangular image with offset using supplied pixel data.
	 * @param data
	 * @param imageWidth
	 * @param x0
	 * @param y0
	 */
	public RectangularShortImage(short [] data, int imageWidth, int x0, int y0) {
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
	public RectangularShortImage(RectangularShortImage origData, int x0, int y0, int width, int height, boolean fixBounding ) {

		if (width <= 0 || height <=0 || width > origData.width || height > origData.height) {
			throw new IllegalArgumentException("Invalid image size.");
		}

		if ( ! origData.isCoordinateValid(x0, y0) || ! origData.isCoordinateValid(x0 + width - 1 , y0 + height - 1) ) {
			if (! fixBounding) {
				throw new IllegalArgumentException("subimage region out of bound.");
			} else {
				if (x0 < origData.x0) {
					x0 = origData.x0;
				}
				if (y0 < origData.y0) {
					y0 = origData.y0;
				}

				if (x0 + width > origData.x0 + origData.width) {
					x0 = origData.x0 + origData.width - width;
				}
				if (y0 + height > origData.y0 + origData.height) {
					y0 = origData.y0 + origData.height - height;
				};
			}
		}
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;		

		this.data = new short[width * height];
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
	public RectangularShortImage(RectangularShortImage origData, int x0, int y0, int width, int height) {
		this(origData, x0, y0, width, height, false);
	}

	/**
	 *Create a clone. Pixel data are duplicated.
	 */
	@Override
	public RectangularShortImage clone() {
		return new RectangularShortImage(data.clone(), width, x0, y0);
	}

	@Override
	public double getValue(int idx) {
		return (double) (data[idx]);
	}

	@Override
	public void setValue(int idx, double v) {
		data[idx] = (short) v; 
	}

	@Override
	public RectangularImage getSubImage(int x0, int y0, int width, int height, boolean fixBounding) {
		return new RectangularShortImage(this, x0, y0, width, height, fixBounding);
	}
}
