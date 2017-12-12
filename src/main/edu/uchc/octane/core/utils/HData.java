package edu.uchc.octane.core.utils;

public interface HData {

	int getDimension();
	double get(int d);

	default double sqDistance(HData p) {
		double d = 0;
		for (int i = 0; i < getDimension(); i ++) {
			d+= (get(i) - p.get(i)) * (get(i) - p.get(i));
		}
		return d;
	}
}
