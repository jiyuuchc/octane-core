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

package edu.uchc.octanecore.frameanalysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octanecore.datasource.RectangularImage;

public class WatershedProcessor{

	public int ROISize;
	public double threshold, noise;
	
	public interface CallBackFunctions {	

		public boolean fit(RectangularImage ROI, int x, int y); 
	
	}
	
	protected final static int PROCESSED = 1;
	protected final static int FLOODED = 2;
	protected final static int MASKED = 4;

	protected RectangularImage data;
	protected char [] pixelStates;
	
	class Pixel implements Comparable<Pixel> {
		
		public int idx;

		public Pixel(int idx) {
			this.idx = idx;
		}
		
		@Override
		public int compareTo(Pixel o) {
			
			return (int) FastMath.signum(- data.getValue(idx) + data.getValue(o.idx));
		
		}
	}

	public WatershedProcessor(double noise) {
		this(noise, 0, 11);
	}

	public WatershedProcessor(double noise, int threshold, int ROISize) {

		this.threshold = threshold;
		this.noise = noise;
		this.ROISize = ROISize;
	}

	public boolean isProcessed(int index) {
		return ( ( pixelStates[index] & PROCESSED ) != 0);
	}

	public boolean isFlooded(int index) {
		return ( ( pixelStates[index] & FLOODED) != 0);
	}

	public boolean isMasked(int index) {
		return ( ( pixelStates[index] & MASKED) != 0);
	}

	public void process(int index) {
		pixelStates[index] |= PROCESSED;
	}

	public void flood(int index) {
		pixelStates[index] |= FLOODED;
	}
	
	public void mask(int index) {
		pixelStates[index] |= MASKED;
	}
	
	public void processFrame(RectangularImage data, CallBackFunctions callback) {
		
		this.data = data;
		pixelStates = new char[data.getLength()];

		ArrayList<Pixel> pixels = new ArrayList<Pixel>();

		for (int i = 0; i < data.getLength(); i ++) {
			
			if (data.getValue(i) > threshold) {
				pixels.add(new Pixel(i));
			} else {
				mask(i);
			}
		}
		Collections.sort(pixels);
		 
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		ArrayList<Integer> pool = new ArrayList<Integer>();
 
		for (Pixel pixel : pixels) {
			
			if ( isProcessed(pixel.idx) ){
				continue;
			}

			double peakValue = data.getValue(pixel.idx);
			boolean isMax = true;
			
			queue.clear();
			pool.clear();

			queue.offer(pixel.idx);
			
			flood(pixel.idx);
			
			while (! queue.isEmpty()) {

				int index = queue.poll();
				pool.add(index);
				int [] neighbours = data.getNeighboursIndices(index);

				for (int i = 0; i < neighbours.length; i++) { // analyze all neighbors (in 8 directions) at the same level

					if ( isProcessed(neighbours[i]) ) { //conflict
						isMax = false; 
						break;
					}

					if ( ! isFlooded(neighbours[i]) && ! isMasked(neighbours[i]) && data.getValue(neighbours[i]) > peakValue - noise) {
						queue.add(neighbours[i]);
						flood(neighbours[i]);
					}
				} 
			}

			for ( Integer idx : pool) {
				process(idx);
			} 

			if (isMax) {

				int x = data.getXCordinate(pixel.idx);
				int y = data.getYCordinate(pixel.idx);
				RectangularImage subImage = new RectangularImage(data, x - ROISize / 2, y - ROISize / 2, ROISize, ROISize, true);
				if (callback.fit(subImage, x, y) == false) {
					break;
				}				
			}
		}
	}
	
}
