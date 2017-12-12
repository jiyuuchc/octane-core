package edu.uchc.octane.core;

import java.util.Arrays;

public class Main {

	public static void printHelp() {
		System.out.println("Octane v0.1 - available commands");
		System.out.println("\tanalyze\t analyze raw image data for localizations");
		System.out.println("\tdrift\t correct drift in the localzation data");
		System.out.println("\timport\t import localization data from Thunderstorm output");
		System.out.println("\tmerge\t track trajectories and merge coordinantes" );
		System.out.println();
		System.out.println("octane cmd -h for further help");
	}

	public static void main(String[] args) throws Exception {

		String cmd = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);

		if (cmd.equals("about") ){
			System.out.println("octane 0.1");
			System.out.println("Single molecule localization microscopy data analysis tools");
		} else if (cmd.equals("import")) {
			ImportCommand.run(args);
		}  else if (cmd.equals("drift")) {
			DriftCommand.run(args);
		} else if (cmd.equals("analyze")) {
			AnalyzeCommand.run(args);
		} else if (cmd.equals("merge")) {
			TrackingCommand.run(args);
		} else {
			printHelp();
		}

		System.exit(0);
	}
}
