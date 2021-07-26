package edu.uchc.octane.core.tracking;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.data.HData;
import edu.uchc.octane.core.data.LocalizationData;
import edu.uchc.octane.core.localizationimage.LocalizationImage;

public class TrackingDataFile extends OnePassTracking {

	int dimension;
	LocalizationImage locData;
	// double [][] data;
	int [] cols;
	ConnectionOptimizer optimizer;

	public TrackingDataFile(ConnectionOptimizer optimizer, int maxBlinking) {
		this(optimizer, maxBlinking, false);
	}

	public TrackingDataFile(ConnectionOptimizer optimizer, int maxBlinking, boolean is3D) {
		super(maxBlinking);
		this.optimizer = optimizer;
		dimension = is3D?3:2;
	}

	class TrackingHData implements HData {
		int idx;

		public TrackingHData(int idx) {
			this.idx = idx;
		}

		@Override
		public int getDimension() {
			return dimension;
		}

		@Override
		public double get(int d) {
			return locData.getData(cols[d])[idx];
		}
	}

	public LocalizationData processLocalizations(LocalizationImage loc) {
	    return processLocalizations(loc, false);
	}
	
	public LocalizationData processLocalizations(LocalizationImage loc, boolean doMerge) {
		locData = loc;
		LocalizationData odf = loc.getData();
		cols = new int[] {loc.xCol, loc.yCol, loc.zCol};

		int maxFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMax();
		int minFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMin();

		@SuppressWarnings("unchecked")
		List<TrackingHData> [] dataset = new ArrayList[maxFrame - minFrame + 1];

		for (int i = 0; i <= maxFrame - minFrame; i ++) {
			dataset[i] = new ArrayList<TrackingHData>();
		}
		// create dataset, index 0 --> minFrame
		for (int i = 0; i < locData.getNumLocalizations(); i ++ ) {
			int frame = (int) locData.getData(locData.frameCol)[i];
			dataset[frame - minFrame].add(new TrackingHData(i));
		}

		List<Trajectory> results = doTracking(dataset, optimizer);
		//List<Trajectory> results = doTracking(dataset, new MinSumDistance(maxDisplacement));

		//generate new datafile
		if (doMerge) {
		    double [][] newData = new double[odf.headers.length][results.size()];
		    for (int i = 0 ; i < results.size(); i ++) {
		        mergeTrack(results.get(i), newData, i);
		    }
		    
		    return new LocalizationData(newData, locData.getData().headers);

		} else {
		    double [][] origData = odf.data;
		    double [][] newData = new double[origData.length + 1][locData.getNumLocalizations()];
		    int s = 0;
		    for (int i = 0; i < results.size(); i++) {
		        s+= results.get(i).size();
		    }
		    assert(locData.getNumLocalizations() == s);
		    int curRow = 0;
		    for (int trackIdx = 0; trackIdx < results.size(); trackIdx++) {
		        Trajectory tr = results.get(trackIdx);
		        for (int i = 0; i < tr.size(); i ++) { // loop through each node in trajectory
		            TrackingHData d = (TrackingHData) tr.get(i);
		            int oldRow = d.idx;
		            for (int col = 0; col < locData.getNumOfCol(); col++ ) {
		                newData[col][curRow] = origData[col][oldRow];
		            }
		            newData[loc.getNumOfCol()][curRow] = trackIdx;
		            curRow ++;
		        }
		    }
		    
		    String [] newHeaders = new String[odf.headers.length + 1];
		    System.arraycopy(odf.headers, 0, newHeaders, 0, odf.headers.length);
		    newHeaders[newHeaders.length - 1] = "TrackIdx";
		    
		    return new LocalizationData(newData, newHeaders);
		}
	}

	public LocalizationData processLocalizations(LocalizationData data) {
	    return processLocalizations(data, false);
	}

	public LocalizationData processLocalizations(LocalizationData data, boolean doMerge) {
		return processLocalizations(new LocalizationImage(data), doMerge);
	}

	void mergeTrack(Trajectory track, double[][] target, int trackIdx) {

		for (int i = 0; i < track.size(); i ++) {
			TrackingHData d = (TrackingHData) track.get(i);
			for (int j = 0; j < target.length; j++) {
				target[j][trackIdx] += locData.getData(j)[d.idx];
			}
		}

		//average most
		for (int j = 0; j < target.length; j++) {
			target[j][trackIdx] /= track.size();
		}
		//except for frames
		target[locData.frameCol][trackIdx] = track.lastFrame;
		// and intensity
		if (locData.intensityCol != -1) {
			target[locData.intensityCol][trackIdx] *= track.size();
		}
		//and error, very rough est
//		if (locData.errCol != -1) {
//			target[locData.errCol][trackIdx] /= FastMath.sqrt(track.size());
//		}
	}
}
