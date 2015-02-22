package org.melophonic.audio.spi;

import java.net.URI;
import java.util.Map;


public interface AnalysisService {

	public static final double DEFAULT_SILENCE_THRESHOLD_DB = -70.0;
	
	/**
	 * Returns a <code>Map<Double, Double></code> of tracking times in seconds
	 * to sound pressure levels, outputting either linear or logarithmic values.
	 * 
	 * @param audioUri input audio
	 * @param linear if true, returns linear SPL values; if false, returns dB values
	 * @param silenceThresholdDb minimum SPL required to trigger an event
	 * @return event times (s) mapped to SPLs
	 * @throws Exception
	 */
	Map<Double, Double> getSoundPressureLevels(URI audioUri, boolean linear, double silenceThresholdDb) throws Exception;

}
