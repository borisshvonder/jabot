package jabot.idxsolr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import jabot.idxapi.Index;
import jabot.idxapi.tests.AbstractIndexTestBase;

//TODO: Don't run during normal build
public class SolrIndexAcceptanceIntegrationTest extends AbstractIndexTestBase {
	private File tmp;
	private EmbeddedSolrClient client;
	
	@Override
	protected Index create() throws IOException {
		tmp = Files.createTempDir();
		client = new EmbeddedSolrClient(tmp);
		client.start();
		return new SolrIndex(client);
	}

	@Override
	protected void destroy(final Index _) throws Exception {
		client.close();
		FileUtils.forceDelete(tmp);
	}

}
