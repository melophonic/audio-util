package org.melophonic.audio.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.melophonic.audio.util.AudioConverter;
import org.uncommons.maths.combinatorics.CombinationGenerator;

@RunWith(Parameterized.class)
public abstract class AnalysisServiceTest<S extends AnalysisService> extends AbstractAudioTest {

	final static AudioConverter.Parameters normalizeParams = new AudioConverter.Parameters(Encoding.PCM_SIGNED, AudioFileFormat.Type.WAVE, 44100F, 16);

	final AudioFileSet<Double> normalizedFiles;

	protected S service;

	public AnalysisServiceTest(AudioFileSet<Double> normalizedFiles) {
		super();
		this.normalizedFiles = normalizedFiles;
	}

	protected abstract Class<S> getServiceClass();

	@Parameters
	public static Collection<Object[]> data() throws Exception {
		return wrapParameters(getAudioFileSets(normalizeParams));
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		ServiceLoader<AnalysisService> loader = ServiceLoader.load(AnalysisService.class);
		for (Iterator<AnalysisService> i = loader.iterator(); i.hasNext();) {
			AnalysisService as = i.next();
			if (as.getClass().equals(getServiceClass())) {
				service = (S) as;
				return;
			}
		}
		fail("Unable to load " + getServiceClass().getName());
	}

	boolean linear = true;
	double silenceThresholdDb = AnalysisService.DEFAULT_SILENCE_THRESHOLD_DB;
	
	@Test
	public void testFingerprintService() throws Exception {
		CombinationGenerator<URI> pairs = new CombinationGenerator<>(normalizedFiles.keySet(), 2);
		while (pairs.hasMore()) {
			List<URI> pair = pairs.nextCombinationAsList();
			Map<Double, Double> aspl = service.getSoundPressureLevels(pair.get(0), linear, silenceThresholdDb);
			Map<Double, Double> bspl = service.getSoundPressureLevels(pair.get(1), linear, silenceThresholdDb);
			
			double a = avg(aspl.values());
			double b = avg(bspl.values());
			
			// expect some diff. due to encoding
			log.info("Loudness a:{} b:{}", a, b);
			assertEquals(a, b, 1.0f);
		
		}

	}
	
	public static double avg(Collection<Double> values) {
		double total = 0.0;
		for (Double value : values) total += value;
		return total / values.size();
	}

}
