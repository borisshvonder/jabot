package jabot.idxsolr;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

import jabot.common.props.PropsConfig;

//TODO: Don't run during normal build
public class SolrIndexManagerIntegrationTest {
	
	@Test
	public void test() throws IOException {
		final File tmp1 = Files.createTempDir();
		final File tmp2 = Files.createTempDir();

		try {
			final Properties props = new Properties();
			props.setProperty("solr", "tmp1,tmp2");
			props.setProperty("solr.tmp1", "embedded:" + tmp1.getAbsolutePath());
			props.setProperty("solr.tmp1.prop1", "val1");
			props.setProperty("solr.tmp2", "embedded:" + tmp2.getAbsolutePath());
			try (final SolrIndexManager fixture = new SolrIndexManager(new PropsConfig(props))) {
				fixture.init();
				Assert.assertEquals(Arrays.asList("tmp1", "tmp2"), fixture.listComponents());
				Assert.assertEquals("val1", fixture.getParams("tmp1").get("prop1"));
				Assert.assertNotNull(fixture.getIndex("tmp2"));
			}
		} finally {
			FileUtils.forceDelete(tmp1);
			FileUtils.forceDelete(tmp2);
		}
	}
}
