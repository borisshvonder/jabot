package jabot.idxsolr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.props.PropsConfig;

/**
 * Connects to Solr instance using properties file.
 * Currently, followin connection schemes supported:
 * <ul>
 *   <li>embedded:/var/db/core - connect to file:/var/db/core folder using embedded solr</li>
 *   <li>http://server:port/solr/core - connect to http solr endpoint</li>
 * </ul>
 * 
 * Connector is initialized with keys specified after a prefix (default:solr), see following example:
 * <pre>
 * solr=embedded:/var/db/code
 * solr.batchSize=100
 * solr.batchBytes=1048576
 * solr.pageSize=10
 * </pre>
 * 
 * @threadsafe
 */
public class SolrConnector implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(SolrConnector.class);
	private final PropsConfig config;
	private final String configbase;
	private final AtomicReference<SolrClient> connection = new AtomicReference<>();
	
	public SolrConnector(final PropsConfig properties, final String propbase) {
		this.config = properties;
		this.configbase = propbase;
	}
	
	public SolrConnector(final PropsConfig properties) {
		this(properties, "solr");
	}
	
	/**
	 * Since there is no reason two have multiple connections to same solr instance, the same connection will be
	 * returned each time
	 * @throws IOException 
	 */
	public SolrClient connect() throws IOException {
		SolrClient client = connection.get();
		if (client == null) {
			synchronized(connection) {
				client = connection.get();
				if (client == null) {
					client = createClient();
					connection.set(client);
				}
			}
		}
		return client;
	}

	/** Release the connection */
	public void disconnect() {
		final SolrClient client = connection.get();
		if (client instanceof Closeable) {
			try {
				((Closeable)client).close();
			} catch (final IOException ex) {
				LOG.warn("Failed to shutdown client {}", client, ex);
			}
			connection.compareAndSet(client, null);
		}
	}
	
	
	private SolrClient createClient() throws IOException {
		final String connectString = config.getString(configbase, null);
		Validate.notNull(connectString, "solr connection string is not specified in configuration");
		
		final URI uri = URI.create(config.getString(configbase, null));
		switch(uri.getScheme()) {
		case "embedded": return createEmbedded(uri);
		case "http": return createHttp(uri);
		default: throw new IllegalArgumentException("Unsupported scheme: "+uri.getScheme());
		}
	}

	private SolrClient createEmbedded(final URI connectString) throws IOException {
		final File localFile = new File(connectString.getPath());
		final EmbeddedSolrClient ret = new EmbeddedSolrClient(localFile);
		ret.start();
		return ret;
	}
	
	private SolrClient createHttp(final URI connectString) throws IOException {
		final HttpSolrClient ret = new HttpSolrClient(connectString);
		ret.start();
		return ret;
	}

	@Override
	public void close() {
		disconnect();
	}
}
