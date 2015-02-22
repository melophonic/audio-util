package org.melophonic.audio.spi.musicg;

import org.melophonic.audio.spi.FingerprintServiceTest;

public class MGFingerprintServiceTest extends FingerprintServiceTest<MGFingerprintService> {
	
	public MGFingerprintServiceTest(AudioFileSet<byte[]> normalizedFiles) {
		super(normalizedFiles);
	}

	protected Class<MGFingerprintService> getServiceClass() {
		return MGFingerprintService.class;
	}
	


}
