package edu.uchc.octane.core.image;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.uchc.octane.core.datasource.Localizations;
import edu.uchc.octane.core.datasource.octaneDataFile;

public class LocalizationImage extends Localizations {

	private short [] pixels = null;
	private boolean isDirty = true, isDone = false;
	private Runnable renderingCallback = null;

	private int [] cachedDataIdx = null;
	private int [] cachedPixelIdx = null;

	protected double pixelSize;
	protected int dimx, dimy;
	protected Rectangle roi;
	protected HashMap<Integer, double []> filters;


	private RenderingThread renderingThread = null;

	class RenderingThread extends Thread {

		@Override
		public void run() {
			isDirty = false;
			isDone = false;
			setPriority(Thread.MAX_PRIORITY);

			doRendering();
		}

		public void doRendering() {

			if (isDirty || interrupted()) {
				return;
			}
			Arrays.fill(pixels, (short)0);
			for (int i = 0 ; i < cachedDataIdx.length; i ++) {

				if (isDirty || interrupted()) {
					return;
				}

				int idx = cachedDataIdx[i];
				int idx2 = cachedPixelIdx[i];
				boolean filteredOut = false;
				synchronized(filters) {
					for (Map.Entry<Integer, double[]> entry : filters.entrySet()) {
						int col = entry.getKey();
						double [] r = entry.getValue();
						if (data[col][idx] < r[0] || data[col][idx] > r[1]) {
							filteredOut = true;
							break;
						}
					}
				}
				if (! filteredOut) {
					pixels[idx2] ++;
				}
			}

			isDone = true;

			if (renderingCallback != null) {
				renderingCallback.run();
			}
		}
	}

	public LocalizationImage(octaneDataFile locData) {
		super(locData);
		pixelSize = 16.0;
		dimx = 5120;
		dimy = 5120;

		//stats = new DoubleSummaryStatistics[data.length];
//		for (int i= 0; i < data.length; i++) {
//			stats[i]=  Arrays.stream(data[i]).summaryStatistics();
//		}

		setRoi(null);
		//renderingThread = new RenderingThread();
		//renderingThread.start();

		filters = new HashMap<Integer, double[]>();

	}

	public LocalizationImage(ObjectInputStream s) throws ClassNotFoundException, IOException {
		this((octaneDataFile) s.readObject());
	}

	public HashMap<Integer, double[] > getViewFilters() {
		return filters;
	}

	public void addViewFilter(int col, double[] v) {

		isDirty = true;
		renderingThread = null;
		synchronized(filters) {
			filters.put(col, v);
		}
		// restartRendering();
	}

	public void setDim(int x, int y) {
		dimx = x;
		dimy = y;
	}

	public int getDimX() {
		if (roi != null) {
			return roi.width;
		} else {
			return dimx;
		}
	}

	public int getDimY() {
		if (roi != null) {
			return roi.height;
		} else {
			return dimy;
		}
	}

//	public void setPixelSize(double s) {
//		pixelSize = s;
//	}

	public double getPixelSize() {
		return pixelSize;
	}

	public void setRoi(Rectangle rect) {

		if (rect == null) {
			rect = new Rectangle(dimx, dimy);
		}

		if (rect.equals(roi)) {
			return;
		}

		isDirty = true;
		roi = rect;

		int [] tmpIdx1 = new int[getNumLocalizations()];
		int [] tmpIdx2 = new int[getNumLocalizations()];
		renderingThread = null;

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

		//restartRendering();
	}

	public Rectangle getRoi() {
		return roi;
	}

	public boolean isRenderingDone() {
		return isDone;
	}

	public void render(short [] pixels) {

		Rectangle rect = roi;
		if (rect == null) {
			rect = new Rectangle(0,0,getDimX(), getDimY());
		}
		assert(pixels.length == rect.width * rect.height);
		for (int i = 0 ; i < getNumLocalizations(); i ++) {

			int x = (int)(getXAt(i) / pixelSize);
			int y = (int)(getYAt(i) / pixelSize);
			if (rect.contains(x, y)) {
				int idx = (y - rect.y) * rect.width + x - rect.x;
				pixels[idx] ++;
			}
		}
	}

	public short [] getRendered() {
		if (renderingThread == null) {
			restartRendering();
		}

		while (! isDone ) {
			try {
				renderingThread.join();
			} catch (InterruptedException e) {
			}
		}

		return pixels;
	}

	public void restartRendering() {

		if (renderingThread != null && renderingThread.isAlive()) {
			renderingThread.interrupt();
			try {
				renderingThread.join();
			} catch (InterruptedException e) {
				;
			}
		}
		renderingThread = new RenderingThread();
		renderingThread.start();
	}

	public void onRenderingDone(Runnable callback) {
		renderingCallback = callback;
	}
}
