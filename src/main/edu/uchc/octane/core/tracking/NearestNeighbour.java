package edu.uchc.octane.core.tracking;

import java.util.List;

import edu.uchc.octane.core.data.HData;
import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;

public class NearestNeighbour implements ConnectionOptimizer {

	double maxDisplacement2;

	public NearestNeighbour(double maxDisplacement) {
		maxDisplacement2 = maxDisplacement * maxDisplacement;
	}

	@Override
	public void connect(List<Trajectory> activeTracks, List<HData> points, int curFrame) {
		for (Trajectory track : activeTracks) {
			HData fromPoint = track.get(track.size()-1);
			double curMin = Double.MAX_VALUE;
			HData curPoint = null;
			for (HData point: points) {
				double d = fromPoint.sqDistance(point);
				if ( d < maxDisplacement2  && d < curMin) {
					curMin = d;
					curPoint = point;
				}
			}
			if (curPoint != null) {
				track.add(curPoint);
				track.lastFrame = curFrame;
				points.remove(curPoint);
			}
		}
	}
}

