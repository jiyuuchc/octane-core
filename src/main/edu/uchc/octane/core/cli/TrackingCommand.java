package edu.uchc.octane.core.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.localizationimage.LocalizationImage;
import edu.uchc.octane.core.tracking.TrackingDataFile;

public class TrackingCommand {
	static Options options;
	static double trackingDistance;
	static long blinkings;
	static LocalizationImage locData;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("ht%b%");

		options.getOption("h").setDescription("print this message");
		options.getOption("t").setDescription("maximum tracking distance");
		options.getOption("b").setDescription("maximum blinking frames");
		return options;
	}

	public static void printHelp() {
		String syntax = "octane merge [options] thunderstorm_data_file <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
		System.out.println("Tracking distance : " + trackingDistance);
		System.out.println("Blinking : " + blinkings);
	}

	public static void tracking(List<String> args) throws IOException, ClassNotFoundException {
		System.out.println("Octane: merge tracks ...");

		System.out.println("Loading File : " + args.get(0));
		ObjectInputStream s = new ObjectInputStream(new FileInputStream(args.get(0)));
		s.close();
		System.out.println("Load File: done");

		System.out.println("Tracking ...");
		printParameters();

		locData = new LocalizationImage((OctaneDataFile) s.readObject());
		TrackingDataFile tracker = new TrackingDataFile(trackingDistance, (int) blinkings);

		OctaneDataFile mergedData = tracker.processLocalizations(locData);

        ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args.get(1)));
        System.out.println("Output file: " + args.get(1));
        fo.writeObject(mergedData);
        fo.close();

	}

	public static void run(String [] args) throws IOException, ClassNotFoundException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}
			trackingDistance = CommandUtils.getParsedDouble(cmd, "t", 200.0);
			blinkings = CommandUtils.getParsedLong(cmd, "b", 1);

			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0) + ".t";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			tracking(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}

}
