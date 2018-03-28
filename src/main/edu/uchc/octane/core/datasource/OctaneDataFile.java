package edu.uchc.octane.core.datasource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public OctaneDataFile(double[][] data, String[] headers) {
        this.data = data;
        this.headers = headers;
    }

    public OctaneDataFile(OctaneDataFile odf) {
        headers = odf.headers.clone();
        data = new double[odf.data.length][];
        for (int i = 0; i < data.length; i++) {
            data[i] = odf.data[i].clone();
        }
    }

    public static OctaneDataFile importFromThunderstorm(File csvFile) throws IOException {

        logger.info("Reading data...");
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            ArrayList<double[]> locations = new ArrayList<double[]>();
            String line = br.readLine();
            String [] headers = (String[]) CSVUtils.parseLine(line).toArray();

            while ((line = br.readLine()) != null) {

                List<String> row = CSVUtils.parseLine(line);
                double[] oneLoc = new double[row.size()];
                for (int i = 0; i < row.size(); i++) {
                    oneLoc[i] = Double.parseDouble(row.get(i));
                }
                locations.add(oneLoc);
            }

            logger.info("Converting from thunderstorm format.");

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
