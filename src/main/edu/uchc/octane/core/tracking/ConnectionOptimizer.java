package edu.uchc.octane.core.tracking;

import java.util.Set;

import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;
import edu.uchc.octane.core.utils.HData;

public interface ConnectionOptimizer {

	void connect(Iterable<Trajectory> activeTracks, Set<HData> points, int curFrame);
}
