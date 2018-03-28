package edu.uchc.octane.core.tracking;

import edu.uchc.octane.core.utils.HData;

public class HDataOnArray implements HData {

    double [] data;
    
    HDataOnArray(double[] data) {
        this.data = data;
    }

    @Override
    public int getDimension() {
        return data.length;
    }

    @Override
    public double get(int d) {
        return data[d];
    }
}
