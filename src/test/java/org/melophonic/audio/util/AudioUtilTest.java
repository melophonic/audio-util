package org.melophonic.audio.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.melophonic.audio.util.AudioUtil.getAvailableMixers;
import static org.melophonic.audio.util.AudioUtil.getResourceName;
import static org.melophonic.audio.util.AudioUtil.getSupportedAudioFileFormatTypes;

import java.net.URI;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.Mixer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioUtilTest {
	
	final static Logger log = LoggerFactory.getLogger(AudioUtilTest.class);

	@Test
	public void testGetAvailableMixers() {
		List<Mixer.Info> mixers = getAvailableMixers();
		assertNotNull(mixers);
		assertFalse(mixers.isEmpty());
		for (Mixer.Info mixer : getAvailableMixers()) log.info("Mixer: " + mixer);
	}
	
	@Test 
	public void testGetSupportedAudioFileFormatTypes() {
		log.info("supportedAudioFileFormatTypes");
		for (AudioFileFormat.Type type : getSupportedAudioFileFormatTypes()) {
			log.info(type.toString() + ": " + type.getExtension());
		}
	}
	
	final static String[] testUris = {
		"http://example.com/foo/bar/foo42?param=true",
		"http://example.com/foo42",
		"http://example.com/foo42/",
		"file:/some/path/foo42/"
	};
	
	@Test
	public void testGetResourceName() throws Exception {
		for (String testUri : testUris) {
			URI uri = new URI(testUri);
			assertEquals("foo42", getResourceName(uri));
		}
		
	}

}
