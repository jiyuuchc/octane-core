package edu.uchc.octane.core.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONObject;

public class MMTaggedTiff {

	private String directory_;
	private int lastFrameOpenedDataSet_ = -1;
	private JSONObject summaryMetadata_;
	//private String summaryMetadataString_;

	// Map of image labels to file
	private TreeMap<String, MultipageTiffReader> tiffReadersByLabel_;

	public MMTaggedTiff(String dir, boolean separateMDFile, boolean separateFilesForPositions) throws IOException {
//	      separateMetadataFile_ = separateMDFile;
//	      splitByXYPosition_ = separateFilesForPositions;

		directory_ = dir;
		tiffReadersByLabel_ = new TreeMap<String, MultipageTiffReader>(new ImageLabelComparator());
		openExistingDataSet();
	}

	void openExistingDataSet() throws IOException {
		MultipageTiffReader reader = null;
		File dir = new File(directory_);

		// int numRead = 0;
		for (File f : dir.listFiles()) {
			if (f.getName().endsWith(".tif") || f.getName().endsWith(".TIF")) {
				reader = loadFile(f);
			}
			// numRead++;
		}

		if (reader != null) {
			setSummaryMetadata(reader.getSummaryMetadata());
			// displayAndComments_ = reader.getDisplayAndComments();
		}
	}

	private MultipageTiffReader loadFile(File f) throws IOException {
		MultipageTiffReader reader = null;
		reader = new MultipageTiffReader(f);
		Set<String> labels = reader.getIndexKeys();
		for (String label : labels) {
			tiffReadersByLabel_.put(label, reader);
			int frameIndex = Integer.parseInt(label.split("_")[2]);
			lastFrameOpenedDataSet_ = Math.max(frameIndex, lastFrameOpenedDataSet_);
		}
		return reader;
	}

	public TaggedImage getImage(int channelIndex, int sliceIndex, int frameIndex, int positionIndex) {
		String label = MultipageTiffReader.generateLabel(channelIndex, sliceIndex, frameIndex, positionIndex);

//		TaggedImage image = writePendingImages_.get(label);
//		if (image != null) {
//			return image;
//		}

		MultipageTiffReader reader = tiffReadersByLabel_.get(label);
		if (reader == null) {
			return null;
		}
		return reader.readImage(label);
	}

	public JSONObject getSummaryMetadata() {
		return summaryMetadata_;
	}

	public void close() {
		for (MultipageTiffReader r : new HashSet<MultipageTiffReader>(tiffReadersByLabel_.values())) {
			try {
				r.close();
			} catch (IOException ex) {
//				ReportingUtils.logError(ex);
			}
		}
	}

	private void setSummaryMetadata(JSONObject md) {
		summaryMetadata_ = md;
		// summaryMetadataString_ = null;
		if (summaryMetadata_ != null) {
			// summaryMetadataString_ = md.toString();
			boolean slicesFirst = summaryMetadata_.optBoolean("SlicesFirst", true);
			boolean timeFirst = summaryMetadata_.optBoolean("TimeFirst", false);
			TreeMap<String, MultipageTiffReader> oldImageMap = tiffReadersByLabel_;
			tiffReadersByLabel_ = new TreeMap<String, MultipageTiffReader>(
					new ImageLabelComparator(slicesFirst, timeFirst));
			tiffReadersByLabel_.putAll(oldImageMap);
//			if (summaryMetadata_ != null && summaryMetadata_.length() > 0) {
//				processSummaryMD();
//			}
		}
	}
}
