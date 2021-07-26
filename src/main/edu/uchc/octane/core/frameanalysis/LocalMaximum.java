//FILE:          ParticleAnalysis.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
//
// LICENSE:      This file is distributed under the BSD license.
//	               License text is included with the source distribution.
//
//	               This file is distributed in the hope that it will be useful,
//	               but WITHOUT ANY WARRANTY; without even the implied warranty
//	               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//	               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//	               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//	               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES./**
//

package edu.uchc.octane.core.frameanalysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import edu.uchc.octane.core.pixelimage.RectangularImage;
import edu.uchc.octane.core.utils.ImageFilters;

// looking for local maxima that are spatially separated from other maxmima by a valley.
// The valley is defined as pixels whose intensities are lower than the peak by more than the (tolerance).

public class LocalMaximum{

	final static Logger logger = LoggerFactory.getLogger(LocalMaximum.class);

	public int ROISize;
	public double threshold, tolerance;

	public interface CallBackFunctions {

		public boolean fit(RectangularImage ROI, int x, int y);

	}

	private final static int PROCESSED = 1;
	private final static int COLLECTED = 2;

	RectangularImage /*data,*/ filteredData;
	char [] pixelStates;

	class Pixel implements Comparable<Pixel> {
		public int idx;
		public Pixel(int idx) {
			this.idx = idx;
		}
		@Override
		public int compareTo(Pixel o) {
			return (int) FastMath.signum(- filteredData.getValue(idx) + filteredData.getValue(o.idx));
		}
	}

	public LocalMaximum(double noise) {
		this(noise, 0, 4);
	}

	public LocalMaximum(double noise, int threshold, int ROISize) {

		this.threshold = threshold;
		this.tolerance = noise;
		this.ROISize = ROISize; 
	}

	boolean isProcessed(int index) {
		return ( ( pixelStates[index] & PROCESSED ) != 0);
	}

	boolean isCollected(int index) {
		return ( ( pixelStates[index] & COLLECTED) != 0);
	}

//	public boolean isMasked(int index) {
//		return ( ( pixelStates[index] & MASKED) != 0);
//	}

	void process(int index) {
		pixelStates[index] |= PROCESSED;
	}

	void collect(int index) {
		pixelStates[index] |= COLLECTED;
	}

//	public void mask(int index) {
//		pixelStates[index] |= MASKED;
//	}

	public int processFrame(RectangularImage data, CallBackFunctions callback) {

		double[] filtered = ImageFilters.symmetricFilter(
				ImageFilters.makeGaussianFilter(1.0,7),
				data.getValueVector(),
				data.width );
		filteredData = new RectangularDoubleImage(filtered, data.width);
		pixelStates = new char[data.getLength()];

		ArrayList<Pixel> pixels = new ArrayList<Pixel>();

		for (int i = 0; i < filteredData.getLength(); i ++) {

			if (filteredData.getValue(i) > threshold) {
				pixels.add(new Pixel(i));
			} else {
//				mask(i);
			}
		}
		Collections.sort(pixels);

		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		ArrayList<Integer> pool = new ArrayList<Integer>();
		int nMax = 0;

		for (Pixel pixel : pixels) {

			if ( isProcessed(pixel.idx) ){ continue; }

			double peakValue = filteredData.getValue(pixel.idx);
			boolean isMax = true;
			queue.clear();
			pool.clear();
			queue.offer(pixel.idx);
			collect(pixel.idx);

			while (! queue.isEmpty()) {
				int index = queue.poll();
				pool.add(index);
				int [] neighbours = filteredData.getNeighboursIndices(index);

				for (int p : neighbours) { // analyze all neighbors (in 8 directions) at the same level
					if ( isProcessed(p) ) { //conflict
						isMax = false;
						for (Integer idx : queue) {
							process(idx);
						}
						break;
					}
					if ( ! isCollected(p) && /*! isMasked(neighbours[i]) && */ filteredData.getValue(p) > peakValue - tolerance) {
						queue.add(p);
						collect(p);
					}
				}
			}

			for ( Integer idx : pool) {
				process(idx);
			}

			if (isMax) {
				nMax ++;
				int x = data.getXCordinate(pixel.idx);
				int y = data.getYCordinate(pixel.idx);
				RectangularImage subImage = data.getSubImage(x - ROISize, y - ROISize, ROISize * 2 + 1, ROISize * 2 + 1, true);
				
				//exclude fitted region from future peak detection
				for (int i = 0; i < subImage.getLength(); i++) {
				    int idx = filteredData.getIndexOfCoordinate(subImage.getXCordinate(i), subImage.getYCordinate(i));
				    process(idx);
				}
				
				if (callback.fit(subImage, x, y) == false) {
					break;
				}
			}
		}

		logger.info("Detected peaks: " + nMax);
		return nMax;
	}
}
