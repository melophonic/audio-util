package org.melophonic.audio.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous audio utility methods for the javasound API
 *
 */
public class AudioUtil {

	final static Logger log = LoggerFactory.getLogger(AudioUtil.class);
	
	public static final float DEFAULT_FLOAT_COMPARISON_THRESHOLD = 1E-9F;

	/**
	 * 
	 * @param file
	 * @return the duration in seconds of the input file
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public static double getDurationInSeconds(File file) throws UnsupportedAudioFileException, IOException {
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
		AudioFormat format = audioInputStream.getFormat();
		return getDurationInSeconds(format, file.length());
	}
	
	/**
	 * 
	 * @param format
	 * @param audioFileLengthBytes
	 * @return the duration in seconds of an audio file in the specified format whose size is audioFileLengthBytes
	 */
	public static double getDurationInSeconds(AudioFormat format, long audioFileLengthBytes) {
		int frameSize = format.getFrameSize();
		double frameRate = format.getFrameRate();
		return (audioFileLengthBytes / (frameSize * frameRate));
	}
	
	
	public static AudioFileFormat.Type[] getSupportedAudioFileFormatTypes() {
		List<AudioFileFormat.Type> types = new ArrayList<>(Arrays.asList(AudioSystem.getAudioFileTypes()));
		types.add(new Type("FLAC", "flac")); // TODO:  this is a hack because the jflac doesn't register an AudioFileWriter
		return types.toArray(new AudioFileFormat.Type[types.size()]);
	}
	
	public static FileFilter getSupportedAudioFileFilter() {
		List<String> exts = new ArrayList<>();
		for (AudioFileFormat.Type type : getSupportedAudioFileFormatTypes()) exts.add(String.format(".%s", type.getExtension()));
		return new SuffixFileFilter(exts);
	}

	/**
	 * Trying to get an audio file type for the passed extension. This works by
	 * examining all available file types. For each type, if the extension this
	 * type promises to handle matches the extension we are trying to find a
	 * type for, this type is returned. If no appropriate type is found, null is
	 * returned.
	 */
	public static AudioFileFormat.Type getAudioFileFormatType(String strExtension) {
		AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
		for (int i = 0; i < aTypes.length; i++) {
			if (aTypes[i].getExtension().equals(strExtension)) {
				return aTypes[i];
			}
		}
		return null;
	}

	public static List<Mixer.Info> getAvailableMixers() {
		return getAvailableMixers(null);
	}

	/**
	 * Get list of available Mixers
	 * 
	 * @param sourceTarget if not null, returns only mixers supporting SourceDataLine (true) or TargetDataLine (false)
	 * @return
	 */
	public static List<Mixer.Info> getAvailableMixers(Boolean sourceTarget) {
		ArrayList<Mixer.Info> mixers = new ArrayList<>();
		if (sourceTarget != null) {
			for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				Line.Info lineInfo = new Line.Info(sourceTarget ? SourceDataLine.class : TargetDataLine.class);
				if (mixer.isLineSupported(lineInfo)) mixers.add(mixerInfo);				
			}
			
		} else {
			mixers.addAll(Arrays.asList(AudioSystem.getMixerInfo()));
		}
		return mixers;
	}

	public static Mixer.Info getMixerInfo(String mixerName) {
		for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			if (mixerInfo.getName().equals(mixerName)) return mixerInfo;
		}
		return null;
	}

	
	/**
	 * Asking for a line is a rather tricky thing. We have to construct an
	 * Info object that specifies the desired properties for the line.
	 * First, we have to say which kind of line we want. The possibilities
	 * are: SourceDataLine (for playback), Clip (for repeated playback) and
	 * TargetDataLine (for recording). Here, we want to do normal capture,
	 * so we ask for a TargetDataLine. Then, we have to pass an AudioFormat
	 * object, so that the Line knows which format the data passed to it
	 * will have. Furthermore, we can give Java Sound a hint about how big
	 * the internal buffer for the line should be. This isn't used here,
	 * signaling that we don't care about the exact size. Java Sound will
	 * use some default value for the buffer size.
	 * 
	 * Adapted from: http://www.jsresources.org/examples/AudioCommon.java.html
	 * 
	 **/
	public static TargetDataLine getTargetDataLine(String mixerName, AudioFormat audioFormat, int bufferSize) {
		TargetDataLine targetDataLine = null;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat, bufferSize);
		try {
			if (mixerName != null) {
				Mixer.Info mixerInfo = getMixerInfo(mixerName);
				if (mixerInfo == null) {
					log.info("mixer not found: " + mixerName);
					return null;
				}
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				targetDataLine = (TargetDataLine) mixer.getLine(info);
			} else {

				log.debug("using default mixer");
				targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
			}

			log.debug("opening line...");
			targetDataLine.open(audioFormat, bufferSize);
			log.debug("opened line");
		} catch (LineUnavailableException e) {
			log.debug("getTargetDataLine", e);
		} catch (Exception e) {
			log.debug("getTargetDataLine", e);
		}

		log.debug("returning line: " + targetDataLine);
		return targetDataLine;
	}

	public static boolean isPcm(AudioFormat.Encoding encoding) {
		return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
	}
	
	public static String getResourceName(URI uri) {
		return uri.toString().replaceFirst(".*/([^/?]+).*", "$1");	
	}
	
	public static boolean equals(float f1, float f2) {
		return equals(f1, f2, DEFAULT_FLOAT_COMPARISON_THRESHOLD);
	}

	public static boolean equals(float f1, float f2, float threshold) {
		return (Math.abs(f1 - f2) < threshold);
	}
	
}

