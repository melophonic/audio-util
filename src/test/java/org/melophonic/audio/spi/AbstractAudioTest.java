package org.melophonic.audio.spi;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.melophonic.audio.util.AudioConverter;
import org.melophonic.audio.util.AudioConverter.Parameters;
import org.melophonic.audio.util.AudioUtil;
import org.melophonic.audio.util.AudioUtilTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAudioTest {

	final static URL baseUrl = AudioUtilTest.class.getResource("/audio");
	
	final static File outputPath = new File("./target/normalized");
	
	protected final Logger log = LoggerFactory.getLogger(getClass());

	public static <T> List<AudioFileSet<T>> getAudioFileSets(String...extensions) throws Exception {
		return getAudioFileSets(null, null, extensions);
	}	
	
	public static <T> List<AudioFileSet<T>> getAudioFileSets(AudioConverter.Parameters normalizeParams, String...extensions) throws Exception {
		return getAudioFileSets(normalizeParams, null, extensions);
	}
	
	public static <T> List<AudioFileSet<T>> getAudioFileSets(AudioConverter.Parameters normalizeParams, T initialValue, String...extensions) throws Exception {
		File basePath = new File(baseUrl.toURI());
		boolean normalize = normalizeParams != null;
		
		if (normalize) {
			if (!outputPath.exists()) outputPath.mkdirs();
			else FileUtils.cleanDirectory(outputPath);			
		}
		
		FileFilter filter = extensions == null || extensions.length == 0 ? AudioUtil.getSupportedAudioFileFilter() : new SuffixFileFilter(extensions);
		
		List<AudioFileSet<T>> audioFileSets = new ArrayList<>();
		for (File baseSetPath : basePath.listFiles((FileFilter) FileFilterUtils.directoryFileFilter())) {
			Function<File, File> processor = null;

			if (normalize) {
				File setPath = new File(outputPath, baseSetPath.getName());
				setPath.mkdirs();
				processor = new Normalizer(setPath, normalizeParams);
			}
		
			AudioFileSet<T> audioFileSet = new AudioFileSet<T>(baseSetPath, initialValue, filter, processor);
			
			System.out.format("%s: %s", audioFileSet.id, audioFileSet.size()).println();
			audioFileSets.add(audioFileSet);
		}
		return audioFileSets;
	}
	
	public static class Normalizer implements Function<File, File> {
		
		final File path;
		final AudioConverter.Parameters normalizeParams;
		
		public Normalizer(File path, Parameters normalizeParams) {
			super();
			this.path = path;
			this.normalizeParams = normalizeParams;
		}

		public File apply(File audioFile) {
			try {
				File normalizedFile = new File(path, audioFile.getName());
				int size = AudioConverter.convert(audioFile, normalizedFile, normalizeParams);
				assertTrue(normalizedFile.exists());
				assertTrue(size > 0);		
				return normalizedFile;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
	public static Collection<Object[]> wrapParameters(Collection<?> collection) {
		List<Object[]> arrays = new ArrayList<>();
		for (Object o : collection) arrays.add(new Object[] {o});		
		return arrays;
	}
	

	public static class AudioFileSet<T> extends LinkedHashMap<URI, T> {

		private static final long serialVersionUID = 1L;
		
		public final String id;

		public AudioFileSet(File basePath, T initialValue, FileFilter filter, Function<File,File> processor) throws Exception {
			super();
			this.id = basePath.getName();
			for (File audioFile : basePath.listFiles(filter)) {
				File file = processor != null ? processor.apply(audioFile) : audioFile;
				put(file.toURI(), initialValue);
			}
		}

	}
	

}
