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
    SummaryStatistics[] stats = null;
    HashMap<String, Integer> headersMap = null;
    FastKDTree tree;
    double[] localDensities;
    final double[][] data;
    public int xCol = 1, yCol = 2, zCol = -1, frameCol = 0, errCol = -1, intensityCol = -1, sigmaCol = -1;

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
        data = raw.data;

        stats = new SummaryStatistics[data.length];

        if (headersMap == null) {
            headersMap = new HashMap<String, Integer>();
            for (int i = 0; i < dataSrc.headers.length; i++) {
                headersMap.put(dataSrc.headers[i], i);
            }
            headersMap.put("LocalDensity", data.length);
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
        for (String key : headersMap.keySet()) {
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

    public SummaryStatistics getSummaryStatistics(int col) {

        if (col < 0 || col >= data.length) {
            return null;
        }
        if (stats[col] == null) {
            stats[col] = new SummaryStatistics();
            for (double d : data[col]) {
                stats[col].addValue(d);
            }
        }
        return stats[col];
    }

    public SummaryStatistics getSummaryStatistics(String header) {
        Integer col = getColFromHeader(header);
        if (col != null) {
            return getSummaryStatistics(col);
        } else {
            return null;
        }
    }

    public double[] getData(int col) {
        return data[col];
    }

    public double[] getData(String colHeader) {
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
        if (tree == null) {
            constructKDtree();
        }
        // int localDensityCol = data.length - 1;
        for (int i = 0; i < getNumLocalizations(); i++) {
            localDensities[i] = tree.radiusSearch(i, distance).size();
        }
    }

    public String getHeader(int i) {
        return dataSrc.headers[i];
    }

    public Integer getColFromHeader(String s) {
        return headersMap.get(s);
    }

    public int getNumOfCol() {
        return data.length;
    }

    public OctaneDataFile getDataSource() {
        return dataSrc;
    }

    public void mergeWith(OctaneDataFile odf) {
        if (odf == null) {
            return;
        }
        if (data.length != odf.data.length) {
            throw new IllegalArgumentException("Data dimension doesn't match");
        }
        
        double [][] oldData = data.clone();
        for (int i = 0; i < data.length; i ++) {
            int newlen = oldData[i].length + odf.data[i].length ;
            data[i] = new double[newlen];
            System.arraycopy(oldData[i], 0, data[i], 0, oldData[i].length);
            System.arraycopy(odf.data[i], 0, data[i], oldData[i].length, odf.data[i].length);
        }
    }
    
    // in default axis convention:
    // rotation around top left corner (0,0)
    // positive theta values rotate counter-clock-wise
    public void rotate(double theta, double x0, double y0) {
        double[] xData = getData(xCol);
        double[] yData = getData(yCol);
        double cosTheta = FastMath.cos(theta);
        double sinTheta = FastMath.sin(theta);
        for (int i = 0; i < getNumLocalizations(); i++) {
            double x = cosTheta * (xData[i] - x0) - sinTheta * (yData[i] - y0);
            double y = sinTheta * (xData[i] - x0) + cosTheta * (yData[i] - y0);
            xData[i] = x + x0;
            yData[i] = y + y0;
        }
    }

    public void translate(double dx, double dy) {
        double[] xData = getData(xCol);
        double[] yData = getData(yCol);
        for (int i = 0; i < getNumLocalizations(); i++) {
            xData[i] += dx;
            yData[i] += dy;
        }
    }
}
