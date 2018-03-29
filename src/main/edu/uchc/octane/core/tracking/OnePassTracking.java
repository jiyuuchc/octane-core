package edu.uchc.octane.core.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.utils.HData;

/**
 * Tracking module to generate trajectories
 * 
 * @author Ji-Yu
 *
 */
public class OnePassTracking {

    final Logger logger = LoggerFactory.getLogger(getClass());

    public class Trajectory extends ArrayList<HData> {
        int lastFrame;

        Trajectory(int lastFrame) {
            super();
            this.lastFrame = lastFrame;
        }

    };

    int maxBlinking;

    public OnePassTracking(int maxBlinking) {
        this.maxBlinking = maxBlinking;
    }

    /**
     * Create trajectories.
     *
     * @return the trajectories
     */
    public ArrayList<Trajectory> doTracking(Collection<? extends HData>[] localizations,
            ConnectionOptimizer connecter) {

        List<Trajectory> activeTracks = new LinkedList<Trajectory>();
        ArrayList<Trajectory> trajectories = new ArrayList<Trajectory>();

        // initial track # = first frame particle #
        for (HData point : localizations[0]) {
            Trajectory t = new Trajectory(0);
            t.add(point);
            activeTracks.add(t);
        }

        int curFrame = 0;
        while (++curFrame < localizations.length) {

            List<HData> points = new LinkedList<HData>();
            points.addAll(localizations[curFrame]);

            logger.info("Frame:%d - %d active tracks, %d candidates, %d stopped tracks",
                    curFrame, activeTracks.size(), points.size(), trajectories.size());

            connecter.connect(activeTracks, points, curFrame);

            // remove all tracks that has been lost for too long
            // int firstPos = xytData_.getFirstOfFrame(curFrame_);
            for (ListIterator<Trajectory> it = activeTracks.listIterator(); it.hasNext();) {
                Trajectory track = it.next();
                if (curFrame - track.lastFrame > maxBlinking) {
                    it.remove();
                    trajectories.add(track);
                }
            }

            // add new particles into the track list
            for (HData point : points) {
                Trajectory t = new Trajectory(curFrame);
                t.add(point);
                activeTracks.add(t);
            }
        } // while

        trajectories.addAll(activeTracks);
        activeTracks = null;

        return trajectories;
    } // doTracking
}
