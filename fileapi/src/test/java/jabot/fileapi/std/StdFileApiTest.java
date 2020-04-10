package jabot.fileapi.std;

import java.io.File;
import java.net.URI;

import org.junit.Assert;
import org.junit.Ignore;

import jabot.fileapi.FileApi;
import jabot.fileapi.std.StdFileApi;

@Ignore("This is an integration test, not paralellizable. And I don't (yet) have a facility to run integration tests")
public class StdFileApiTest extends FileApiTestBase {
	private static final File TEMP_ROOT = new File(System.getProperty("java.io.tmpdir"));
	private static final File TEMP_TEST = new File(TEMP_ROOT, "StdFileApiTest");

	@Override
	protected FileApi createFixture() {
		deleteRecursively(TEMP_TEST);
		TEMP_TEST.mkdir();
		return new StdFileApi();
	}

	@Override
	protected void destroyFixture(FileApi fixture) throws Exception {
		deleteRecursively(TEMP_TEST);
	}

	@Override
	protected URI testLocation(FileApi fixture) {
		return TEMP_TEST.toURI();
	}

	private void deleteRecursively(final File file) {
		if (file.isFile()) {
			Assert.assertTrue("Can't remove: "+file, file.delete());
		} else if (file.isDirectory()) {
			final File[] contents = file.listFiles();
			if (contents != null) {
				for (final File f : contents) {
					deleteRecursively(f);
				}
			}
			Assert.assertTrue("Can't remove: "+file, file.delete());
		} else {
			Assert.fail("Can't remove: "+file);
		}
	}

}
