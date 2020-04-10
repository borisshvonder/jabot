package jabot.fileapi.std;

import java.net.URI;

import org.junit.Assert;

import jabot.fileapi.FileApi;
import jabot.fileapi.std.RamFileApi;

public class RamFileApiTest extends FileApiTestBase {

	@Override
	protected RamFileApi createFixture() {
		final RamFileApi result = new RamFileApi();
		result.setStressTest(false);
		Assert.assertFalse(result.isStressTest());
		return result;
	}

	@Override
	protected void destroyFixture(final FileApi fixture) throws Exception {}

	@Override
	protected URI testLocation(final FileApi fixture) {
		return URI.create(fixture.getScheme()+":/");
	}

}
