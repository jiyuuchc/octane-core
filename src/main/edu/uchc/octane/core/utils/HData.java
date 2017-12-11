package edu.uchc.octane.core.utils;

/* interface for an immutable collection of high-dimensional real data */

public interface HData {

	int getDimension();
	int size();
	double get(int idx, int d);
	void selectDimension(int d);
	double get(int idx);

}
