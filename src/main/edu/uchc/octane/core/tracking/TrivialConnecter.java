package edu.uchc.octane.core.tracking;

import java.util.List;
import java.util.ListIterator;

import edu.uchc.octane.core.data.HData;
import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;

public class TrivialConnecter implements ConnectionOptimizer {
    double maxDisplacement2;

    public TrivialConnecter(double maxDisplacement) {
        maxDisplacement2 = maxDisplacement * maxDisplacement;
    }
    
    @Override
    public void connect(List<Trajectory> activeTracks, List<HData> points, int curFrame) {
        for (Trajectory track : activeTracks) {
            HData fromPoint = track.get(track.size() - 1);
            for (ListIterator<HData> it = points.listIterator(); it.hasNext();) {
                HData point = it.next();
                if (fromPoint.sqDistance(point) < maxDisplacement2) {
                    track.add(point);
                    track.lastFrame = curFrame;
                    it.remove();
                    break;
                }
            }
        }
    }
}
