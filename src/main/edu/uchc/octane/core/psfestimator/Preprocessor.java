package edu.uchc.octane.core.psfestimator;

import java.util.ArrayList;
import java.util.List;

import edu.uchc.octane.core.localizationimage.LocalizationImage;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;

public class Preprocessor {

	LocalizationImage locData;
	double minDistance2;
	//double maxIntensity;
	double maxSigma;

	int maxFrame;
	int minFrame;
	List<Integer> [] points;
	ArrayList<RectangularDoubleImage> subImages;
	// ArrayList<Double> bgs;
	ArrayList<Double> xcenters;
	ArrayList<Double> ycenters;
	ArrayList<Double> intensities;

	public Preprocessor() {
		xcenters = new ArrayList<Double>();
		ycenters = new ArrayList<Double>();
		intensities = new ArrayList<Double>();
		subImages = new ArrayList<RectangularDoubleImage>();
	}
	
	public int getNumSubImages() {
		return subImages.size();
	}
	
	public int getNumPoints() {
		int n = 0;
		for (int i = 0; i < points.length; i++) {
			n += points[i].size();
		}
		return n;
	}

	public RectangularDoubleImage [] getSubImages() {
		RectangularDoubleImage [] images = new RectangularDoubleImage[subImages.size()];
		subImages.toArray(images);
		return images;
	}
	
	public double [] getXCenters() {
		double [] d = new double[xcenters.size()];
		for (int i = 0; i < d.length; i++) {
			d[i] = xcenters.get(i);
		}
		return d;
	}

	public double [] getYCenters() {
		double [] d = new double[ycenters.size()];
		for (int i = 0; i < d.length; i++) {
			d[i] = ycenters.get(i);
		}
		return d;
	}
	
	public double [] getIntensities() {
		double [] d = new double[intensities.size()];
		for (int i = 0; i < d.length; i++) {
			d[i] = intensities.get(i);
		}
		return d;		
	}

	public void processImages(RectangularDoubleImage img, int frame, int windowSize, double pixelSize) {
		if (frame < minFrame || frame > maxFrame) {
			return;
		}

		for (int p : points[frame - minFrame]) {
			double xc = locData.getXAt(p) / pixelSize;
			double yc = locData.getYAt(p) / pixelSize;
			try {
				subImages.add(new RectangularDoubleImage(
					img, 
					(int)xc - windowSize, 
					(int)yc - windowSize, 
					windowSize * 2 + 1,
					windowSize * 2 + 1));
				xcenters.add(xc);
				ycenters.add(yc);
				intensities.add(locData.getData(locData.intensityCol)[p]);				
			} catch (IllegalArgumentException e) {
				continue;
			}
		}
	}
	
	public void processDataset(LocalizationImage loc, double minDistance, double maxSigma) {

		locData = loc;
		minDistance2 = minDistance * minDistance;
		this.maxSigma = maxSigma; 

		maxFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMax();
		minFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMin();
		points = new ArrayList[maxFrame - minFrame + 1];

		for (int i = 0; i < points.length; i ++) {
			points[i] = new ArrayList<Integer>();
		}
		// create dataset, index 0 --> minFrame
		for (int i = 0; i < locData.getNumLocalizations(); i ++ ) {
			int frame = (int) locData.getData(locData.frameCol)[i];
			points[frame - minFrame].add(i);
		}

		for (int i = 0; i < points.length; i++) {
			points[i] = selectCandidate(points[i]);
		}
	}

	ArrayList<Integer> selectCandidate(List<Integer> data) {
		
		ArrayList<Integer> newData = new ArrayList<Integer>();
		
		boolean [] toRemove = new boolean[data.size()];

		for (int i = 0; i < data.size(); i++) {

			if (toRemove[i]) { continue;}

			int p1 = data.get(i);

//			if (locData.getData(locData.intensityCol)[p1] > maxIntensity) {
//				toRemove[i] = true;
//				continue;
//			}

			if (locData.getData(locData.sigmaCol)[p1] > maxSigma) {
				toRemove[i] = true;
				continue;
			}

			for (int j = 0; j < data.size(); j++) {
				
				if (i == j) { continue;}
				
				int p2 = data.get(j);
				
				if ( sqDistance(p1, p2) < minDistance2 ) {
					toRemove[i] = true;
					toRemove[j] = true;
					break;
				}
			}			
		}
		
		for (int i = 0 ; i < data.size(); i++) {
			if (! toRemove[i]) {
				newData.add(data.get(i));
			}
		}
		
		return newData;
	}

	double sqDistance(int p1, int p2) {
		return (locData.getXAt(p1) - locData.getXAt(p2)) * (locData.getXAt(p1) - locData.getXAt(p2))
				+ (locData.getYAt(p1) - locData.getYAt(p2)) * (locData.getYAt(p1) - locData.getYAt(p2));
	}
}
