package edu.uchc.octane.core.datasource;

public abstract class ImageData {

	public abstract double [] getValueVector();
	public abstract void setValueVector(double [] values);

	public abstract int getXCordinate(int idx);
	public abstract int getYCordinate(int idx);

	public double getValue(int idx) {
		return getValueVector()[idx];
	}

	public void setValue(int idx, double v) {
		getValueVector()[idx] = v;;
	}

	public abstract int getIndexOfCoordinate(int x, int y);
	public abstract boolean isCoordinateValid(int x, int y);

	public int getLength() {

		return getValueVector().length;

	}

	public double getValueAtCoordinate(int x, int y) {

		return getValue(getIndexOfCoordinate(x,y));

	}

	public void setValueAtCoordinate(int x, int y, double v) {

		getValueVector()[getIndexOfCoordinate(x,y)] = v;

	}

	public void copyFrom(ImageData data2) {

		for (int i = 0; i < data2.getLength(); i++) {
			setValueAtCoordinate(data2.getXCordinate(i), data2.getYCordinate(i), data2.getValue(i));
		}

	}

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
