///////////////////////////////////////////////////////////////////////////////
//Modified from micro-manager source 
// 	by Ji Yu jyu@uchc.edu
//-----------------------------------------------------------------------------
//
// AUTHOR:       Henry Pinkard, henry.pinkard@gmail.com, 2012
//
// COPYRIGHT:    University of California, San Francisco, 2012
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
package edu.uchc.octane.core.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public class MultipageTiffReader {

	private static final long BIGGEST_INT_BIT = (long) Math.pow(2,31);
	
	public static final char BITS_PER_SAMPLE = 258;
	public static final char IMAGE_DESCRIPTION = 270;
	public static final char STRIP_OFFSETS = 273;
	public static final char SAMPLES_PER_PIXEL = 277;
	public static final char STRIP_BYTE_COUNTS = 279;
	public static final char MM_METADATA = 51123;
	public static final int SUMMARY_MD_HEADER = 2355492;
	public static final int COMMENTS_OFFSET_HEADER = 99384722;
	public static final int COMMENTS_HEADER = 84720485;
	public static final int INDEX_MAP_OFFSET_HEADER = 54773648;
	public static final int INDEX_MAP_HEADER = 3453623;
	public static final int DISPLAY_SETTINGS_OFFSET_HEADER = 483765892;
	public static final int DISPLAY_SETTINGS_HEADER = 347834724;

	public static final int DISPLAY_SETTINGS_BYTES_PER_CHANNEL = 256;
	public static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();

	private ByteOrder byteOrder_;
	private File file_;
	private RandomAccessFile raFile_;
	private FileChannel fileChannel_;

	private JSONObject displayAndComments_;
	private JSONObject summaryMetadata_;
	private int byteDepth_ = 0;;
	private boolean rgb_;

	private HashMap<String, Long> indexMap_;

	//	/**
//	 * This constructor is used for a file that is currently being written
//	 * 
//	 * @param summaryMD - summary metadata in JSON format
//	 */
//	public MultipageTiffReader(JSONObject summaryMD) {
//		displayAndComments_ = new JSONObject();
//		summaryMetadata_ = summaryMD;
//		byteOrder_ = BYTE_ORDER;
//		getRGBAndByteDepth(summaryMD);
//		writingFinished_ = false;
//	}

	public void setIndexMap(HashMap<String, Long> indexMap) {
		indexMap_ = indexMap;
	}

	public void setFileChannel(FileChannel fc) {
		fileChannel_ = fc;
	}

	/**
	 * This constructor is used for opening datasets that have already been saved
	 * 
	 * @param file File to be opened
	 * @throws java.io.IOException
	 */
	public MultipageTiffReader(File file) throws IOException {
		displayAndComments_ = new JSONObject();
		file_ = file;
		try {
			createFileChannel(false);
		} catch (Exception ex) {
			throw new IOException("Can't successfully open file: " + file_.getName());
		}
		long firstIFD = readHeader();
		summaryMetadata_ = readSummaryMD();

		readIndexMap();
//FIXME
//		try {
//			displayAndComments_.put("Channels", readDisplaySettings());
//			displayAndComments_.put("Comments", readComments());
//		} catch (Exception ex) {
//			ReportingUtils.logError("Problem with JSON Representation of DisplayAndComments");
//		}

		if (summaryMetadata_ != null) {
			getRGBAndByteDepth(summaryMetadata_);
		}
	}

	public static boolean isMMMultipageTiff(String directory) throws IOException {
		File dir = new File(directory);
		File[] children = dir.listFiles();
		File testFile = null;
		for (File child : children) {
			if (child.isDirectory()) {
				File[] grandchildren = child.listFiles();
				for (File grandchild : grandchildren) {
					if (grandchild.getName().endsWith(".tif")) {
						testFile = grandchild;
						break;
					}
				}
			} else if (child.getName().endsWith(".tif") || child.getName().endsWith(".TIF")) {
				testFile = child;
				break;
			}
		}
		if (testFile == null) {
			throw new IOException("Unexpected file structure: is this an MM dataset?");
		}
		RandomAccessFile ra;
		try {
			ra = new RandomAccessFile(testFile, "r");
		} catch (FileNotFoundException ex) {
			// ReportingUtils.logError(ex);
			return false;
		}
		FileChannel channel = ra.getChannel();
		ByteBuffer tiffHeader = ByteBuffer.allocate(36);
		ByteOrder bo;
		channel.read(tiffHeader, 0);
		char zeroOne = tiffHeader.getChar(0);
		if (zeroOne == 0x4949) {
			bo = ByteOrder.LITTLE_ENDIAN;
		} else if (zeroOne == 0x4d4d) {
			bo = ByteOrder.BIG_ENDIAN;
		} else {
			ra.close();
			throw new IOException("Error reading Tiff header");
		}
		tiffHeader.order(bo);
		int summaryMDHeader = tiffHeader.getInt(32);
		channel.close();
		ra.close();
		return summaryMDHeader == SUMMARY_MD_HEADER;
	}

	private void getRGBAndByteDepth(JSONObject md) {
		String pixelType = null;
		if (md != null ) {
			try {
				pixelType = md.getString("pixelType"); 
			} catch (JSONException ex) {
//				try {
//					int ijType = md.getInt("IJType");
//					if (ijType == ImagePlus.GRAY8) {
//						pixelType = "GRAY8";
//					} else if (ijType == ImagePlus.GRAY16) {
//						pixelType = "GRAY16";
//					} else if (ijType == ImagePlus.GRAY32) {
//						pixelType = "GRAY32";
//					} else if (ijType == ImagePlus.COLOR_RGB) {
//						pixelType = "RGB32";
//					} 
//				} catch (JSONException e2) {
//				}
			}
		}
		if (pixelType != null) {
			rgb_ = pixelType.startsWith("RGB");

			if (pixelType.equals("RGB32") || pixelType.equals("GRAY8")) {
				byteDepth_ = 1;
			} else {
				byteDepth_ = 2;
			}
		}
	}

	public JSONObject getSummaryMetadata() {
		return summaryMetadata_;
	}

	public TaggedImage readImage(String label) {
		if (indexMap_.containsKey(label)) {
			if (fileChannel_ == null) {
				return null;
			}
			try {
				long byteOffset = indexMap_.get(label);

				IFDData data = readIFD(byteOffset);
				return readTaggedImage(data);
			} catch (IOException ex) {
				return null;
			}

		} else {
			// label not in map--either writer hasnt finished writing it
			return null;
		}
	}

	public Set<String> getIndexKeys() {
		if (indexMap_ == null)
			return null;
		return indexMap_.keySet();
	}

	private JSONObject readSummaryMD() {
		try {
			ByteBuffer mdInfo = ByteBuffer.allocate(8).order(byteOrder_);
			fileChannel_.read(mdInfo, 32);
			int header = mdInfo.getInt(0);
			int length = mdInfo.getInt(4);

			if (header != SUMMARY_MD_HEADER) {
				return null;
			}

			ByteBuffer mdBuffer = ByteBuffer.allocate(length).order(byteOrder_);
			fileChannel_.read(mdBuffer, 40);
			JSONObject summaryMD = new JSONObject(getString(mdBuffer));

			// Summary MD written start of acquisition and never changed, this code makes
			// sure acquisition comment
			// field is current
//			if (displayAndComments_ != null && displayAndComments_.has("Comments")
//					&& displayAndComments_.getJSONObject("Comments").has("Summary")) {
//				summaryMD.put("Comment", displayAndComments_.getJSONObject("Comments").getString("Summary"));
//			}
			return summaryMD;
		} catch (IOException ex) {
			return null;
		} catch (JSONException ex) {
			return null;
		}
	}

	private ByteBuffer readIntoBuffer(long position, int length) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(length).order(byteOrder_);
		fileChannel_.read(buffer, position);
		return buffer;
	}

	private long readOffsetHeaderAndOffset(int offsetHeaderVal, int startOffset) throws IOException {
		ByteBuffer buffer1 = readIntoBuffer(startOffset, 8);
		int offsetHeader = buffer1.getInt(0);
		if (offsetHeader != offsetHeaderVal) {
			throw new IOException(
					"Offset header incorrect, expected: " + offsetHeaderVal + "   found: " + offsetHeader);
		}
		return unsignInt(buffer1.getInt(4));
	}

	private void readIndexMap() throws IOException {
		long offset = readOffsetHeaderAndOffset(INDEX_MAP_OFFSET_HEADER, 8);
		ByteBuffer header = readIntoBuffer(offset, 8);
		if (header.getInt(0) != INDEX_MAP_HEADER) {
			throw new IOException();
		}
		int numMappings = header.getInt(4);
		indexMap_ = new HashMap<String, Long>();
		ByteBuffer mapBuffer = readIntoBuffer(offset + 8, 20 * numMappings);
		for (int i = 0; i < numMappings; i++) {
			int channel = mapBuffer.getInt(i * 20);
			int slice = mapBuffer.getInt(i * 20 + 4);
			int frame = mapBuffer.getInt(i * 20 + 8);
			int position = mapBuffer.getInt(i * 20 + 12);
			long imageOffset = unsignInt(mapBuffer.getInt(i * 20 + 16));
			if (imageOffset == 0) {
				break; // end of index map reached
			}
			// If a duplicate label is read, forget about the previous one
			// if data has been intentionally overwritten, this gives the most current
			// version
			indexMap_.put(generateLabel(channel, slice, frame, position), imageOffset);
		}
	}

	private IFDData readIFD(long byteOffset) throws IOException {
		ByteBuffer buff = readIntoBuffer(byteOffset, 2);
		int numEntries = buff.getChar(0);

		ByteBuffer entries = readIntoBuffer(byteOffset + 2, numEntries * 12 + 4).order(byteOrder_);
		IFDData data = new IFDData();
		for (int i = 0; i < numEntries; i++) {
			IFDEntry entry = readDirectoryEntry(i * 12, entries);
			if (entry.tag == MM_METADATA) {
				data.mdOffset = entry.value;
				data.mdLength = entry.count;
			} else if (entry.tag == STRIP_OFFSETS) {
				data.pixelOffset = entry.value;
			} else if (entry.tag == STRIP_BYTE_COUNTS) {
				data.bytesPerImage = entry.value;
			}
		}
		data.nextIFD = unsignInt(entries.getInt(numEntries * 12));
		data.nextIFDOffsetLocation = byteOffset + 2 + numEntries * 12;
		return data;
	}

	private String getString(ByteBuffer buffer) {
		try {
			return new String(buffer.array(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
//			ReportingUtils.logError(ex);
			return "";
		}
	}

	private TaggedImage readTaggedImage(IFDData data) throws IOException {
		ByteBuffer pixelBuffer = ByteBuffer.allocate((int) data.bytesPerImage).order(byteOrder_);
		ByteBuffer mdBuffer = ByteBuffer.allocate((int) data.mdLength).order(byteOrder_);
		fileChannel_.read(pixelBuffer, data.pixelOffset);
		fileChannel_.read(mdBuffer, data.mdOffset);
		JSONObject md = new JSONObject();
		try {
			md = new JSONObject(getString(mdBuffer));
		} catch (JSONException ex) {
//			ReportingUtils.logError("Error reading image metadata from file");
		}

		if (byteDepth_ == 0) {
			getRGBAndByteDepth(md);
		}

		if (rgb_) {
			if (byteDepth_ == 1) {
				// This gets a little unpleasant. Our source pixels array is in
				// BGR format (see MultipageTiffWriter.getPixelBuffer()), and we
				// need to transform it into RGBA format -- swapping the R and B
				// components and inserting a blank alpha component.
				byte[] pixels = new byte[(int) (4 * data.bytesPerImage / 3)];
				byte[] source = pixelBuffer.array();
				int numPixels = 0;
				int numComponents = 0;
				for (int i = 0; i < source.length; ++i) {
					pixels[i + numPixels] = source[i - (2 * (i % 3)) + 2];
					numComponents++;
					if (numComponents == 3) {
						// Insert a blank alpha byte to cap off the pixel.
						pixels[i + numPixels + 1] = 0;
						numPixels++;
						numComponents = 0;
					}
				}
				return new TaggedImage(pixels, md);
			} else {
				short[] pixels = new short[(int) (2 * (data.bytesPerImage / 3))];
				int i = 0;
				while (i < pixels.length) {
					pixels[i] = pixelBuffer.getShort(2 * ((i / 4) * 3 + (i % 4)));
					i++;
					if ((i + 1) % 4 == 0) {
						pixels[i] = 0;
						i++;
					}
				}
				return new TaggedImage(pixels, md);
			}
		} else {
			if (byteDepth_ == 1) {
				return new TaggedImage(pixelBuffer.array(), md);
			} else {
				short[] pix = new short[pixelBuffer.capacity() / 2];
				for (int i = 0; i < pix.length; i++) {
					pix[i] = pixelBuffer.getShort(i * 2);
				}
				return new TaggedImage(pix, md);
			}
		}
	}

	private IFDEntry readDirectoryEntry(int offset, ByteBuffer buffer) throws IOException {
		char tag = buffer.getChar(offset);
		char type = buffer.getChar(offset + 2);
		long count = unsignInt(buffer.getInt(offset + 4));
		long value;
		if (type == 3 && count == 1) {
			value = buffer.getChar(offset + 8);
		} else {
			value = unsignInt(buffer.getInt(offset + 8));
		}
		return (new IFDEntry(tag, type, count, value));
	}

	// returns byteoffset of first IFD
	private long readHeader() throws IOException {
		ByteBuffer tiffHeader = ByteBuffer.allocate(8);
		fileChannel_.read(tiffHeader, 0);
		char zeroOne = tiffHeader.getChar(0);
		if (zeroOne == 0x4949) {
			byteOrder_ = ByteOrder.LITTLE_ENDIAN;
		} else if (zeroOne == 0x4d4d) {
			byteOrder_ = ByteOrder.BIG_ENDIAN;
		} else {
			throw new IOException("Error reading Tiff header");
		}
		tiffHeader.order(byteOrder_);
		short twoThree = tiffHeader.getShort(2);
		if (twoThree != 42) {
			throw new IOException("Tiff identifier code incorrect");
		}
		return unsignInt(tiffHeader.getInt(4));
	}

//	private byte[] getBytesFromString(String s) {
//		try {
//			return s.getBytes("UTF-8");
//		} catch (UnsupportedEncodingException ex) {
//			//ReportingUtils.logError("Error encoding String to bytes");
//			return null;
//		}
//	}

	private void createFileChannel(boolean isReadWrite) throws FileNotFoundException, IOException {
		raFile_ = new RandomAccessFile(file_, isReadWrite ? "rw" : "r");
		fileChannel_ = raFile_.getChannel();
	}

	public void close() throws IOException {
		if (fileChannel_ != null) {
			fileChannel_.close();
			fileChannel_ = null;
		}
		if (raFile_ != null) {
			raFile_.close();
			raFile_ = null;
		}
	}

	private long unsignInt(int i) {
		long val = Integer.MAX_VALUE & i;
		if (i < 0) {
			val += BIGGEST_INT_BIT;
		}
		return val;
	}

	public static String generateLabel(int channel, int slice, int frame, int position) {
		final DecimalFormat coreIntegerFormat_ = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		coreIntegerFormat_.applyPattern("0");

		return coreIntegerFormat_.format(channel) + "_" + coreIntegerFormat_.format(slice) + "_"
				+ coreIntegerFormat_.format(frame) + "_" + coreIntegerFormat_.format(position);
	}

	private class IFDData {
		public long pixelOffset;
		public long bytesPerImage;
		public long mdOffset;
		public long mdLength;
		public long nextIFD;
		public long nextIFDOffsetLocation;

		public IFDData() {
		}
	}

	private class IFDEntry {
		public char tag, type;
		public long count, value;

		public IFDEntry(char tg, char typ, long cnt, long val) {
			tag = tg;
			type = typ;
			count = cnt;
			value = val;
		}
	}

}
