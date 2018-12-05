package edu.uchc.octane.core.datasource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchc.octane.core.utils.CSVUtils;

public class OctaneDataFile implements Serializable {

    private static final long serialVersionUID = 1L;

    static final Logger logger = LoggerFactory.getLogger(OctaneDataFile.class);

    public String[] headers;
    public double[][] data;

    void validate() {
    	if (headers.length != data.length) {
    		throw new IllegalArgumentException("Header and data size mismatch");
    	}
    	
    	for (int i = 1; i < data.length; i++) {
    		if (data[i].length != data[i-1].length) {
    			throw new IllegalArgumentException("Not all data columns have the same length");
    		}
    	}    	
    }
    
    public OctaneDataFile(double[][] data, String[] headers) {

    	this.data = data;
        this.headers = headers;
        
        validate();
    }

    //copy constructor
    public OctaneDataFile(OctaneDataFile odf) {

    	headers = odf.headers.clone();
        data = new double[odf.data.length][];
        for (int i = 0; i < data.length; i++) {
            data[i] = odf.data[i].clone();
        }

    }

    //merge ignores header differences
    public void mergeWith(OctaneDataFile newOdf) {
    	if (newOdf == null) {
    		return;
    	}
        if (data.length != newOdf.data.length) {
            throw new IllegalArgumentException("Data dimension doesn't match");
        }

        double [][] oldData = data.clone();
        double [][] newData = newOdf.data;
        for (int i = 0; i < data.length; i ++) {
            int newlen = oldData[i].length + newData[i].length ;
            data[i] = new double[newlen];
            System.arraycopy(oldData[i], 0, data[i], 0, oldData[i].length);
            System.arraycopy(newData[i], 0, data[i], oldData[i].length, newData[i].length);
        }    	
    }
    
    public void exportToCSV(File csvFile) throws IOException{

    	logger.info("Writing CSV data...");
        BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile));

        for (int i = 0; i < data.length; i++) {
            bw.write(headers[i]);
            if (i != data.length - 1) {
                bw.write(",");
            }
        }
        bw.newLine();
        
        for (int i = 0; i < data[0].length; i ++) {
            for (int j = 0; j < data.length; j++) {
                bw.write(Double.toString(data[j][i]));
                if (j != data.length - 1) {
                    bw.write(",");
                }
            }
            bw.newLine();
        }

        bw.close();
    }
    
    public static OctaneDataFile readFromFile(String pathname) throws IOException, ClassNotFoundException {

    	ObjectInputStream fi = new ObjectInputStream(new java.io.FileInputStream(pathname));
        OctaneDataFile odf = (OctaneDataFile) fi.readObject();
        
        fi.close();
        
        try {
        	odf.validate();
        } catch (IllegalArgumentException e){
        	throw new ClassNotFoundException(e.getMessage());
        }
        return odf;
    }
    
    public void writeToFile(String pathname) throws IOException {
		ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(pathname));
		fo.writeObject(data);
		fo.close();    	
    }

    public static OctaneDataFile importFromCSV(File csvFile) {

        logger.info("Importing CSV data...");
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            ArrayList<double[]> locations = new ArrayList<double[]>();
            String line = br.readLine();
            List<String> h = CSVUtils.parseLine(line);
            
            String [] headers = new String[h.size()];
            h.toArray(headers);

            while ((line = br.readLine()) != null) {

                List<String> row = CSVUtils.parseLine(line);
                double[] oneLoc = new double[row.size()];
                for (int i = 0; i < row.size(); i++) {
                    try {
                        oneLoc[i] = Double.parseDouble(row.get(i));
                    } catch (NumberFormatException e) {
                        oneLoc[i] = 0;
                    }
                }
                locations.add(oneLoc);
            }

            logger.info("Converting from CSV format.");

            double[][] data = new double[headers.length][];

            for (int i = 0; i < headers.length; i++) {
                data[i] = new double[locations.size()];
            }

            for (int i = 0; i < headers.length; i++) {
                for (int j = 0; j < locations.size(); j++) {
                    data[i][j] = locations.get(j)[i];
                }
            }

            OctaneDataFile dataset = new OctaneDataFile(data, headers);

            return dataset;

        } catch (IOException e) {
            logger.error("IO error");
            logger.error(e.getMessage());
            return null;
        }

        // try (CloseableIterator<String[]> it = CsvParser.iterator(csvFile)) {
        // String [] headers = it.next();
        // double [] oneLoc;
        // while (it.hasNext()) {
        // oneLoc = new double[headers.length];
        // String [] row = it.next();
        // for (int i = 0; i < row.length; i ++) {
        // oneLoc[i] = Double.parseDouble(row[i]);
        // }
        // locations.add(oneLoc);
        //
        // if (locations.size() % 1000000 == 0) {
        // logger.info("Read " + locations.size() + " lines.");
        // }
        // }
        //
        // logger.info("Converting from thunderstorm format.");
        // double [][] data = new double[headers.length][];
        // for (int i = 0; i < headers.length; i ++) {
        // data[i] = new double[locations.size()];
        // }
        // for (int i = 0; i < headers.length; i++) {
        // for (int j = 0; j < locations.size(); j++) {
        // data[i][j] = locations.get(j)[i];
        // }
        // }
        //
        // OctaneDataFile dataset = new OctaneDataFile(data, headers);
        // return dataset;
        // }
    }
}
