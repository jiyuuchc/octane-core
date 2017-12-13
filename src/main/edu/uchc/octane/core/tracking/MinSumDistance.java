package edu.uchc.octane.core.tracking;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;
import edu.uchc.octane.core.utils.HData;

public class MinSumDistance implements ConnectionOptimizer {

	double maxDistance2;
	HashMap<Trajectory, HData> connections;

	@Override
	public void connect(Iterable<Trajectory> activeTracks, Set<HData> points, int curFrame) {
		HData[] pointsArray = (HData[]) points.toArray();
	}

	double formBestConnection(final Trajectory [] starts, final HData [] ends) {
		if (starts.length == 0 || ends.length == 0) return 0;

		Trajectory tr = starts[0];
		Trajectory [] newStarts = Arrays.copyOfRange(starts, 1, starts.length);

		HData minP = null;
		double min = formBestConnection(newStarts, ends) + maxDistance2; // first option is not connect
		for (int idx = 0; idx < ends.length; idx ++) {
			double curDist = distance(tr, ends[idx]);

			HData [] newEnds = Arrays.copyOfRange(ends, 1, ends.length);
			if (idx != 0) {
				newEnds[idx - 1] = ends[0];
			}
			curDist += formBestConnection(newStarts, newEnds);
			if (curDist < min) {
				min = curDist;
				minP = ends[idx];
			}
		}
		connections.put(tr, minP);
		return min;
	}

	double distance(Trajectory t, HData p) {
		return t.get(t.size()-1).sqDistance(p);
	}

	<T> void swap(T [] arr, int idx1, int idx2) {
		T tmp = arr[idx1];
		arr[idx1] = arr[idx2];
		arr[idx2] = tmp;
	}

	void processNextFrame(Trajectory [] tracks, HData [] points, int curFrame) {
		int startTrack = 0, startPoint = 0;
		connections = new HashMap<Trajectory, HData>();

		while (startTrack < tracks.length && startPoint < points.length ){
			int tracksP1 = startTrack, tracksP2 = startTrack + 1;
			int pointsP1 = startPoint, pointsP2 = startPoint;

			while (tracksP1 < tracksP2 || pointsP1 < pointsP2) {
				if (tracksP1 < tracksP2) {
					for (int i = pointsP2; i < points.length; i++) {
						if (distance(tracks[tracksP1], points[i]) < maxDistance2) {
							swap(points, pointsP2 ++ , i);
						}
					}
					tracksP1 ++;
				}

				if (pointsP1 < pointsP2) {
					for (int i = tracksP2; i < tracks.length; i++) {
						if (distance(tracks[i], points[pointsP1]) < maxDistance2) {
							swap(tracks, tracksP2 ++, i);
						}
					}
					pointsP1 ++;
				}
			}
			formBestConnection(Arrays.copyOfRange(tracks, startTrack, tracksP1), Arrays.copyOfRange(points, startPoint, pointsP1));
			startTrack = tracksP1;
			startPoint = pointsP1;
		}

		for ( int i = 0 ; i < tracks.length; i++) {
			Trajectory tr = tracks[i];
			HData p = connections.get(tr);
			if ( p != null ) {
				tr.add(p);
				tr.lastFrame = curFrame;
			}
		}
	}
}
