package jabot.idxsolr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

import jabot.common.props.PropsConfig;

//TODO: Don't run during normal build
public class SolrConnectorIntegrationTest {
	@Test
	public void test() throws IOException {
		final File tmp = Files.createTempDir();
		try {
			final Properties props = new Properties();
			props.setProperty("solr", "embedded:"+tmp.getAbsolutePath());
			try(final SolrConnector fixture = new SolrConnector(new PropsConfig(props))) {
				Assert.assertNotNull(fixture.connect());
				fixture.disconnect();
			}
		} finally {
			FileUtils.forceDelete(tmp);
		}
	}
}
