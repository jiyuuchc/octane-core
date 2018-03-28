package edu.uchc.octane.core.tracking;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.localizationimage.LocalizationImage;
import edu.uchc.octane.core.utils.HData;

public class TrackingDataFile extends OnePassTracking {

	int dimension;
	LocalizationImage locData;
	// double [][] data;
	int [] cols;
	double maxDisplacement;

	public TrackingDataFile(double maxDisplacement, int maxBlinking) {
		this(maxDisplacement, maxBlinking, false);
	}

	public TrackingDataFile(double maxDisplacement, int maxBlinking, boolean is3D) {
		super(maxBlinking);
		this.maxDisplacement = maxDisplacement; 
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

	public OctaneDataFile processLocalizations(LocalizationImage loc) {
		locData = loc;
		//data = loc.data;
		cols = new int[] {loc.xCol, loc.yCol, loc.zCol};
		//double [][] data = locData.data;

		int maxFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMax();
		List<TrackingHData> [] dataset = new ArrayList[maxFrame];

		for (int i = 0; i < maxFrame; i ++) {
			dataset[i] = new ArrayList<TrackingHData>();
		}
		for (int i = 0; i < locData.getNumLocalizations(); i ++ ) {
			int frame = (int) locData.getData(locData.frameCol)[i] - 1;
			if (frame >=0 && frame < maxFrame) {
				dataset[frame].add(new TrackingHData(i));
			}
		}

		List<Trajectory> results = doTracking(dataset, new TrivialConnecter(maxDisplacement));

		double [][] newData = new double[locData.getNumOfCol()][results.size()];
		for (int i = 0 ; i < results.size(); i ++) {
			mergeTrack(results.get(i), newData, i);
		}
		return new OctaneDataFile(newData, locData.getHeaders());
	}

	public OctaneDataFile processLocalizations(OctaneDataFile data) {
		return processLocalizations(new LocalizationImage(data));
	}

	void mergeTrack(Trajectory track, double[][] target, int idx) {

		for (int i = 0; i < track.size(); i ++) {
			TrackingHData d = (TrackingHData) track.get(i);
			for (int j = 0; j < target.length; j++) {
				target[j][idx] += locData.getData(j)[d.idx];
			}
		}

		//average most
		for (int j = 0; j < target.length; j++) {
			target[j][idx] /= track.size();
		}
		//except for frames
		target[locData.frameCol][idx] = track.lastFrame;
		// and intensity
		if (locData.intensityCol != -1) {
			target[locData.intensityCol][idx] *= track.size();
		}
		//and error, very rough est
		if (locData.errCol != -1) {
			target[locData.errCol][idx] /= FastMath.sqrt(track.size());
		}
	}
}
