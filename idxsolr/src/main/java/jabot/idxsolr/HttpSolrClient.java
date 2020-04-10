package jabot.idxsolr;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.Validate;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

public class HttpSolrClient implements SolrClient, Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(HttpSolrClient.class);
	private final URI uri;
	
	private org.apache.solr.client.solrj.SolrClient server;
	
	public static void main(String [] args) throws IOException, SolrServerException {
		HttpSolrClient client = new HttpSolrClient(URI.create("http://127.0.0.1:8983/solr/core0"));
		client.start();
		final UpdateRequest req = new UpdateRequest();
		req.setAction(ACTION.COMMIT, true, true);
		client.update(req);
		client.close();
		System.out.println("OK");
	}
	
	public HttpSolrClient(final URI uri) {
		Validate.notNull(uri, "uri cannot be null");
		
		this.uri = uri;
	}

	public void start() throws JsonProcessingException, IOException {
		Validate.isTrue(server == null, "already started");
	
		server = new org.apache.solr.client.solrj.impl.HttpSolrClient.Builder(uri.toString())
				.allowCompression(true)
				.build();
		
		LOG.trace("Started");
	}

	@Override
	public void close() throws IOException {
		if (server != null) {
			server.close();
			LOG.trace("Closed");
			server = null;
		}
	}

	@Override
	public QueryResponse search(final SolrQuery query) throws SolrServerException, IOException {
		Validate.notNull(server, "Not initialized, you forgot to call start()");

		LOG.trace("query={}", query);

		return server.query(query);
	}

	@Override
	public NamedList<Object> update(final UpdateRequest req) throws SolrServerException, IOException {
		Validate.notNull(server, "Not initialized, you forgot to call start()");

		LOG.trace("update={}", req);

		return server.request(req);
	}
}
