package jabot.idxsolr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;

import com.google.common.io.Files;

public class EmbeddedSolrClient implements SolrClient, Closeable {
	private static final String EMBEDDED_CLASSPATH_ROOT="/idxsolr/embedded/";
	private final File solrHome;
	private EmbeddedSolrServer server;
	
	public static void main(String [] args) throws IOException, SolrServerException {
		EmbeddedSolrClient client = new EmbeddedSolrClient(new File("/tmp/solrDataDir"));
		client.start();
		final UpdateRequest req = new UpdateRequest();
		req.setAction(ACTION.COMMIT, true, true);
		client.update(req);
		client.close();
		System.out.println("OK");
	}

	public EmbeddedSolrClient(final File solrHome) {
		Validate.notNull(solrHome, "solrHome cannot be null");

		this.solrHome = solrHome;
	}

	public void start() throws IOException {
		bootstrapIfEmpty();
		server = new EmbeddedSolrServer(Paths.get(solrHome.toURI()), "core0");
	}

	@Override
	public void close() throws IOException {
		if (server != null) {
			server.close();
			server = null;
		}
	}

	@Override
	public QueryResponse search(final SolrQuery query) throws SolrServerException, IOException {
		Validate.notNull(server, "Not initialized, you forgot to call start()");

		return server.query(query);
	}

	@Override
	public NamedList<Object> update(UpdateRequest req) throws SolrServerException, IOException {
		Validate.notNull(server, "Not initialized, you forgot to call start()");

		return server.request(req);
	}

	private void bootstrapIfEmpty() throws IOException {
		if (!solrHome.exists()) {
			mkdirs(solrHome);
		}
		if (!solrHome.isDirectory()) {
			throw new IOException("Not a directory " + solrHome);
		}
		final String[] contents = solrHome.list();
		if (contents == null || contents.length == 0) {
			final List<String> manifest = readManifest();
			for (final String path : manifest) {
				final String sourcePath = normalizePath(EMBEDDED_CLASSPATH_ROOT+path);
				final File targetPath = new File(solrHome, path); // TODO: unix/windows slashes
				mkdirs(targetPath.getParentFile());
				Files.copy(() -> getClass().getResourceAsStream(sourcePath), targetPath);
			}
		}
	}

	private String normalizePath(final String path) {
		return path.replaceAll("/./", "/").replaceAll("//", "/");
	}

	private List<String> readManifest() throws IOException {
		try(final InputStream in = getClass().getResourceAsStream(EMBEDDED_CLASSPATH_ROOT+"manifest.txt")) {
			return IOUtils.readLines(in, StandardCharsets.UTF_8);
		}
	}

	private void mkdirs(final File path) throws IOException {
		path.mkdirs();
		if (!path.isDirectory()) {
			throw new IOException("Cannot create " + path);
		}
	}
}
