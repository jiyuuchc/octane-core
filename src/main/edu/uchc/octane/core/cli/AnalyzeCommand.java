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

import edu.uchc.octane.core.fitting.leastsquare.AsymmetricGaussianPSF;
import edu.uchc.octane.core.fitting.leastsquare.DAOFitting;
import edu.uchc.octane.core.fitting.leastsquare.IntegratedGaussianPSF;
import edu.uchc.octane.core.fitting.leastsquare.LeastSquare;
import edu.uchc.octane.core.frameanalysis.LocalMaximum;
import edu.uchc.octane.core.localizationdata.LocalizationDataset;
import edu.uchc.octane.core.pixelimage.RectangularDoubleImage;
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
	
	static LocalMaximum.CallBackFunctions getCallbackSimple(LeastSquare fitter, int frame) {
		return new LocalMaximum.CallBackFunctions() {
			double [] start = {0, 0, 0, 1.5, 1};

			@Override
			public boolean fit(RectangularDoubleImage img, int x, int y) {

				// System.out.println("Location " + (c++) +" : " + x + " - " + y + " - " + img.getValueAtCoordinate(x, y));
				start[0] = x; start[1] = y;  start[2] = img.getValueAtCoordinate(x, y) * 10;

				double [] result = fitter.fit(img, start);
				if (result != null ) {
					// reject bad fitting
					double bg = result[result.length -1];
					//double sigma = result[3];
					if (bg > 0.1) {
						synchronized(positions) {
							positions.add(convertParameters(result, frame));
						}
					}				    
					cnt[frame] ++;					
				}
				return true;
			}			
		};
	}

	static LocalMaximum.CallBackFunctions getCallbackCalibration(LeastSquare fitter, int frame) {
		return new LocalMaximum.CallBackFunctions() {
			double [] start = {0, 0, 0, 1.5, 1.5, 1};

			@Override
			public boolean fit(RectangularDoubleImage img, int x, int y) {

				start[0] = x; start[1] = y;  start[2] = img.getValueAtCoordinate(x, y) * 10;

				double [] result = fitter.fit(img, start);
				if (result != null ) {
					// reject bad fitting
					double bg = result[result.length -1];
					//double sigmax = result[3];
					//double sigmay = result[4];					
					if (bg > 0 ) {
						synchronized(positions) {
							positions.add(convertParameters(result, frame));
						}
					}				    
					cnt[frame] ++;					
				}
				return true;
			}
		};
	}

	static LocalMaximum.CallBackFunctions getCallbackDAO(DAOFitting fitter, int frame) {
		return new LocalMaximum.CallBackFunctions() {
			double [] start = {0, 0, 0, 1.5, 1};

			@Override
			public boolean fit(RectangularDoubleImage img, int x, int y) {

				// System.out.println("Location " + (c++) +" : " + x + " - " + y + " - " + img.getValueAtCoordinate(x, y));
				start[0] = x; start[1] = y;  start[2] = img.getValueAtCoordinate(x, y) * 10;

				double [] results = fitter.fit(img, start);
				while (results != null ) {
					cnt[frame] ++;

					double bg = results[results.length -1];
					double sigma = results[3];
					if (bg > 0.1 || sigma < 6) {
						synchronized(positions) {
							positions.add(convertParameters(results, frame));
						}
					}
					results = fitter.getNextResult();
				}
				return true;
			}
		};
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
			pixels[i] = iPixels[i]&0xffff - backgroundIntensity ;
		}
		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		cnt[frame] = 0;
		if (asymmetric) {
			finder.processFrame(data, getCallbackCalibration(fitter, frame));
		} else {
			finder.processFrame(data, getCallbackSimple(fitter, frame));
		}
	}

	static void processFrameWithDAO(TaggedImage img, int frame) throws JSONException {
		short [] iPixels = (short[]) img.pix;
		double [] pixels = new double[iPixels.length];
		DAOFitting fitter = new DAOFitting(new IntegratedGaussianPSF(false, false));
		LocalMaximum finder = new LocalMaximum(thresholdIntensity, 0, (int) windowSize);
		for (int i = 0; i < pixels.length; i ++) {
			double p = iPixels[i] >= 0 ? iPixels[i] : iPixels[i] + 65536.0;
			pixels[i] = p - backgroundIntensity ;
		}
		RectangularDoubleImage data = new RectangularDoubleImage(pixels, img.tags.getInt("Width"));
		cnt[frame] = 0;

		finder.processFrame(data, getCallbackDAO(fitter, frame));
	}

	public static void process(List<String> args) throws JSONException, IOException {
		System.out.println("Analyze data: " + args.get(0));

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
					if (! multiPeak) {
						processFrame(img, f);
					} else {
						processFrameWithDAO(img,f);
					}
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
		LocalizationDataset raw = new LocalizationDataset(data, headers);

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
