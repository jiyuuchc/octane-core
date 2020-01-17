package edu.uchc.octane.core.pixelimage;

/**
 * Rectangular pixel image assuming row-major order of internal data
 */
public abstract class RectangularImage extends PixelImageBase {

	// x0, y0 is the top-left coordinate of the data
	public int x0, y0,width, height;

	@Override
	public int getXCordinate(int idx) {
		return idx % width + x0;
	}

	@Override
	public int getYCordinate(int idx) {
		return idx / width + y0;
	}

	@Override
	public int getIndexOfCoordinate(int x, int y) {
		return x - x0 + (y - y0) * width;
	}

	@Override
	public boolean isCoordinateValid(int x, int y) {
		return (x >= x0 && x < x0 + width && y >= y0 && y < y0 + height );
	}
	
	@Override
	public int getLength() {
		return width * height;
	}
	
	/**
	 * Return a subimage of this image
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param fixBounding
	 * @return
	 */
	public abstract RectangularImage getSubImage(int x0, int y0, int width, int height, boolean fixBounding);
	public RectangularImage getSubImage(int x0, int y0, int width, int height) {
		return getSubImage(x0, y0, width, height, false);
	}
}
