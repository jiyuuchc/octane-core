package edu.uchc.octane.core.localizationimage;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.utils.FastKDTree;
import edu.uchc.octane.core.utils.HDataCollection;

public class LocalizationImage {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final OctaneDataFile odf;

    ArrayList<SummaryStatistics> stats;
    ArrayList<String> headerList;
    ArrayList<double []> dataList;
    
    public int xCol = 1, yCol = 2, zCol = -1, frameCol = 0, errCol = -1, intensityCol = -1, sigmaCol = -1;

    FastKDTree tree;
    
    //represent data as HDataCollection
    class HDataImp implements HDataCollection {

        private double[][] data_;
        private int d = 0;

        public HDataImp() {
            data_ = new double[getDimension()][];
            data_[0] = getData(xCol);
            data_[1] = getData(yCol);
            if (! is2DData() ) {
            	data_[2] = getData(zCol);
            }
        }

        @Override
        public int getDimension() {
            return is2DData() ? 2 : 3;
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
        	if (d >= 0 && d < data_.length) {
        		this.d = d;
        	} else {
        		throw new IllegalArgumentException("Dimension out of bound"); 
        	}
        }

        @Override
        public double get(int idx) {
            return get(idx, d);
        }
    }

    public HDataCollection getHDataView() {
    	return new HDataImp();
    }

    
    public LocalizationImage(OctaneDataFile odf) {
        this.odf = odf;

        dataList = new ArrayList<>(Arrays.asList(odf.data));
        headerList = new ArrayList<>(Arrays.asList(odf.headers));

        stats = new ArrayList<SummaryStatistics>();
        for (int i = 0; i < this.getNumOfCol(); i++) {
        	stats.add(null);
        }

        guessHeaders();
    }

    @SuppressWarnings("unchecked")
    public LocalizationImage(LocalizationImage loc) {
    	odf = loc.odf;

    	dataList = (ArrayList<double[]>)loc.dataList.clone();
    	stats = (ArrayList<SummaryStatistics>) loc.stats.clone();
    	headerList = (ArrayList<String>)loc.headerList.clone();

    	tree = loc.tree;
    	
    	guessHeaders();
    }

    void guessHeaders() {
        for (int col = 0; col < headerList.size(); col ++) {
            String key = headerList.get(col).toLowerCase();
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

        if (col < 0 || col >= getNumOfCol()) {
            return null;
        }
        if (stats.get(col) == null) {
            stats.set(col, new SummaryStatistics());
            for (double d : getData(col)) {
                stats.get(col).addValue(d);
            }
        }
        return stats.get(col);
    }

    public SummaryStatistics getSummaryStatistics(String header) {
    	return getSummaryStatistics(getColFromHeader(header));
    }

    public double[] getData(int col) {
    	if (col < 0 || col >= getNumOfCol()) {
    		return null;
    	} else {
    		return dataList.get(col);
    	}
    }

    public double[] getData(String colHeader) {
        return getData(getColFromHeader(colHeader));
    }

    public int getNumLocalizations() {
        return getData(frameCol).length;
    }

    public double getXAt(int idx) {
        return getData(xCol)[idx];
    }

    public double getYAt(int idx) {
        return getData(yCol)[idx];
    }

    public double getZAt(int idx) {
        return getData(zCol)[idx];
    }

    public boolean is2DData() {
    	return zCol == -1;
    }

    public void constructKDtree() {
    	logger.info("Construct a KDTree - start");
        tree = new FastKDTree(getHDataView());
        logger.info("Construct a KDTree - finished");
    }

    public void measureLocalDensity(double distance) {
        if (tree == null) {
            constructKDtree();
        }
        
        double [] localDensities = getData("density");

        if ( localDensities == null) {
        	localDensities = new double[getNumLocalizations()];
            addAuxData("density", localDensities);
        }

    	logger.info("Compute local density - start");
        for (int i = 0; i < getNumLocalizations(); i++) {

            localDensities[i] = tree.radiusSearch(i, distance).size();
            if (i % 50000 == 0) {
            	logger.info("Processed " + i + "/" + getNumLocalizations() +  " points");
            }
        }
        logger.info("Compute local density - finished");
        
    }

    public String getHeader(int i) {
        return headerList.get(i);
    }

    public int getColFromHeader(String s) {
    	return headerList.indexOf(s);
    }

    public ArrayList<String> getHeaders() {
        return headerList;
    }

    public int getNumOfCol() {
        return dataList.size();
    }

    public OctaneDataFile getDataSource() {
        return odf;
    }
    
    public void mergeWith(OctaneDataFile newOdf) {
        if (newOdf == null) {
            return;
        }
        
        odf.mergeWith(newOdf);
        
        //reset and remove all auxiliary data
        dataList = new ArrayList<>(Arrays.asList(odf.data));
        headerList = new ArrayList<>(Arrays.asList(odf.headers));

        stats = new ArrayList<SummaryStatistics>(getNumOfCol());

        guessHeaders();
    }
    
    public void addAuxData(String header, double[] newColumn) {
    	if (newColumn.length != this.getNumLocalizations()) {
    		throw new IllegalArgumentException("New data column length does match existing ones");
    	}

    	headerList.add(header);
    	dataList.add(newColumn);
    	stats.add(null);
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
