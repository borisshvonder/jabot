package jabot.fileapi.std;

import org.junit.Assert;

import jabot.fileapi.std.RamFileApi;

public class RamFileApStressTest extends RamFileApiTest {
	@Override
	protected RamFileApi createFixture() {
		final RamFileApi result = super.createFixture();
		result.setStressTest(true);
		Assert.assertTrue(result.isStressTest());
		return result;
	}
}
