package org.melophonic.audio.spi.tarsos;

import org.melophonic.audio.spi.AnalysisServiceTest;



public class TarsosAnalysisServiceTest extends AnalysisServiceTest<TarsosAnalysisService> {
	
	public TarsosAnalysisServiceTest(AudioFileSet<Double> normalizedFiles) {
		super(normalizedFiles);
	}

	protected Class<TarsosAnalysisService> getServiceClass() {
		return TarsosAnalysisService.class;
	}
	


}
