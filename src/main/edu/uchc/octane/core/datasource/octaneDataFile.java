package edu.uchc.octane.core.datasource;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.simpleflatmapper.csv.CsvParser;
import org.simpleflatmapper.util.CloseableIterator;

public class octaneDataFile implements Serializable  {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public String [] headers;
	public double [][] data;

	public octaneDataFile(double[][] data, String []headers) {
		this.data = data;
		this.headers = headers;
	}

	public static octaneDataFile importFromThunderstorm(File csvFile) throws IOException {

		ArrayList<double[]> locations = new ArrayList<double[]>();
        try (CloseableIterator<String[]> it = CsvParser.iterator(csvFile)) {
        	String [] headers = it.next();
        	double [] oneLoc;
        	while (it.hasNext()) {
        		oneLoc = new double[headers.length];
        		String [] row = it.next();
        		for (int i = 0; i < row.length; i ++) {
        			oneLoc[i] = Double.parseDouble(row[i]);
        		}
        		locations.add(oneLoc);

        		if (locations.size() % 1000000 == 0) {
        			System.out.println("Read " + locations.size() + " lines.");
        		}
        	}

        	System.out.println("Convering...");
            double [][] data = new double[headers.length][];
            for (int i = 0; i < headers.length; i ++) {
            	data[i] = new double[locations.size()];
            }
            for (int i = 0; i < headers.length; i++) {
            	for (int j = 0; j < locations.size(); j++) {
            		data[i][j] = locations.get(j)[i];
            	}
            }

            octaneDataFile dataset = new octaneDataFile(data, headers);
            return dataset;
        }
	}
}
