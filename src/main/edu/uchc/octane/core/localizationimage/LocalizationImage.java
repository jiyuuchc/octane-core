package edu.uchc.octane.core.localizationimage;

import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.utils.FastKDTree;
import edu.uchc.octane.core.utils.HDataCollection;

public class LocalizationImage {

	final Logger logger = LoggerFactory.getLogger(getClass());

	final OctaneDataFile dataSrc;
	SummaryStatistics [] stats = null;
	HashMap<String, Integer> headersMap = null;
    FastKDTree tree;

	public final double [][]data;
	public int xCol = 1, yCol = 2, zCol = -1, frameCol = 0, errCol = -1, intensityCol = -1, sigmaCol = -1, densityCol = -1;

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

	public LocalizationImage(OctaneDataFile raw) {
		dataSrc = raw;
		data = new double[raw.data.length + 1][]; // last column is for storing local density
		System.arraycopy(raw.data, 0, data, 0, raw.data.length);

		stats = new SummaryStatistics[data.length];

		if (headersMap == null) {
            headersMap = new HashMap<String, Integer> ();
            for (int i = 0; i < dataSrc.headers.length; i++) {
                headersMap.put(dataSrc.headers[i], i);
            }
            headersMap.put("LocalDensity", data.length - 1);            
        }
		
		guessHeaders();
	}
	
	public LocalizationImage(LocalizationImage loc) {
		this(new OctaneDataFile(loc.dataSrc));
		for (int i = 0; i < stats.length; i++) {
		    if (loc.stats[i] != null) {
		        stats[i] = new SummaryStatistics(loc.stats[i]);
		    }
		}
	}

	void guessHeaders() {
		for (String key:headersMap.keySet()) {
			int col = headersMap.get(key);
			key = key.toLowerCase();
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

	}
	
	public SummaryStatistics getSummaryStatitics(int col) {

	    if (col < 0 || col >= data.length) {
	        return null;
	    }
		if ( stats[col] == null) {
		    for (double d : data[col]) {
		        stats[col].addValue(d);
		    }
		}
		return stats[col];
	}

	public SummaryStatistics getSummaryStatitics(String header) {
	    Integer col = getColFromHeader(header);
		if (col != null ) {
			return getSummaryStatitics(col);
		} else {
			return null;
		}
	}

	public double [] getData(int col) {
		return data[col];
	}

	public double [] getData(String colHeader) {
		return getData(getColFromHeader(colHeader));
	}

	public String[] getHeaders() {
		return dataSrc.headers;
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
	
    public String getHeader(int i) {
        return dataSrc.headers[i];
    }
    
    public Integer getColFromHeader(String s) {
        
        return headersMap.get(s);
    }

    //in default axis convention:
	// rotation around top left corner (0,0)
	// positive theta values rotate counter-clock-wise
	public void rotate(double theta) {
		double [] xData = getData(xCol);
		double [] yData = getData(yCol);
		double cosTheta = FastMath.cos(theta);
		double sinTheta = FastMath.sin(theta);
		for (int i = 0; i < getNumLocalizations(); i ++) {
			xData[i] = cosTheta * xData[i] - sinTheta * yData[i];
			yData[i] = sinTheta * xData[i] + cosTheta * yData[i];
		}
	}
	
	public void translate(double dx, double dy) {
        double [] xData = getData(xCol);
        double [] yData = getData(yCol);
        for (int i = 0; i < getNumLocalizations(); i ++) {
            xData[i] += dx;
            yData[i] += dy;
        }	    
	}
}
