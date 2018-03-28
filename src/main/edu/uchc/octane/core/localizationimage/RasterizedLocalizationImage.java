package edu.uchc.octane.core.localizationimage;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.OctaneDataFile;

public class RasterizedLocalizationImage extends LocalizationImage implements Runnable {

    short[] pixels = null;
    Runnable renderingCallback = null;
    int[] cachedDataIdx = null;
    int[] cachedPixelIdx = null;
    double pixelSize;
    int dimx, dimy;
    Rectangle roi;
    HashMap<Integer, double[]> filters;
    Thread renderingThread = null;
    boolean isDone;

    public RasterizedLocalizationImage(OctaneDataFile locData) {
        this(locData, 16.0);
    }

    public RasterizedLocalizationImage(OctaneDataFile locData, double pixelSize) {
        super(locData);
        this.pixelSize = pixelSize;
        int maxX = (int) FastMath.floor(getSummaryStatistics(xCol).getMax() / pixelSize);
        int maxY = (int) FastMath.floor(getSummaryStatistics(yCol).getMax() / pixelSize);
        dimx = findPreferredRasterSize(maxX);
        dimy = findPreferredRasterSize(maxY);
        setRoi(null);
        filters = new HashMap<Integer, double[]>();
    }

    public RasterizedLocalizationImage(OctaneDataFile locData, double pixelSize, int dimX, int dimY) {
        super(locData);
        this.pixelSize = pixelSize;
        this.dimx = dimX;
        this.dimy = dimY;
        setRoi(null);
        filters = new HashMap<Integer, double[]>();
    }

    public RasterizedLocalizationImage(ObjectInputStream s) throws ClassNotFoundException, IOException {
        this((OctaneDataFile) s.readObject());
    }

    public RasterizedLocalizationImage(RasterizedLocalizationImage orig) {
        super(orig);
        pixelSize = orig.pixelSize;
        dimx = orig.dimx;
        dimy = orig.dimy;
        setRoi(orig.roi);
        filters = new HashMap<Integer, double[]>();
        for (Integer key : orig.filters.keySet()) {
            filters.put(key, orig.filters.get(key).clone());
        }
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
        return dimx;
    }

    public int getDimY() {
        return dimy;
    }

    public double getPixelSize() {
        return pixelSize;
    }

    public HashMap<Integer, double[]> getViewFilters() {
        return filters;
    }

    public synchronized void addViewFilter(int col, double[] v) {

        quitCurrentRendering();
        filters.put(col, v);
    }

    public synchronized void setRoi(Rectangle rect) {

        if (rect == null) {
            rect = new Rectangle(dimx, dimy);
        }

        if (rect.equals(roi)) {
            return;
        }

        roi = rect;
        quitCurrentRendering();

        cachedDataIdx = null;
        cachedPixelIdx = null;
        pixels = null;
    }

    public Rectangle getRoi() {
        return roi;
    }

    public short[] getRendered() {
        if (renderingThread == null) {
            startRendering();
        }

        try {
            renderingThread.join();
        } catch (InterruptedException e) {
            return null;
        }

        return pixels;
    }

    public synchronized void startRendering() {
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
        rotate(theta, pixelSize * dimx / 2, pixelSize * dimy / 2);
    }

    @Override
    public synchronized void rotate(double theta, double x0, double y0) {
        quitCurrentRendering();
        super.rotate(theta, x0, y0);

        cachedDataIdx = null;
        cachedPixelIdx = null;
    }

    @Override
    public synchronized void translate(double dx, double dy) {
        quitCurrentRendering();
        super.translate(dx, dy);

        cachedDataIdx = null;
        cachedPixelIdx = null;
    }

    @Override
    public synchronized void mergeWith(OctaneDataFile odf) {
        quitCurrentRendering();
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

    public synchronized double [][] getFilteredData() {
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
                double[] vc = data[col];
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
        
        double[][] newData = new double[data.length][filteredList.size()];
        for (int i = 0; i < filteredList.size(); i++) {
            for (int j = 0; j < data.length; j++) {
                newData[j][i] = data[j][filteredList.get(i)];
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
                double[] vc = data[col];
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
