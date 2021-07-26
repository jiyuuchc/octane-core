package edu.uchc.octane.core.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.data.HData;
import edu.uchc.octane.core.tracking.OnePassTracking.Trajectory;

public class MinSumDistance implements ConnectionOptimizer {

    final Logger logger = LoggerFactory.getLogger(getClass());

    int curFrame;

    double maxDistance2;
    HashMap<Trajectory, HData> connections;

    public MinSumDistance(double dist) {
        maxDistance2 = dist * dist;
        connections = new HashMap<Trajectory, HData>();
    }

    @Override
    public void connect(List<Trajectory> activeTracks, List<HData> newPoints, int curFrame) {
        // HData[] pointsArray = (HData[]) points.toArray();
        this.curFrame = curFrame;

        List<Trajectory> tracks = new LinkedList<Trajectory>(activeTracks);
        List<HData> points = new LinkedList<HData>(newPoints);

        int nConnections = 0;

        while (!tracks.isEmpty() && !points.isEmpty()) {

            List<Trajectory> starts = new ArrayList<Trajectory>();
            List<HData> ends = new ArrayList<HData>();

            starts.add(tracks.get(0));
            tracks.remove(0);
            int processedStarts = 0;
            int processedEnds = 0;

            while (processedStarts < starts.size() || processedEnds < ends.size()) {

                // divide into smaller networks (starts, ends)
                for (; processedStarts < starts.size(); processedStarts++) {
                    Trajectory tr = starts.get(processedStarts);
                    ListIterator<HData> it = points.listIterator();
                    while (it.hasNext()) {
                        HData point = it.next();
                        if (distanceSq(tr, point) < maxDistance2) {
                            ends.add(point);
                            it.remove();
                        }
                    }
                }

                for (; processedEnds < ends.size(); processedEnds++) {
                    HData point = ends.get(processedEnds);
                    ListIterator<Trajectory> it = tracks.listIterator();
                    while (it.hasNext()) {
                        Trajectory tr = it.next();
                        if (distanceSq(tr, point) < maxDistance2) {
                            starts.add(tr);
                            it.remove();
                        }
                    }
                }
            }

            // process the small network
            if (ends.size() > 0) {
                Trajectory[] s = new Trajectory[starts.size()];
                HData[] e = new HData[ends.size()];
                starts.toArray(s);
                ends.toArray(e);
                
                // logger.debug("subnetwork: {}:{}", s.length, e.length);
                
                formBestConnection(s, e);

                for (Trajectory tr : s) {

                    HData p = connections.get(tr);
                    if (p != null) {
                        tr.add(p);
                        tr.lastFrame = curFrame;
                        newPoints.remove(p);
                        
                        nConnections ++;
                    }
                }
            }
        }

        logger.info("Made {} connections", nConnections);

    }

    // recursively optimize a network to find conenction minimize total distance^2
    double formBestConnection(final Trajectory[] starts, final HData[] ends) {

        HashMap<Trajectory, HData> bestConnections;
        
        if (starts.length == 0) { // only new end points
            connections.clear();
            return ends.length * maxDistance2;
        }

        Trajectory tr = starts[0];
        Trajectory[] newStarts = Arrays.copyOfRange(starts, 1, starts.length);

        HData curMinEndPoint = null;
        double curMinDist2 = formBestConnection(newStarts, ends) + maxDistance2; // first option is not connect
        bestConnections = new HashMap<Trajectory, HData>(connections);

        for (int idx = 0; idx < ends.length; idx++) {
            double dist2 = distanceSq(tr, ends[idx]);

            if (dist2 < maxDistance2) {

                HData[] newEnds = Arrays.copyOfRange(ends, 1, ends.length);
                if (idx != 0) {
                    newEnds[idx - 1] = ends[0];
                }
                dist2 += formBestConnection(newStarts, newEnds);
                if (dist2 < curMinDist2) {
                    curMinDist2 = dist2;
                    curMinEndPoint = ends[idx];
                    bestConnections = new HashMap<Trajectory, HData>(connections);
                }
            }
        }

        connections = bestConnections;
        if (curMinEndPoint != null) {
            connections.put(tr, curMinEndPoint);
        }
        return curMinDist2;
    }

    double distanceSq(Trajectory t, HData p) {
        return t.get(t.size() - 1).sqDistance(p);
    }

//    void process(List<Trajectory> tracks, List<HData> points) {
//
//        int nConnections = 0;
//
//        while (!tracks.isEmpty() && !points.isEmpty()) {
//
//            List<Trajectory> starts = new ArrayList<Trajectory>();
//            List<HData> ends = new ArrayList<HData>();
//
//            starts.add(tracks.get(0));
//            tracks.remove(0);
//            int processedStarts = 0;
//            int processedEnds = 0;
//
//            while (processedStarts < starts.size() || processedEnds < ends.size()) {
//
//                // divide into smaller networks (starts, ends)
//                for (; processedStarts < starts.size(); processedStarts++) {
//                    Trajectory tr = starts.get(processedStarts);
//                    ListIterator<HData> it = points.listIterator();
//                    while (it.hasNext()) {
//                        HData point = it.next();
//                        if (distanceSq(tr, point) < maxDistance2) {
//                            ends.add(point);
//                            it.remove();
//                        }
//                    }
//                }
//
//                for (; processedEnds < ends.size(); processedEnds++) {
//                    HData point = ends.get(processedEnds);
//                    ListIterator<Trajectory> it = tracks.listIterator();
//                    while (it.hasNext()) {
//                        Trajectory tr = it.next();
//                        if (distanceSq(tr, point) < maxDistance2) {
//                            starts.add(tr);
//                            it.remove();
//                        }
//                    }
//                }
//            }
//
//            // process the small network
//            if (ends.size() > 0) {
//                Trajectory[] s = new Trajectory[starts.size()];
//                HData[] e = new HData[ends.size()];
//                starts.toArray(s);
//                ends.toArray(e);
//                
//                // logger.debug("subnetwork: {}:{}", s.length, e.length);
//                
//                formBestConnection(s, e);
//
//                for (Trajectory tr : s) {
//
//                    HData p = connections.get(tr);
//                    if (p != null) {
//                        tr.add(p);
//                        tr.lastFrame = curFrame;
//                        
//                        nConnections ++;
//                    }
//                }
//            }
//        }
//        
//        logger.info("Made {} connections", nConnections);
//    }
//
//    void process(Trajectory[] tracks, HData[] points) {
//        int curTrackIdx = 0, curPointIdx = 0;
//
//        while (curTrackIdx < tracks.length && curPointIdx < points.length) {
//            int trackIdx1 = curTrackIdx, trackIdx2 = curTrackIdx + 1;
//            int pointIdx1 = curPointIdx, pointIdx2 = curPointIdx;
//
//            while (trackIdx1 < trackIdx2 || pointIdx1 < pointIdx2) {
//                if (trackIdx1 < trackIdx2) {
//                    for (int i = pointIdx2; i < points.length; i++) {
//                        if (distanceSq(tracks[trackIdx1], points[i]) < maxDistance2) {
//                            swap(points, pointIdx2++, i);
//                        }
//                    }
//                    trackIdx1++;
//                }
//
//                if (pointIdx1 < pointIdx2) {
//                    for (int i = trackIdx2; i < tracks.length; i++) {
//                        if (distanceSq(tracks[i], points[pointIdx1]) < maxDistance2) {
//                            swap(tracks, trackIdx2++, i);
//                        }
//                    }
//                    pointIdx1++;
//                }
//            }
//            formBestConnection(Arrays.copyOfRange(tracks, curTrackIdx, trackIdx1),
//                    Arrays.copyOfRange(points, curPointIdx, pointIdx1));
//            curTrackIdx = trackIdx1;
//            curPointIdx = pointIdx1;
//        }
//    }

    <T> void swap(T[] arr, int idx1, int idx2) {
        T tmp = arr[idx1];
        arr[idx1] = arr[idx2];
        arr[idx2] = tmp;
    }

}
