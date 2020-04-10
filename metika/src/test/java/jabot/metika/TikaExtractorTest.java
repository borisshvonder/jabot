package jabot.metika;

import jabot.metapi.MetadataExtractor;
import jabot.metapi.tests.MetadataExtractorAcceptanceTestBase;

public class TikaExtractorTest extends MetadataExtractorAcceptanceTestBase {

	@Override
	protected MetadataExtractor createFixture() {
		return new TikaExtractor();
	}

	@Override
	protected void destroyFixture(MetadataExtractor fixture) {}
	
}
