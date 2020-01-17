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

import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.fitting.leastsquare.AsymmetricGaussianPSF;
import edu.uchc.octane.core.fitting.leastsquare.DAOFitting;
import edu.uchc.octane.core.fitting.leastsquare.IntegratedGaussianPSF;
import edu.uchc.octane.core.fitting.leastsquare.LeastSquare;
import edu.uchc.octane.core.fitting.leastsquare.PSFFittingFunction;
import edu.uchc.octane.core.frameanalysis.LocalMaximum;
import edu.uchc.octane.core.localizationdata.LocalizationDataset;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
import edu.uchc.octane.core.pixelimage.RectangularImage;
import edu.uchc.octane.core.utils.MMTaggedTiff;
import edu.uchc.octane.core.utils.TaggedImage;

public class AnalyzeCommand {

	static Options options;
	static long windowSize = 3;
	static long thresholdIntensity = 300;
	static long backgroundIntensity = 1600;
	static long startingFrame = 0;
	static long endingFrame = -1;
	static double pixelSize = 160;
	static boolean multiPeak = false;
	static boolean asymmetric = false;

	static int [] cnt;
	static List<double[]> positions;
	static String [] headers;

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
	
	public static void process(List<String> args) throws JSONException, IOException {
		System.out.println("Analyze data: " + args.get(0));

		PSFFittingFunction psf = asymmetric ? new AsymmetricGaussianPSF() : new IntegratedGaussianPSF();
		headers = psf.getHeaders();

		positions = new ArrayList<double[]>();
		MMTaggedTiff stackReader = new MMTaggedTiff(args.get(0), false, false);
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

		double [][] data = new double[headers.length][positions.size()];
		for (int i = 0; i < headers.length; i ++) {
			for (int j = 0; j < positions.size(); j++) {
				data[i][j] = positions.get(j)[i];
			}
		}
		LocalizationDataset raw = new LocalizationDataset(data, headers);

		System.out.println("Saving to file: " + args.get(1));
		ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(args.get(1)));
		System.out.println("Output file: " + args.get(1));
		fo.writeObject(raw);
		fo.close();
	}
	
	static void processFrame(TaggedImage img, int frame) throws JSONException {
		short [] iPixels = (short[]) img.pix;
		double [] pixels = new double[iPixels.length];

		LocalMaximum finder = new LocalMaximum(thresholdIntensity, 0, (int) windowSize);
		for (int i = 0; i < pixels.length; i ++) {
			pixels[i] = iPixels[i]&0xffff - backgroundIntensity ;
		}
		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		cnt[frame] = 0;

		finder.processFrame(data, new LocalMaximum.CallBackFunctions() {
			PSFFittingFunction psf = asymmetric ? new AsymmetricGaussianPSF() : new IntegratedGaussianPSF();
			Fitter fitter = multiPeak ? new DAOFitting(psf) :  new LeastSquare(psf);

			@Override
			public boolean fit(RectangularImage ROI, int x, int y) {

				double [] results = fitter.fit(ROI, null);
				while (results != null ){
					cnt[frame]++;
					synchronized(positions) {
						positions.add(convertParameters(results, frame));
					}
					if (multiPeak) {
						results = ((DAOFitting) fitter).getNextResult();
					} else {
						results = null;
					}
				}

				return true;
			}
		});
	}

	static double[] convertParameters(double [] param, int f) {
		double [] r = new double[param.length + 1];

		// first column is frame number (1-based)
		r[0] = f + 1;

		for (int i = 0; i < headers.length; i++) {
			String s = headers[i]; 
			if ( s.equals("x") || s.equals("y") || s.equals("z") || s.startsWith("sigma")) {
				r[i+1] = param[i] * pixelSize;
			} else {
				r[i+1] = param[i];
			}
		}

		return r;
	}
}
