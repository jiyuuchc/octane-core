package edu.uchc.octane.core.datasource;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;

import edu.uchc.octane.core.utils.FastKDTree;
import edu.uchc.octane.core.utils.HDataCollection;

public class Localizations {

	final octaneDataFile locData;
	DoubleSummaryStatistics [] stats;
	HashMap<String, Integer> headersMap;

	public final double [][]data;
	public int xCol = 1, yCol = 2, zCol = -1, frameCol = 0, errCol = -1, intensityCol = -1, sigmaCol = -1, densityCol = -1;

	FastKDTree tree;

	class HDataImp implements HDataCollection {

		private double[][] data_;
		private int d = 0;

		public HDataImp() {
			data_ = new double[2][];
			data_[0] = data[xCol];
			data_[1] = data[yCol];
		}

		@Override
		public int getDimension() {
			return 2;
		}

		@Override
		public int size() {
			return getNumLocalizations();
		}

		@Override
		public double get(int idx, int d) {
			return data_[d][idx];
		}

		@Override
		public void selectDimension(int d) {
			this.d = d;

		}

		@Override
		public double get(int idx) {
			return get(idx, d);
		}

	}

	public Localizations(octaneDataFile raw) {
		locData = raw;
		data = new double[raw.data.length + 1][]; // last column is for storing local density
		System.arraycopy(raw.data, 0, data, 0, raw.data.length);

		stats = new DoubleSummaryStatistics[data.length];

		headersMap = new HashMap<String, Integer> ();
		for (int i = 0; i < locData.headers.length; i++) {
			headersMap.put(locData.headers[i], i);
		}
		headersMap.put("LocalDensity", data.length - 1);

		guessHeaders();
	}

	private void guessHeaders() {
		for (String key:headersMap.keySet()) {
			key = key.toLowerCase();
			int col = headersMap.get(key);
			if (key.startsWith("x ") || key.equals("x")) {
				xCol = col;
			} else if (key.startsWith("y ") || key.equals("y")) {
				yCol = col;
			} else if (key.startsWith("z ") || key.equals("z")) {
				zCol = col;
			} else if (key.startsWith("uncertain") || key.startsWith("err")) {
				errCol = col;
			} else if (key.startsWith("int")) {
				intensityCol = col;
			} else if (key.startsWith("frame")) {
				frameCol = col;
			} else if (key.startsWith("sigma")) {
				sigmaCol = col;
			}
		}

		if (errCol == -1) {
			errCol = data.length -1;
		}
	}

	public DoubleSummaryStatistics getSummaryStatitics(int col) {

		if ( stats[col] == null) {
			stats[col]=  Arrays.stream(data[col]).summaryStatistics();
		}
		return stats[col];
	}

	public DoubleSummaryStatistics getSummaryStatitics(String header) {
		if (headersMap.get(header) != null ) {
			return getSummaryStatitics(headersMap.get(header));
		} else {
			return null;
		}
	}

	double [] getData(int col) {
		return data[col];
	}

	double [] getData(String colHeader) {
		return data[headersMap.get(colHeader)];
	}

	public String[] getHeaders() {
		return locData.headers;
	}

	public int getNumLocalizations() {
		return data[frameCol].length;
	}

	public double getXAt(int idx) {
		return data[xCol][idx];
	}

	public double getYAt(int idx) {
		return data[yCol][idx];
	}

	public double getZAt(int idx) {
		return data[zCol][idx];
	}

	public void constructKDtree() {
		tree = new FastKDTree(new HDataImp());
	}

	public void measureLocalDensity(double distance) {
		assert(tree != null);
		int localDensityCol = data.length -1 ;
		for (int i = 0; i < getNumLocalizations(); i++) {
			data[localDensityCol][i] = tree.radiusSearch(i, distance).size();
		}
		densityCol = localDensityCol;
	}
}
