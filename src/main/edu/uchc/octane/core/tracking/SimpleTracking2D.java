package edu.uchc.octane.core.tracking;

import java.util.ArrayList;

import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.core.datasource.Localizations;
import edu.uchc.octane.core.datasource.RawLocalizationData;
import edu.uchc.octane.core.utils.HData;

public class SimpleTracking2D extends SimpleTracking {

	public SimpleTracking2D(double maxDisplacement, int maxBlinking) {
		super(maxDisplacement, maxBlinking);
	}

	Localizations locData;
	double [][] data;

	class TrackingHData implements HData {
		int idx;

		public TrackingHData(int idx) {
			this.idx = idx;
		}

		@Override
		public int getDimension() {
			return 2;
		}

		@Override
		public double get(int d) {
			if (d==0) {
				return data[locData.xCol][idx];
			} else {
				return data[locData.yCol][idx];
			}
		}
	}

	public RawLocalizationData processLocalizations(Localizations loc) {
		locData = loc;
		data = loc.data;

		int maxFrame = (int) locData.getSummaryStatitics(locData.frameCol).getMax();
		ArrayList<TrackingHData> [] dataset = new ArrayList[maxFrame];
		double [][] data = locData.data;
		for (int i = 0; i < maxFrame; i ++) {
			dataset[i] = new ArrayList<TrackingHData>();
		}
		for (int i = 0; i < data[locData.frameCol].length; i ++ ) {
			int frame = (int) data[locData.frameCol][i] - 1;
			if (frame >=0 && frame < maxFrame) {
				dataset[frame].add(new TrackingHData(i));
			}
		}

		ArrayList<Trajectory> results = doTracking(dataset);

		double [][] newData = new double[data.length][results.size()];
		for (int i = 0 ; i < results.size(); i ++) {
			mergeTrack(results.get(i), newData, i);
		}
		return new RawLocalizationData(newData, locData.getHeaders());
	}

	void mergeTrack(Trajectory track, double[][] target, int idx) {

		for (int i = 0; i < track.size(); i ++) {
			TrackingHData d = (TrackingHData) track.get(i);
			for (int j = 0; j < target.length; j++) {
				target[j][idx] += data[j][d.idx];
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
