package edu.uchc.octane.core.datasource;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;

public class Localizations {

	final RawLocalizationData locData;
	DoubleSummaryStatistics [] stats;
	HashMap<String, Integer> headersMap;

	public final double [][]data;
	public double [] xCol, yCol, zCol, frameCol, errCol, intensityCol;

	public Localizations(RawLocalizationData raw) {
		locData = raw;
		this.data = locData.data;

		stats = new DoubleSummaryStatistics[data.length];

		headersMap = new HashMap<String, Integer> ();
		for (int i = 0; i < locData.headers.length; i++) {
			headersMap.put(locData.headers[i], i);
		}

		guessHeaders();
	}

	private void guessHeaders() {
		for (String key:headersMap.keySet()) {
			key = key.toLowerCase();
			int col = headersMap.get(key);
			if (key.startsWith("x ") || key.equals("x")) {
				xCol = data[col];
			} else if (key.startsWith("y ") || key.equals("y")) {
				yCol = data[col];
			} else if (key.startsWith("z ") || key.equals("z")) {
				zCol = data[col];
			} else if (key.startsWith("uncertain") || key.startsWith("err")) {
				errCol = data[col];
			} else if (key.startsWith("int")) {
				intensityCol = data[col];
			} else if (key.startsWith("frame")) {
				frameCol = data[col];
			}
		}

		if (frameCol == null) {
			frameCol = data[0];
		}
		if (xCol == null) {
			xCol = data[1];
		}
		if (yCol == null) {
			yCol = data[2];
		}
		if (errCol == null) {
			errCol = data[data.length -1 ];
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
		return frameCol.length;
	}

	public double getXAt(int idx) {
		return xCol[idx];
	}

	public double getYAt(int idx) {
		return yCol[idx];
	}
}
