package edu.uchc.octane.core.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
import edu.uchc.octane.core.fitting.AsymmetricGaussianPSF;
import edu.uchc.octane.core.fitting.DAOFitting;
import edu.uchc.octane.core.fitting.IntegratedGaussianPSF;
import edu.uchc.octane.core.fitting.LeastSquare;
import edu.uchc.octane.core.frameanalysis.LocalMaximum;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import mmcorej.TaggedImage;

public class AnalyzeCommand {

	static Options options;
	static long windowSize = 3;
	static long thresholdIntensity = 100;
	static long backgroundIntensity = 1600;
	static long startingFrame = 0;
	static long endingFrame = -1;
	static double pixelSize = 160;
	static boolean multiPeak = false;
	static boolean asymmetric = false;

	static int [] cnt;
	static List<double[]> positions;

	public static Options setupOptions() {
		options = PatternOptionBuilder.parsePattern("hw%t%b%s%e%p%ma");

		options.getOption("h").setDescription("print this message");
		options.getOption("w").setDescription("fitting window size");
		options.getOption("t").setDescription("intensity threshold value");
		options.getOption("b").setDescription("background intensity");
		options.getOption("s").setDescription("starting frame");
		options.getOption("e").setDescription("ending frame");
		options.getOption("p").setDescription("pixel size");
		options.getOption("m").setDescription("perform multi-peak fitting");
		options.getOption("a").setDescription("asymmetric psf fitting (for 3D)");

		return options;
	}

	public static void printHelp() {
		String syntax = "octane analyze [options] data_file_folder <output_file>";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(syntax, options);
	}

	public static void printParameters() {
		System.out.println("Processing Frame : " + startingFrame + " - " + endingFrame);
		System.out.println("Background intensity = " + backgroundIntensity);
		System.out.println("Threshold intensity = " + thresholdIntensity);
		System.out.println("Fitting window size = " + windowSize);
		System.out.println("Pixels size = " + pixelSize);
		System.out.println("Multi-peak fitting: " + (multiPeak?"yes":"no"));
		System.out.println("3D fitting: " + (asymmetric?"yes":"no"));
	}

	public static void run(String [] args) throws JSONException, IOException {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(setupOptions(), args);

			if (cmd.hasOption("h")) {
				printHelp();
				return;
			}

			windowSize = CommandUtils.getParsedLong(cmd, "w", windowSize);
			thresholdIntensity = CommandUtils.getParsedLong(cmd, "t", thresholdIntensity );
			backgroundIntensity = CommandUtils.getParsedLong(cmd, "b", backgroundIntensity  );
			startingFrame = CommandUtils.getParsedLong(cmd, "s", startingFrame);
			endingFrame = CommandUtils.getParsedLong(cmd, "e", endingFrame);;
			pixelSize = CommandUtils.getParsedDouble(cmd, "p", pixelSize);
			multiPeak = cmd.hasOption("m");
			asymmetric = cmd.hasOption("a");

			List<String> remainings = cmd.getArgList();
			if (remainings.size() == 1) {
				String outputFile = remainings.get(0).replaceAll("/+$", "") + ".octane";
				remainings.add(outputFile);
			}

			if (remainings.size() != 2) {
				printHelp();
				return;
			}

			process(remainings);

		} catch (ParseException | ClassCastException e) {
			printHelp();
			return;
		}
	}

	static void processFrame(TaggedImage img, int frame) throws JSONException {
		short [] iPixels = (short[]) img.pix;
		double [] pixels = new double[iPixels.length];
		LeastSquare fitter;
		if (asymmetric) {
			fitter = new LeastSquare(new AsymmetricGaussianPSF());
		}else {
			fitter = new LeastSquare(new IntegratedGaussianPSF());
		}
		LocalMaximum finder = new LocalMaximum(thresholdIntensity, 0, (int) windowSize);
		for (int i = 0; i < pixels.length; i ++) {
			pixels[i] = iPixels[i] - backgroundIntensity ;
		}
		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		cnt[frame] = 0;

		finder.processFrame(data, new LocalMaximum.CallBackFunctions() {
			double [] start = {0, 0, 0, 1.5, 1};

			@Override
			public boolean fit(RectangularDoubleImage img, int x, int y) {

				// System.out.println("Location " + (c++) +" : " + x + " - " + y + " - " + img.getValueAtCoordinate(x, y));
				start[0] = x; start[1] = y;  start[2] = img.getValueAtCoordinate(x, y) * 10;

				double [] results = fitter.fit(img, start);
				if (results != null ) {
					cnt[frame] ++;
					synchronized(positions) {
						positions.add(convertParameters(results, frame));
					}
				}
				return true;
			}
		});
	}

	static void processFrameWithDAO(TaggedImage img, int frame) throws JSONException {
		short [] iPixels = (short[]) img.pix;
		double [] pixels = new double[iPixels.length];
		DAOFitting fitter = new DAOFitting(new IntegratedGaussianPSF(false, false));
		LocalMaximum finder = new LocalMaximum(thresholdIntensity, 0, (int) windowSize);
		for (int i = 0; i < pixels.length; i ++) {
			pixels[i] = iPixels[i] - backgroundIntensity ;
		}
		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		cnt[frame] = 0;

		finder.processFrame(data, new LocalMaximum.CallBackFunctions() {
			double [] start = {0, 0, 0, 1.5, 1};

			@Override
			public boolean fit(RectangularDoubleImage img, int x, int y) {

				// System.out.println("Location " + (c++) +" : " + x + " - " + y + " - " + img.getValueAtCoordinate(x, y));
				start[0] = x; start[1] = y;  start[2] = img.getValueAtCoordinate(x, y) * 10;

				double [][] results = fitter.fit(img, start);
				if (results != null ) {
					cnt[frame] += results.length;
					synchronized(positions) {
						for (int i = 0; i < results.length; i++) {
							positions.add(convertParameters(results[i], frame));
						}
					}
				}
				return true;
			}
		});
	}

	public static void process(List<String> args) throws JSONException, IOException {
		System.out.println("Analyze data: " + args.get(0));

		positions = new ArrayList<double[]>();
		TaggedImageStorageMultipageTiff stackReader = new TaggedImageStorageMultipageTiff(args.get(0), false, null, false, false, false);
		int frames = stackReader.getSummaryMetadata().getInt("Frames");
		System.out.println("Total frames: " + frames);

		if (startingFrame < 0 ) {
			startingFrame = 0;
		}
		if (endingFrame >= frames || endingFrame < 0) {
			endingFrame = frames;
		}

		cnt = new int[frames];
		printParameters();
		IntStream.range((int)startingFrame, (int)endingFrame).parallel().forEach( f -> {
			TaggedImage img;
			synchronized (stackReader) {
				img= stackReader.getImage(0 /*channel*/, 0 /*slice*/, f /*frame*/, 0 /*position*/);
			}

			if (img != null ) {
				try {
					processFrame(img, f);
				} catch (JSONException e) {
					assert(false); //shouldn't happen
				}
				System.out.println("Processed frame " + f + ", Found " + cnt[f] + " molecules." );
			}
		});

		stackReader.close();

		String [] headers = {"frame","x", "y", "intensity", "sigma", "offset"};
		if (asymmetric && !multiPeak) {
			headers = new String [] {"frame","x", "y", "intensity", "sigmaX", "sigmaY", "offset"};
		}
		double [][] data = new double[headers.length][positions.size()];
		for (int i = 0; i < headers.length; i ++) {
			for (int j = 0; j < positions.size(); j++) {
				data[i][j] = positions.get(j)[i];
			}
		}
		OctaneDataFile raw = new OctaneDataFile(data, headers);

		System.out.println("Saving to file: " + args.get(1));
		ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args.get(1)));
        System.out.println("Output file: " + args.get(1));
        fo.writeObject(raw);
        fo.close();
	}

	static double[] convertParameters(double [] param, int f) {
		double [] r = new double[param.length + 1];
		r[0] = f + 1;
		r[1] = param[0] * pixelSize ; //x
		r[2] = param[1] * pixelSize ; //y
		r[3] = param[2];
		if (asymmetric && !multiPeak) {
			r[4] = param[3] * pixelSize;
			r[5] = param[4] * pixelSize;
			r[6] = param[5];
		} else {
			r[4] = param[3] * pixelSize;
			r[5] = param[4];
		}
		return r;
	}
}
