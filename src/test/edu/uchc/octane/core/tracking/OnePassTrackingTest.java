package edu.uchc.octane.core.tracking;

import org.junit.Test;

import edu.uchc.octane.core.utils.HData;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class OnePassTrackingTest {

    @Test
    public void testTrackingNoBlining() {

        List<HDataOnArray>[] data = new ArrayList[2];
        data[0] = new ArrayList<HDataOnArray>();
        data[1] = new ArrayList<HDataOnArray>();

        // frame 1
        data[0].add(new HDataOnArray(new double[] { 0, 0 }));
        data[0].add(new HDataOnArray(new double[] { 400, 300 }));

        // frame 2
        data[1].add(new HDataOnArray(new double[] { 10, 40 }));
        data[1].add(new HDataOnArray(new double[] { 400, 900 }));

        OnePassTracking tracker = new OnePassTracking(0);
        List<OnePassTracking.Trajectory> result = tracker.doTracking(data, new TrivialConnecter(100));

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(1700, calcTotalSqDist(result), 0.0001);
    }

    @Test
    public void testTrackingWithBlinking() {

        List<HDataOnArray>[] data = new ArrayList[3];
        for (int i = 0; i < data.length; i++) {
            data[i] = new ArrayList<HDataOnArray>();
        }

        // frame 1
        data[0].add(new HDataOnArray(new double[] { 0, 0 }));
        data[0].add(new HDataOnArray(new double[] { 400, 300 }));

        // frame 2
        data[1].add(new HDataOnArray(new double[] { 400, 290 }));
        data[1].add(new HDataOnArray(new double[] { 400, 900 }));

        // frame 3
        data[2].add(new HDataOnArray(new double[] { 10, 40 }));

        OnePassTracking tracker = new OnePassTracking(1);
        List<OnePassTracking.Trajectory> result = tracker.doTracking(data, new TrivialConnecter(100));

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(1800, calcTotalSqDist(result), 0.0001);
    }

    @Test
    public void testTrackingNearestNeighbour() {

        List<HDataOnArray>[] data = new ArrayList[3];
        for (int i = 0; i < data.length; i++) {
            data[i] = new ArrayList<HDataOnArray>();
        }

        // frame 1
        data[0].add(new HDataOnArray(new double[] { 0, 0 }));

        // frame 2
        data[1].add(new HDataOnArray(new double[] { 0, 20 }));
        data[1].add(new HDataOnArray(new double[] { 0, 10 }));
        data[1].add(new HDataOnArray(new double[] { 30, 10 }));

        OnePassTracking tracker = new OnePassTracking(0);
        List<OnePassTracking.Trajectory> result = tracker.doTracking(data, new NearestNeighbour(100));

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(100, calcTotalSqDist(result), 0.0001);
    }

    @Test
    public void testTrackingGlobalOptim() {

        List<HDataOnArray>[] data = new ArrayList[2];
        data[0] = new ArrayList<HDataOnArray>();
        data[1] = new ArrayList<HDataOnArray>();

        // frame 1
        data[0].add(new HDataOnArray(new double[] { 0, 0 }));
        data[0].add(new HDataOnArray(new double[] { 100, 0 }));

        // frame 2
        data[1].add(new HDataOnArray(new double[] { 30, 0 }));
        data[1].add(new HDataOnArray(new double[] { -40, 0 }));

        OnePassTracking tracker = new OnePassTracking(0);
        List<OnePassTracking.Trajectory> result = tracker.doTracking(data, new MinSumDistance(100));

        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(6500, calcTotalSqDist(result), 0.0001);
    }

    public double calcTotalSqDist(List<OnePassTracking.Trajectory> result) {
        double totalDist = 0;
        for (int i = 0; i < result.size(); i++) {
            OnePassTracking.Trajectory tr = result.get(i);
            HData p = tr.get(0);
            for (int j = 1; j < tr.size(); j++) {
                totalDist += tr.get(j).sqDistance(p);
            }
        }
        return totalDist;
    }
}
