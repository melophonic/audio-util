package org.melophonic.audio.util;

import java.io.File;
import java.net.URI;
import java.util.Collection;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.melophonic.audio.spi.AbstractAudioTest;

@RunWith(Parameterized.class)
public class AudioConverterTest extends AbstractAudioTest {

	final AudioFileSet<Double> normalizedFiles;

	public AudioConverterTest(AudioFileSet<Double> normalizedFiles) {
		super();
		this.normalizedFiles = normalizedFiles;
	}
	
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		return wrapParameters(getAudioFileSets(".wav", ".flac"));
	}		


	@Test
	public void testDownsampleWav() throws Exception {
		for (URI uri : normalizedFiles.keySet()) {
			File audioFile = new File(uri);
			File targetFile = new File("./target/converted." + audioFile.getName());
			if (targetFile.exists()) FileUtils.forceDelete(targetFile);
			AudioConverter.Parameters params = new AudioConverter.Parameters(Encoding.PCM_SIGNED, AudioFileFormat.Type.WAVE, 44100F, 16);
			AudioConverter.convert(audioFile, targetFile, params);
		}
	}
	
}
