package edu.uchc.octane.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.uchc.octane.core.datasource.RawLocalizationData;
import edu.uchc.octane.core.drift.CorrelationEstimator;
import edu.uchc.octane.core.image.LocalizationImage;

public class Main {

//	final static int IDX_FRAME = 0;
//	final static int IDX_X = 1;
//	final static int IDX_Y = 2;
//	final static int IDX_SIGMA = 3;
//	final static int IDX_INTENSITY = 4;
//	final static int IDX_OFFSET = 5;
//	final static int IDX_BG = 6;
//	final static int IDX_CHI = 7;
//	final static int IDX_ERR = 8;

	public static void readTest(String [] args) throws Exception {

		long startTime = System.currentTimeMillis();
        ObjectInputStream fi = new ObjectInputStream(new FileInputStream(args[0]));

        LocalizationImage d = new LocalizationImage(fi);
        fi.close();

        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        startTime = endTime;

        d.constructKDtree();

        endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
	}

	public static void importAndConvert(String [] args) throws IOException {
		System.out.println("Octane: import...");
        String csvFilepath = args[1];

        File csvFile = new File(csvFilepath);
        // long startTime = System.currentTimeMillis();
        System.out.println("Reading CSV file: " + csvFilepath);
        RawLocalizationData dataset = RawLocalizationData.importFromThunderstorm(csvFile);
        ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args[2]));
        System.out.println("Output file: " + args[2]);
        fo.writeObject(dataset);
        fo.close();

        //long endTime = System.currentTimeMillis() ;
        //System.out.println(endTime - startTime);

	}

	public static void basdi(String [] args ) throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream fi = new ObjectInputStream(new FileInputStream(args[1]));

        LocalizationImage data = new LocalizationImage(fi);
        fi.close();

	}

	public static void drift(String [] args) throws IOException, IOException, ClassNotFoundException {

		CorrelationEstimator c = new CorrelationEstimator();
		System.out.println("Loading File...");
		ObjectInputStream s = new ObjectInputStream(new FileInputStream(args[1]));
		RawLocalizationData d = (RawLocalizationData) s.readObject();
		s.close();
		System.out.println("Load File: done");

		System.out.println("Estimating...");
		//c.estimate( d, new Rectangle(1000,1000,2048,2048), 20);
		c.estimate( d, null, 10);
		c.correct(d);

		if (args.length > 2) {
			System.out.println("Saving results...");
	        ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args[2]));
	        fo.writeObject(d);
	        fo.close();
	        System.out.println("Saving : done");
		}
	}

	public static void main(String[] args) throws Exception {

		String cmd = args[0];

		if (cmd.equals("about") ){
			System.out.println("octane 0.1");
			System.out.println("Single molecule localization microscopy data analysis tools");
		}
		else if (cmd.equals("import")) {
			importAndConvert(args);
		} else if (cmd.equals("basdi")) {
			basdi(args);
		} else if (cmd.equals("drift")) {
			drift(args);
		}
	}
}
