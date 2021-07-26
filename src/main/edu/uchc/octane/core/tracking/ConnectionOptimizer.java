package edu.uchc.octane.core.tracking;

import java.util.List;

import edu.uchc.octane.core.data.HData;
import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;

public interface ConnectionOptimizer {

	void connect(List<Trajectory> activeTracks, List<HData> points, int curFrame);
}
