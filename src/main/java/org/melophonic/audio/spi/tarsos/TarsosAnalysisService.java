package org.melophonic.audio.spi.tarsos;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import org.melophonic.audio.spi.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * Adapted from an example in the TarsosDSP library
 */
public class TarsosAnalysisService implements AnalysisService {

	final Logger log = LoggerFactory.getLogger(getClass());
	
	public final static int DEFAULT_LOUDNESS_SIZE = 2048;
	public final static int DEFAULT_LOUDNESS_OVERLAP = 0;

	@Override
	public Map<Double, Double> getSoundPressureLevels(URI audioUri, boolean linear, double silenceThresholdDb) throws Exception {
		return getSoundPressureLevels(audioUri, linear, silenceThresholdDb, DEFAULT_LOUDNESS_SIZE, DEFAULT_LOUDNESS_OVERLAP);
	}

	public Map<Double, Double> getSoundPressureLevels(URI audioUri, boolean linear, double silenceThresholdDb, int size, int overlap) throws Exception {
		AudioDispatcher dispatcher = AudioDispatcherFactory.fromURL(audioUri.toURL(), size, overlap);
		LoudnessProcessor loudnessProcessor = new LoudnessProcessor(linear, silenceThresholdDb);
		dispatcher.addAudioProcessor(loudnessProcessor.silenceDetecor);
		dispatcher.addAudioProcessor(loudnessProcessor);
		dispatcher.run();
		return loudnessProcessor;
	}
	
	static class LoudnessProcessor extends TreeMap<Double, Double> implements AudioProcessor {
		
		private static final long serialVersionUID = 5409110729077631745L;
		
		final boolean linear;
		final SilenceDetector silenceDetecor;
		
		public LoudnessProcessor(boolean linear, double silenceThresholdDb) {
			this.linear = linear;
			this.silenceDetecor = new SilenceDetector(silenceThresholdDb, false);
		}
		
		
		@Override
		public void processingFinished() {}
	
		@Override
		public boolean process(AudioEvent audioEvent) {
			//log.debug(audioEvent.getTimeStamp() + ";" + silenceDetecor.currentLinearSPL());
			double currentSPL = linear ? silenceDetecor.currentLinearSPL() : silenceDetecor.currentSPL();
			put(audioEvent.getTimeStamp(), currentSPL);
			return true;
		}		
		
	}
	
	/**
	 * The continuing silence detector does not break the audio processing pipeline when silence is detected.
	 */
	public static class SilenceDetector implements AudioProcessor {
		
		private final double threshold;//db
		
		private final boolean breakProcessingQueueOnSilence;
		

		
		/**
		 * Create a new silence detector with a defined threshold.
		 * 
		 * @param silenceThreshold
		 *            The threshold which defines when a buffer is silent (in dB).
		 *            Normal values are [-70.0,-30.0] dB SPL.
		 * @param breakProcessingQueueOnSilence 
		 */
		public SilenceDetector(final double silenceThreshold,boolean breakProcessingQueueOnSilence){
			this.threshold = silenceThreshold;
			this.breakProcessingQueueOnSilence = breakProcessingQueueOnSilence;
		}

		/**
		 * Calculates the local (linear) energy of an audio buffer.
		 * 
		 * @param buffer
		 *            The audio buffer.
		 * @return The local (linear) energy of an audio buffer.
		 */
		private double localEnergy(final float[] buffer) {
			double power = 0.0D;
			for (float element : buffer) {
				power += element * element;
			}
			return power;
		}

		/**
		 * Returns the dBSPL for a buffer.
		 * 
		 * @param buffer
		 *            The buffer with audio information.
		 * @return The dBSPL level for the buffer.
		 */
		private double soundPressureLevel(final float[] buffer) {
			double value = Math.pow(localEnergy(buffer), 0.5);
			value = value / buffer.length;
			currentLinearSPL = value;
			return linearToDecibel(value);
		}

		/**
		 * Converts a linear to a dB value.
		 * 
		 * @param value
		 *            The value to convert.
		 * @return The converted value.
		 */
		private double linearToDecibel(final double value) {
			return 20.0 * Math.log10(value);
		}
		
		double currentSPL = 0;
		public double currentSPL(){
			return currentSPL;
		}
		
		double currentLinearSPL = 0;
		public double currentLinearSPL(){
			return currentLinearSPL;
		}
		

		/**
		 * Checks if the dBSPL level in the buffer falls below a certain threshold.
		 * 
		 * @param buffer
		 *            The buffer with audio information.
		 * @param silenceThreshold
		 *            The threshold in dBSPL
		 * @return True if the audio information in buffer corresponds with silence,
		 *         false otherwise.
		 */
		public boolean isSilence(final float[] buffer, final double silenceThreshold) {
			currentSPL = soundPressureLevel(buffer);
			return currentSPL < silenceThreshold;
		}

		public boolean isSilence(final float[] buffer) {
			return isSilence(buffer, threshold);
		}


		@Override
		public boolean process(AudioEvent audioEvent) {
			boolean isSilence = isSilence(audioEvent.getFloatBuffer());
			//break processing chain on silence?
			if(breakProcessingQueueOnSilence){
				//break if silent
				return !isSilence;
			}else{
				//never break the chain
				return true;
			}
		}


		@Override
		public void processingFinished() {
		}
	}

	
	
}
