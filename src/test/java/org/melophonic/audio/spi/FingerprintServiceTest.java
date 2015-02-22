package org.melophonic.audio.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.melophonic.audio.util.AudioUtil.DEFAULT_FLOAT_COMPARISON_THRESHOLD;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.melophonic.audio.spi.FingerprintService.FingerprintComparison;
import org.melophonic.audio.util.AudioConverter;
import org.melophonic.audio.util.AudioUtil;
import org.uncommons.maths.combinatorics.CombinationGenerator;

@RunWith(Parameterized.class)
public abstract class FingerprintServiceTest<S extends FingerprintService> extends AbstractAudioTest {

	final static AudioConverter.Parameters normalizeParams = new AudioConverter.Parameters(Encoding.PCM_SIGNED, AudioFileFormat.Type.WAVE, 44100F, 16);

	final AudioFileSet<byte[]> normalizedFiles;

	protected S service;

	public FingerprintServiceTest(AudioFileSet<byte[]> normalizedFiles) {
		super();
		this.normalizedFiles = normalizedFiles;
	}

	protected abstract Class<S> getServiceClass();

	@Parameters
	public static Collection<Object[]> data() throws Exception {
		return wrapParameters(getAudioFileSets(normalizeParams, ".wav"));
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		ServiceLoader<FingerprintService> loader = ServiceLoader.load(FingerprintService.class);
		for (Iterator<FingerprintService> i = loader.iterator(); i.hasNext();) {
			FingerprintService fs = i.next();
			if (fs.getClass().equals(getServiceClass())) {
				service = (S) fs;
				return;
			}
		}
		fail("Unable to load " + getServiceClass().getName());
	}

	@Test
	public void testFingerprintService() throws Exception {
		for (URI audio : normalizedFiles.keySet()) {
			long start = System.currentTimeMillis();
			byte[] fingerprint = service.calculateFingerprint(audio);
			long elapsed = System.currentTimeMillis() - start;
			log.info(String.format("Fingerprinted %s in %s ms", AudioUtil.getResourceName(audio), elapsed));
			normalizedFiles.put(audio, fingerprint);
		}

		CombinationGenerator<URI> pairs = new CombinationGenerator<>(normalizedFiles.keySet(), 2);
		while (pairs.hasMore()) {
			List<URI> pair = pairs.nextCombinationAsList();
			byte[] a = normalizedFiles.get(pair.get(0));
			byte[] b = normalizedFiles.get(pair.get(1));
			long start = System.currentTimeMillis();
			FingerprintComparison fc = service.compareFingerprints(a, b);
			long elapsed = System.currentTimeMillis() - start;
			String _a = AudioUtil.getResourceName(pair.get(0));
			String _b = AudioUtil.getResourceName(pair.get(1));

			log.info(String.format("Comparison[%s v. %s] Similarity %s at %s secs. (%s ms)", _a, _b, fc.getSimilarity(), fc.getMostSimilarTime(), elapsed));
			assertNotNull(fc);
			assertEquals(1.0f, fc.getSimilarity(), DEFAULT_FLOAT_COMPARISON_THRESHOLD);
			assertEquals(0.0f, fc.getMostSimilarTime(), 1.1f); //TODO one of these is 1.0 secs longer 
		}

	}

}
