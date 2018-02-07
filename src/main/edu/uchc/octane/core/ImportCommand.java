package edu.uchc.octane.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.json.JSONException;

import edu.uchc.octane.core.datasource.OctaneDataFile;

public class ImportCommand {

	static Options options;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("h");

		options.getOption("h").setDescription("print this message");

		return options;
	}

	public static void printHelp() {
		String syntax = "octane import [options] thunderstorm_data_file <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
	}

	public static void importAndConvert(List<String> args) throws IOException {
		System.out.println("Octane: import...");
        String csvFilepath = args.get(0);

        File csvFile = new File(csvFilepath);
        // long startTime = System.currentTimeMillis();
        System.out.println("Reading CSV file: " + csvFilepath);

        OctaneDataFile dataset = OctaneDataFile.importFromThunderstorm(csvFile);
        ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args.get(1)));

        System.out.println("Output file: " + args.get(1));
        fo.writeObject(dataset);
        fo.close();

	}

	public static void run(String [] args) throws JSONException, IOException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}

			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0) + ".octane";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			importAndConvert(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}
}
