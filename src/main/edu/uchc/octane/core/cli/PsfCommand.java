package edu.uchc.octane.core.cli;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.json.JSONException;
import org.micromanager.acquisition.TaggedImageStorageMultipageTiff;

import edu.uchc.octane.core.datasource.OctaneDataFile;
import edu.uchc.octane.core.localizationimage.LocalizationImage;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import edu.uchc.octane.core.psfestimator.Estimator;
import edu.uchc.octane.core.psfestimator.Preprocessor;
import mmcorej.TaggedImage;

public class PsfCommand {

	static Options options;
	static long windowSize = 5;
	static long psfSize = 6;
	static double minDistance = 3000;
	static double maxSigma = 200; // FIXME set as an option
	static double pixelSize = 160;
	static long scale = 3;

	static double DEFAULT_SIGMA = 125;
	static double DEFAULT_BG = 1800;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("hw%W%d%p%f%");

		options.getOption("h").setDescription("print this message");
		options.getOption("W").setDescription("fitting window size");
		options.getOption("w").setDescription("psf size");
		options.getOption("d").setDescription("minimal distance between candidate spots");
		options.getOption("p").setDescription("pixel size");
		options.getOption("f").setDescription("scaling factor");
		
		return options;
	}

	public static void printParameters() {
		System.out.println("Estimate PSF from data...");
		System.out.println("Fitting window size = " + windowSize);
		System.out.println("PSF size = " + psfSize);
		System.out.println("Cnadidate minimal distance = " + minDistance);
		System.out.println("Pixels size = " + pixelSize);
		System.out.println("PSF scaling = " + scale);
	}
	
	public static void printHelp() {
		String syntax = "octane psf [options] data_file_folder octane_file";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void run(String [] args) throws JSONException, IOException, ClassNotFoundException {

		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}

			windowSize = CommandUtils.getParsedLong(cmd, "W", windowSize);
			psfSize = CommandUtils.getParsedLong(cmd, "w", psfSize);
			minDistance = CommandUtils.getParsedDouble(cmd, "d", minDistance);
			pixelSize = CommandUtils.getParsedDouble(cmd, "p", pixelSize);
			scale = CommandUtils.getParsedLong(cmd, "f", scale);

			List<String> remainings = cmd.getArgList();
			if (remainings.size() != 2) {
				printHelp();
				return;
			}
			
			estimatePsf(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}
	
	public static void estimatePsf(List<String> args) throws ClassNotFoundException, IOException, JSONException {
		
		System.out.println("Estimate PSF from : " + args.get(0) + ", " + args.get(1));
		printParameters();
		
		Preprocessor preprocessor = new Preprocessor();

		LocalizationImage locData = new LocalizationImage(OctaneDataFile.readFromFile(args.get(1)));
		
		System.out.println("Read localization data from " + args.get(1));
		System.out.println("Total localizations:  " + locData.getNumLocalizations());
		
		preprocessor.processDataset(locData, minDistance, maxSigma);
		
		System.out.println("Selected " + preprocessor.getNumPoints() + " psf candidats");

		TaggedImageStorageMultipageTiff stackReader = new TaggedImageStorageMultipageTiff(args.get(0), false, null, false, false, false);

		int maxFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMax();
		int minFrame = (int) locData.getSummaryStatistics(locData.frameCol).getMin();
		for (int f = minFrame; f <= maxFrame; f++) {

			TaggedImage img= stackReader.getImage(0 /*channel*/, 0 /*slice*/, f /*frame*/, 0 /*position*/);
			short [] iPixels = (short[]) img.pix;
			double [] pixels = new double[iPixels.length];

			for (int i = 0; i < pixels.length; i ++) {
				pixels[i] = iPixels[i];
			}
			
			RectangularDoubleImage curFrame = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
			preprocessor.processImages(curFrame, f, (int) windowSize, (int)pixelSize); 
		}
		
		System.out.println("Obtained " + preprocessor.getNumSubImages() + " sub-images");
		
		double [] bgs = new double[preprocessor.getNumSubImages()];
		Arrays.fill(bgs, DEFAULT_BG);
		Estimator estimator = new Estimator(
				(int) scale,
				preprocessor.getSubImages(),
				preprocessor.getXCenters(),
				preprocessor.getYCenters(),
				bgs,
				preprocessor.getIntensities());
		RectangularDoubleImage psf = estimator.estimate((int)(psfSize * 2 + 1), DEFAULT_SIGMA / pixelSize);
		
		System.out.println("PSF output :");
		for (int i = 0; i < psf.getLength(); i++) {
			System.out.format("%f",psf.getValue(i));
			if ((i + 1) % (psfSize * 2 + 1) == 0) {
				System.out.println("");
			} else {
				System.out.print(", ");
			}
		}
	}
}
