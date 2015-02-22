package org.melophonic.audio.spi;

import java.net.URI;

public interface FingerprintService {
	
	/**
	 * The results of the <code>compareFingerprints</code> operation
	 */
	public interface FingerprintComparison {
		
		/**
		 * @return a value between 0.0 (no similarity) and 1.0 (exact match)
		 */
		double getSimilarity();
		
		/**
		 * @return the time offset in seconds at which audio B is most similar to audio A
		 */
		double getMostSimilarTime();
		
		/**
		 * @return the index of the frame at which audio B is most similar to audio A
		 */
		int getMostSimilarFrame();
		
	}
	
	/**
	 * Calculates the acoustic fingerprint of an audio file.
	 * 
	 * @param audioUri the input audio
	 * @return the unique fingerprint of the input audio
	 * @throws Exception
	 */
	byte[] calculateFingerprint(URI audioUri) throws Exception;
	
	/**
	 * Returns a comparison between two acoustic fingerprints generated
	 * by <code>calculateFingerprint<code>
	 * 
	 * @param a the reference fingerprint
	 * @param b the comparison fingerprint
	 * @return the results of the comparison
	 * @throws Exception
	 */
	FingerprintComparison compareFingerprints(byte[] a, byte[] b) throws Exception;

}
