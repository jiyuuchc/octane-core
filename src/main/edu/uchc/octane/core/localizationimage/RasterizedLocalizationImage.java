package edu.uchc.octane.core.localizationimage;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.data.LocalizationData;

public class RasterizedLocalizationImage extends LocalizationImage implements Runnable {

    final static double DEFAULT_PIXEL_SIZE = 16.0;

    short[] pixels = null;
    Runnable renderingCallback = null;
    int[] cachedDataIdx = null;
    int[] cachedPixelIdx = null;
    double pixelSize;
    Rectangle roi;
    HashMap<Integer, double[]> filters;
    Thread renderingThread = null;
    boolean isDone;
    boolean isDirty;

    public RasterizedLocalizationImage(LocalizationData locData) {
        this(locData, DEFAULT_PIXEL_SIZE);
    }

    public RasterizedLocalizationImage(LocalizationData locData, double pixelSize) {
        this(locData, pixelSize, null);
    }

    public RasterizedLocalizationImage(LocalizationData locData, double pixelSize, Rectangle roi) {
         
         super(locData);
         
         this.pixelSize = pixelSize;
         filters = new HashMap<Integer, double[]>();

         setRoi(roi);
     }

    public RasterizedLocalizationImage(ObjectInputStream s) throws ClassNotFoundException, IOException {
        this((LocalizationData) s.readObject());
    }

    public RasterizedLocalizationImage(RasterizedLocalizationImage orig) {

        super(orig);

        pixelSize = orig.pixelSize;

        roi = orig.roi;
        setRoi(roi);

        filters = new HashMap<Integer, double[]>();
        for (Integer key : orig.filters.keySet()) {
            filters.put(key, orig.filters.get(key).clone());
        }
    }

    Rectangle getDefaultRoi() {
        int maxX = (int) FastMath.floor(getSummaryStatistics(xCol).getMax() / pixelSize);
        int minX = (int) FastMath.floor(getSummaryStatistics(xCol).getMin() / pixelSize);
        int maxY = (int) FastMath.floor(getSummaryStatistics(yCol).getMax() / pixelSize);
        int minY = (int) FastMath.floor(getSummaryStatistics(yCol).getMin() / pixelSize);
        if (minX < 100 && minY < 100) {
            minX = 0;
            minY = 0;
        } 
//        int dimx = findPreferredRasterSize(maxX - minX);
//        int dimy = findPreferredRasterSize(maxY - minY);
        
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    int findPreferredRasterSize(int d) {
        if (d > 5120) {
            return 10 * ((int) FastMath.floor(d / 10.0) + 1);
        }
        // prefer 2^n * 10;
        int pd = 32;
        while (pd * 10 < d) {
            pd *= 2;
        }
        return pd * 10;
    }

    public int getDimX() {
        return roi.width;
    }

    public int getDimY() {
        return roi.height;
    }

    public double getPixelSize() {
        return pixelSize;
    }


    public void setDirty() {
    	isDirty = true;
    	quitCurrentRendering();
    }

    public HashMap<Integer, double[]> getViewFilters() {
        return filters;
    }

    public void setViewFilters(HashMap<Integer, double[]> newFilters) {
        filters = newFilters;
    }

    public synchronized void setViewFilter(int col, double[] v) {

        if (v != null ) {
            setDirty(); // important to stop rendering thread before change filters
            filters.put(col, v);
        } else if (filters.get(col) != null){
        	setDirty(); // important to stop rendering thread before change filters
            filters.remove(col);
        }
    }
    
    public double [] getViewFilter(int col) {
        return filters.get(Integer.valueOf(col));
    }

    public synchronized void setRoi(Rectangle rect) {

        if (rect == null) {
            rect = getDefaultRoi();
        }

        if (rect.equals(roi)) {
            return;
        }

        setDirty();
        roi = rect;

        cachedDataIdx = null;
        cachedPixelIdx = null;
        pixels = null;
    }

    public Rectangle getRoi() {
        return roi;
    }

    public short[] getRendered() {
        if (isDirty) {
            startRendering();
        }

        if (renderingThread != null) {
        	try {
        		renderingThread.join();
        	} catch (InterruptedException e) {
        		return null;
        	}
        }

        return pixels;
    }

    public synchronized void startRendering() {
    	quitCurrentRendering();
        renderingThread = new Thread(this);
        renderingThread.start();
    }

    public void onRenderingDone(Runnable callback) {
        renderingCallback = callback;
    }

    public void interruptRenderingThread() {
        if (renderingThread != null) {
            renderingThread.interrupt();
        }
    }

    public void quitCurrentRendering() {
        if (renderingThread != null) {
            renderingThread.interrupt();
            try {
                renderingThread.join();
            } catch (InterruptedException e) {
            }
        }
        renderingThread = null;
    }

    public void rotate(double theta) {
        rotate(theta, pixelSize * (roi.x + roi.width / 2.0), pixelSize * (roi.y + roi.height / 2.0));
    }

    @Override
    public synchronized void rotate(double theta, double x0, double y0) {
        setDirty();
        super.rotate(theta, x0, y0);

        cachedDataIdx = null;
        cachedPixelIdx = null;
    }

    @Override
    public synchronized void translate(double dx, double dy) {
        setDirty();
        super.translate(dx, dy);

        cachedDataIdx = null;
        cachedPixelIdx = null;
    }

    @Override
    public synchronized void mergeWith(LocalizationData odf) {
        setDirty();
        super.mergeWith(odf);

        cachedDataIdx = null;
        cachedPixelIdx = null;
    }

    void cachePositions() {
        int[] tmpIdx1 = new int[getNumLocalizations()];
        int[] tmpIdx2 = new int[getNumLocalizations()];

        int cnt = 0;
        for (int i = 0; i < getNumLocalizations(); i++) {

            int x = (int) (getXAt(i) / pixelSize);
            int y = (int) (getYAt(i) / pixelSize);
            if (roi.contains(x, y)) {
                tmpIdx1[cnt] = i;
                tmpIdx2[cnt] = x - roi.x + (y - roi.y) * roi.width;
                cnt++;
            }
        }

        cachedDataIdx = new int[cnt];
        cachedPixelIdx = new int[cnt];
        System.arraycopy(tmpIdx1, 0, cachedDataIdx, 0, cnt);
        System.arraycopy(tmpIdx2, 0, cachedPixelIdx, 0, cnt);
    }

    public synchronized double[][] getFilteredData() {
        if (cachedDataIdx == null) {
            cachePositions();
        }

        ArrayList<Integer> filteredList = new ArrayList<Integer>();
        for (int i = 0; i < cachedDataIdx.length; i++) {
            boolean filteredOut = false;

            for (Map.Entry<Integer, double[]> entry : filters.entrySet()) {
                int col = entry.getKey();
                double[] r = entry.getValue();
                int c = cachedDataIdx[i];
                double[] vc = getData(col);
                double v = vc[c];
                if (v < r[0] || v > r[1]) {
                    filteredOut = true;
                    break;
                }
            }

            if (!filteredOut) {
                filteredList.add(cachedDataIdx[i]);
            }
        }

        double[][] newData = new double[odf.data.length][filteredList.size()];
        for (int i = 0; i < filteredList.size(); i++) {
            for (int j = 0; j < odf.data.length; j++) {
                newData[j][i] = odf.data[j][filteredList.get(i)];
            }
        }
        return newData;
    }

    @Override
    public void run() {

        isDone = false;

        if (cachedDataIdx == null) {
            cachePositions();
        }
        if (pixels == null) {
            pixels = new short[roi.width * roi.height];
        }
        Arrays.fill(pixels, (short) 0);

        for (int i = 0; i < cachedDataIdx.length; i++) {

            if (Thread.interrupted()) {
                return;
            }

            boolean filteredOut = false;

            for (Map.Entry<Integer, double[]> entry : filters.entrySet()) {
                int col = entry.getKey();
                double[] r = entry.getValue();
                int c = cachedDataIdx[i];
                double[] vc = getData(col);
                double v = vc[c];
                if (v < r[0] || v > r[1]) {
                    filteredOut = true;
                    break;
                }
            }
            if (!filteredOut) {
                pixels[cachedPixelIdx[i]]++;
            }

        }
        isDone = true;

        if (renderingCallback != null) {
            renderingCallback.run();
        }
    }
}
