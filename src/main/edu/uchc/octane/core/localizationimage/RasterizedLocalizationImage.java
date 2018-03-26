package edu.uchc.octane.core.localizationimage;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.OctaneDataFile;

public class RasterizedLocalizationImage extends LocalizationImage {

	private short [] pixels = null;
	private Runnable renderingCallback = null;

	private int [] cachedDataIdx = null;
	private int [] cachedPixelIdx = null;

	double pixelSize;
	int dimx, dimy;
	Rectangle roi;
	HashMap<Integer, double []> filters;

	RenderingThread renderingThread = null;

	class RenderingThread extends Thread {
	    boolean isDone;

		@Override
		public void run() {
		    isDone = false;

			// setPriority(Thread.MAX_PRIORITY);

			Arrays.fill(pixels, (short)0);
			
			for (int i = 0 ; i < cachedDataIdx.length; i ++) {

			    if (interrupted()) {
					return;
				}

				boolean filteredOut = false;

				synchronized(filters) {
				for (Map.Entry<Integer, double[]> entry : filters.entrySet()) {
				    int col = entry.getKey();
				    double [] r = entry.getValue();
				    int c = cachedDataIdx[i];
				    double [] vc = data[col];
//				    if (vc==null) {
//				        System.out.println(col);
//				    }
				    double v = vc[c];
				    if ( v < r[0] || v > r[1]) {
				        filteredOut = true;
				        break;
				    }
				}
				if (! filteredOut) {
				    pixels[cachedPixelIdx[i]] ++;
				}					
			
				}
			}

			isDone = true;

			if (renderingCallback != null) {
				renderingCallback.run();
			}
		}
	}

	public RasterizedLocalizationImage(OctaneDataFile locData) {
		this(locData, 16.0);
	}

	public RasterizedLocalizationImage(OctaneDataFile locData, double pixelSize) {
	    super(locData);
	    this.pixelSize = pixelSize;
	    int maxX = (int) FastMath.floor(getSummaryStatitics(xCol).getMax() / pixelSize);
	    int maxY = (int) FastMath.floor(getSummaryStatitics(yCol).getMax() / pixelSize);
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
        //prefer 2^n * 10;
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

	public void addViewFilter(int col, double[] v) {
	    
	    quitCurrentRendering();
	    synchronized (filters) {
	    filters.put(col, v);
	    }
	}

	public void setRoi(Rectangle rect) {

		if (rect == null) {
			rect = new Rectangle(dimx, dimy);
		}

		if (rect.equals(roi)) {
			return;
		}

		roi = rect;
		quitCurrentRendering();
		
		int [] tmpIdx1 = new int[getNumLocalizations()];
		int [] tmpIdx2 = new int[getNumLocalizations()];

		pixels = new short[rect.width * rect.height];

		int cnt = 0;
		for (int i = 0 ; i < getNumLocalizations(); i ++) {

			int x = (int)(getXAt(i) / pixelSize);
			int y = (int)(getYAt(i) / pixelSize);
			if (rect.contains(x, y)) {
				tmpIdx1[cnt] = i;
				tmpIdx2[cnt] = x - rect.x + (y - rect.y) * rect.width;
				cnt++;
			}
		}

		cachedDataIdx = new int[cnt];
		cachedPixelIdx = new int[cnt];
		System.arraycopy(tmpIdx1, 0, cachedDataIdx, 0, cnt);
		System.arraycopy(tmpIdx2, 0, cachedPixelIdx, 0, cnt);
	}

	public Rectangle getRoi() {
		return roi;
	}

	public short [] getRendered() {
		if ( renderingThread == null ) {
			restartRendering();
		}

		while (! renderingThread.isDone) {
			try {
				renderingThread.join();
			} catch (InterruptedException e) {
			}
		}

		return pixels;
	}

	public void restartRendering() {
		renderingThread = new RenderingThread();
		renderingThread.start();
	}

	public void onRenderingDone(Runnable callback) {
		renderingCallback = callback;
	}
	
	public void interruptRenderingThread() {
	    if (renderingThread != null ) {
	        renderingThread.interrupt();
	    }
	}
	
	public void quitCurrentRendering() {
	    if (renderingThread != null ) {
            renderingThread.interrupt();	
            try {
                renderingThread.join();
            } catch (InterruptedException e) {
            }	    
	    }
	    renderingThread = null;
	}
}
