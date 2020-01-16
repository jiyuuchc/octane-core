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
	
	public void fixBounds(RectangularImage origData, int x0, int y0, int width, int height, boolean fixBounding) {
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
	}
}
