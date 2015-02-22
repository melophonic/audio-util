package org.melophonic.audio.util;

import static org.melophonic.audio.util.AudioUtil.getAudioFileFormatType;
import static org.melophonic.audio.util.AudioUtil.getSupportedAudioFileFormatTypes;
import gnu.getopt.Getopt;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * A utility for converting an audio file from one AudioFormat and Encoding to
 * another, using the <code>javax.sound.sampled</code> API. The input audio is 
 * converted to PCM if necessary and then re-encoded using the parameter values
 * in the input <code>Parameters</code> object or passed as arguments to the 
 * main method. 
 * 
 * This class is adapted from http://www.jsresources.org/examples/AudioConverter.html
 *
 */
public class AudioConverter {

	final static Logger log = LoggerFactory.getLogger(AudioConverter.class);
	
	
	public static class Parameters implements Cloneable {

		/**
		 * The number of channels the audio data should be converted to. The
		 * value is initialized to -1. This is used to represent the condition
		 * that conversion of channels is not requested on the command line.
		 */
		int channels = AudioSystem.NOT_SPECIFIED;

		/**
		 * The sample size in bits the audio data should be converted to.
		 */
		int sampleSizeInBits = AudioSystem.NOT_SPECIFIED;

		/**
		 * The encoding the audio data should be converted to.
		 */
		AudioFormat.Encoding encoding = null;

		/**
		 * The sample rate the audio data should be converted to.
		 */
		float sampleRate = AudioSystem.NOT_SPECIFIED;

		/**
		 * The file type that should be used to write the audio data.
		 */
		AudioFileFormat.Type fileType = null;

		/**
		 * The endianess the audio data should be converted to. This is only
		 * used if bIsEndianessDesired is true.
		 */
		boolean bigEndian = false;

		/**
		 * Whether conversion of endianess is desired. This flag is necessary
		 * because the boolean variable bDesiredBigEndian has no 'unspecified'
		 * value to signal that endianess conversion is not desired.
		 */
		boolean endianessDesired = false;
		
		
		public Parameters() {
			super();
		}

		
		public Parameters(AudioFormat.Encoding encoding, AudioFileFormat.Type fileType, float sampleRate, int sampleSizeInBits) {
			super();
			this.encoding = encoding;
			this.fileType = fileType;
			this.sampleSizeInBits = sampleSizeInBits;
			this.sampleRate = sampleRate;
		}




		public void setDefaults(AudioFormat format) {
			if (encoding == null) {
				encoding = format.getEncoding();
			}
			if (sampleRate == AudioSystem.NOT_SPECIFIED) {
				sampleRate = format.getSampleRate();
			}
			if (sampleSizeInBits == AudioSystem.NOT_SPECIFIED) {
				sampleSizeInBits = format.getSampleSizeInBits();
			}
			if (channels == AudioSystem.NOT_SPECIFIED) {
				channels = format.getChannels();
			}
			if (!endianessDesired) {
				bigEndian = format.isBigEndian();
			}			
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
		    return super.clone();
		}

		public int getChannels() {
			return channels;
		}

		public void setChannels(int channels) {
			this.channels = channels;
		}

		public int getSampleSizeInBits() {
			return sampleSizeInBits;
		}

		public void setSampleSizeInBits(int sampleSizeInBits) {
			this.sampleSizeInBits = sampleSizeInBits;
		}

		public AudioFormat.Encoding getEncoding() {
			return encoding;
		}

		public void setEncoding(AudioFormat.Encoding encoding) {
			this.encoding = encoding;
		}

		public float getSampleRate() {
			return sampleRate;
		}

		public void setSampleRate(float sampleRate) {
			this.sampleRate = sampleRate;
		}

		public AudioFileFormat.Type getFileType() {
			return fileType;
		}

		public void setFileType(AudioFileFormat.Type fileType) {
			this.fileType = fileType;
		}

		public boolean isBigEndian() {
			return bigEndian;
		}

		public void setBigEndian(boolean bigEndian) {
			this.bigEndian = bigEndian;
		}

		public boolean isEndianessDesired() {
			return endianessDesired;
		}

		public void setEndianessDesired(boolean endianessDesired) {
			this.endianessDesired = endianessDesired;
		}
		
	}
	
	

	public static int convert(File inputFile, File outputFile, Parameters parameters) throws Exception {
		AudioFileFormat inputFileFormat = AudioSystem.getAudioFileFormat(inputFile);
		AudioFileFormat.Type defaultFileType = inputFileFormat.getType();
		AudioInputStream stream = AudioSystem.getAudioInputStream(inputFile);

		AudioFormat format = stream.getFormat();
		log.debug("source format: " + format);
		//AudioFormat targetFormat = null;

		// clone before populating with defaults so Parameters can be reused
		Parameters params = (Parameters) parameters.clone();
		params.setDefaults(format);

		/*
		 * Step 1: convert to PCM, if necessary.
		 */
		if (!AudioUtil.isPcm(format.getEncoding())) {
			log.debug("converting to PCM...");
			/*
			 * The following is a heuristics: normally (but not always), 8 bit
			 * audio data are unsigned, while 16 bit data are signed.
			 */
			AudioFormat.Encoding targetEncoding = (format.getSampleSizeInBits() == 8) ? AudioFormat.Encoding.PCM_UNSIGNED : AudioFormat.Encoding.PCM_SIGNED;
			stream = convertEncoding(targetEncoding, stream);
			log.debug("stream: " + stream);
			log.debug("format: " + stream.getFormat());

			/*
			 * Here, we handle a special case: some compressed formats do not
			 * state a sample size (but AudioSystem.NOT_SPECIFIED) because its
			 * unknown how long the samples are after decoding. If no sample
			 * size has been requested with a command line option, In this case,
			 * nDesiredSampleSizeInBits still has the value
			 * AudioSystem.NOT_SPECIFIED despite the filling with default values
			 * above.
			 */
			if (params.sampleSizeInBits == AudioSystem.NOT_SPECIFIED) {
				params.sampleSizeInBits = format.getSampleSizeInBits();
			}
		}

		/*
		 * Step 2: convert number of channels, if necessary.
		 */
		if (stream.getFormat().getChannels() != params.channels) {
			log.debug("converting channels...");
			stream = convertChannels(params.channels, stream);
			log.debug("stream: " + stream);
			log.debug("format: " + stream.getFormat());
		}

		/*
		 * Step 3: convert sample size and endianess, if necessary.
		 */
		boolean bDoConvertSampleSize = (stream.getFormat().getSampleSizeInBits() != params.sampleSizeInBits);
		boolean bDoConvertEndianess = (stream.getFormat().isBigEndian() != params.bigEndian);
		if (bDoConvertSampleSize || bDoConvertEndianess) {
			log.debug("converting sample size and endianess...");
			stream = convertSampleSizeAndEndianess(params.sampleSizeInBits, params.bigEndian, stream);
			log.debug("stream: " + stream);
			log.debug("format: " + stream.getFormat());
		}

		/*
		 * Step 4: convert sample rate, if necessary.
		 */
		if (!AudioUtil.equals(stream.getFormat().getSampleRate(), params.sampleRate)) {
			log.debug("converting sample rate...");
			stream = convertSampleRate(params.sampleRate, stream);
			log.debug("stream: " + stream);
			log.debug("format: " + stream.getFormat());
		}

		/*
		 * Step 5: convert to non-PCM encoding, if necessary.
		 */
		if (!stream.getFormat().getEncoding().equals(params.encoding)) {
			log.debug("converting to " + params.encoding + "...");
			stream = convertEncoding(params.encoding, stream);
			log.debug("format: " + stream.getFormat());
		}

		/*
		 * Since we now know that we are dealing with PCM, we know that the
		 * frame rate is the same as the sample rate.
		 */
		// float fTargetFrameRate = fTargetSampleRate;

		// /* Here, we are constructing the desired format of the
		// audio data (as the result of the conversion should be).
		// We take over all values besides the sample/frame rate.
		// */

		/*
		 * And finally, we are trying to write the converted audio data to a new
		 * file.
		 */
		int nWrittenBytes = 0;
		AudioFileFormat.Type targetFileType = (params.fileType != null) ? params.fileType : defaultFileType;
		nWrittenBytes = AudioSystem.write(stream, targetFileType, outputFile);
		log.debug("Written bytes: " + nWrittenBytes);
		return nWrittenBytes;
		
	}

	public static AudioInputStream convertEncoding(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
		return AudioSystem.getAudioInputStream(targetEncoding, sourceStream);
	}

	public static AudioInputStream convertChannels(int nChannels, AudioInputStream sourceStream) {
		AudioFormat sourceFormat = sourceStream.getFormat();
		AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), nChannels,
				calculateFrameSize(nChannels, sourceFormat.getSampleSizeInBits()), sourceFormat.getFrameRate(), sourceFormat.isBigEndian());
		return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
	}

	public static AudioInputStream convertSampleSizeAndEndianess(int nSampleSizeInBits, boolean bBigEndian, AudioInputStream sourceStream) {
		AudioFormat sourceFormat = sourceStream.getFormat();
		AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(),
				calculateFrameSize(sourceFormat.getChannels(), nSampleSizeInBits), sourceFormat.getFrameRate(), bBigEndian);
		return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
	}

	public static AudioInputStream convertSampleRate(float fSampleRate, AudioInputStream sourceStream) {
		AudioFormat sourceFormat = sourceStream.getFormat();
		AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), fSampleRate, sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(),
				sourceFormat.getFrameSize(), fSampleRate, sourceFormat.isBigEndian());
		return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
	}

	public static int calculateFrameSize(int nChannels, int nSampleSizeInBits) {
		return ((nSampleSizeInBits + 7) / 8) * nChannels;
	}



	private static void printUsageAndExit() {
		log.info("AudioConverter: usage:");
		log.info("\tjava AudioConverter -h");
		log.info("\tjava AudioConverter -l");
		log.info("\tjava AudioConverter");
		log.info("\t\t[-c <channels>]");
		log.info("\t\t[-s <sample_size_in_bits>]");
		log.info("\t\t[-e <encoding>]");
		log.info("\t\t[-f <sample_rate>]");
		log.info("\t\t[-t <file_type>]");
		log.info("\t\t[-B|-L]");
		log.info("\t\t<sourcefile> <targetfile>");
		System.exit(1);
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		Parameters params = new Parameters();
		try {
			/*
			 * Parsing of command-line options takes place...
			 */
			Getopt g = new Getopt("AudioConverter", args, "hlc:s:e:f:t:BLD");
			int c;
			while ((c = g.getopt()) != -1) {
				switch (c) {
				case 'h':
					printUsageAndExit();

				case 'l':
					log.info("Supported AudioFileFormatTypes:");
					for (AudioFileFormat.Type type : getSupportedAudioFileFormatTypes()) {
						log.info("\t" + type.toString());
					}
					System.exit(0);

				case 'c':
					params.channels = Integer.parseInt(g.getOptarg());
					break;

				case 's':
					params.sampleSizeInBits = Integer.parseInt(g.getOptarg());
					break;

				case 'e':
					String strEncodingName = g.getOptarg();
					params.encoding = new AudioFormat.Encoding(strEncodingName);
					break;

				case 'f':
					params.sampleRate = Float.parseFloat(g.getOptarg());
					break;

				case 't':
					String strExtension = g.getOptarg();
					params.fileType = getAudioFileFormatType(strExtension);
					if (params.fileType == null) {
						log.info("Unknown target file type. Check with 'AudioConverter -l'.");
						System.exit(1);
					}
					break;

				case 'B':
					params.bigEndian = true;
					params.endianessDesired = true;
					break;

				case 'L':
					params.bigEndian = true;
					params.endianessDesired = true;
					break;

				case '?':
					printUsageAndExit();

				default:
					log.info("getopt() returned " + c);
					break;
				}
			}

			/*
			 * We make shure that there are only two more arguments, which we take
			 * as the input and output filenames.
			 */
			if (args.length - g.getOptind() < 2) {
				printUsageAndExit();
			}

			File inputFile = new File(args[g.getOptind()]);
			File outputFile = new File(args[g.getOptind() + 1]);

			convert(inputFile, outputFile, params);
		} catch (Exception e) {
			log.error("Error converting audio", e);
		}
		
	}


}
