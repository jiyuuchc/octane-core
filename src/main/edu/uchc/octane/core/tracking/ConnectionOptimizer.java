package edu.uchc.octane.core.tracking;

import java.util.List;

import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;
import edu.uchc.octane.core.utils.HData;

public interface ConnectionOptimizer {

	void connect(List<Trajectory> activeTracks, List<HData> points, int curFrame);
}
