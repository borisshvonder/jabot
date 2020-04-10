package jabot.idxsolr;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.props.PropsConfig;

/**
 * Manages multiple index instances. Configuration-driven.
 * Uses {@link SolrConnector} to hook up to specific solr endpoints. Following sample configuration will explain usage:
 *
 * <pre>
 * solr=core0,core1
 * solr.core0=embedded:/var/db/code
 * solr.core0.batchSize=100
 * solr.core0.batchBytes=1048576
 * solr.core0.pageSize=10
 * solr.core1=http://localhost:8983/solr/core1
 * solr.core1.batchSize=10
 * solr.core1.batchBytes=2048576
 * solr.core1.pageSize=10
 * solr.core1.param1=this parameter is not used by connector, but reported by getParams("core1").get("param1")
 * solr.core1.param2=there can be any number of such additional parameters
 * </pre>
 */
public class SolrIndexManager implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(SolrIndexManager.class);
	private final List<ConfiguredConnector> allConnectors;
	
	public SolrIndexManager(final PropsConfig properties) {
		this(properties, "solr");
	}
	
	public SolrIndexManager(final PropsConfig properties, final String propbase) {
		final String [] components = properties.getString(propbase, "").split(",");
		this.allConnectors = new ArrayList<>(components.length);
		
		final Set<String> names = new HashSet<>(components.length);
		for (final String component : components) {
			final String normalized = component.trim();
			
			if (!names.add(normalized)) {
				throw new IllegalArgumentException("Duplicated component "+normalized);
			}
			
			final ConfiguredConnector configured = ConfiguredConnector.configure(
					normalized, 
					properties, 
					propbase+"."+normalized
			);
			
			allConnectors.add(configured);
		}
	}
	
	public synchronized void init() {
		boolean faulted = false;
		for (final ConfiguredConnector connector : allConnectors) {
			try {
				connector.init();
			} catch (final Exception ex) {
				LOG.error(
					"Exception while initializing connector {}, error is deferred in order to close remaining connectors",
					connector.getId(),
					ex
				);
				faulted = true;
			}
		}
		if (faulted) {
			throw new IllegalStateException("Some connectors failed to close, see log");
		}
	}
	
	@Override
	public void close() throws IOException {
		boolean faulted = false;
		for (final ConfiguredConnector connector : allConnectors) {
			try {
				connector.close();
			} catch (final Exception ex) {
				LOG.error(
					"Exception while closing connector {}, error is deferred in order to close remaining connectors",
					connector.getId(),
					ex
				);
				faulted = true;
			}
		}
		if (faulted) {
			throw new IllegalStateException("Some connectors failed to close, see log");
		}
	}
	
	public Collection<String> listComponents() {
		final List<String> ret = new ArrayList<>(allConnectors.size());
		for (final ConfiguredConnector connector : allConnectors) {
			ret.add(connector.getId());
		}
		return ret;
	}
	
	public Map<String, String> getParams(final String componentId) {
		return findConnector(componentId).getParams();
	}
	
	public SolrIndex getIndex(final String componentId) {
		return findConnector(componentId).getIndex();
	}

	private ConfiguredConnector findConnector(final String id) {
		Validate.notEmpty(id, "id cannot be empty");
		
		for (final ConfiguredConnector connector : allConnectors) {
			if (id.equals(connector.getId())) {
				return connector;
			}
		}
		
		throw new IllegalArgumentException("No such connector");
	}

	private static class ConfiguredConnector implements Closeable {
		private final SolrConnector connector;
		private final String id;
		private final Map<String, String> params;
		private SolrIndex index;
		
		private ConfiguredConnector(final String id, final SolrConnector connector, final Map<String, String> params) {
			this.id = id;
			this.connector = connector;
			this.params = Collections.unmodifiableMap(params);
		}
		
		public static final ConfiguredConnector configure(
				final String id, 
				final PropsConfig properties, 
				final String propbase
		) {
			final Collection<String> paramKeys = properties.keysWithPrefix(propbase+".");
			final Map<String, String> params = new HashMap<>(paramKeys.size());
			for (final String key : paramKeys) {
				final String shortName = key.substring(propbase.length()+1);
				params.put(shortName, properties.getString(key, null));
			}
			
			final SolrConnector connector = new SolrConnector(properties, propbase);
			return new ConfiguredConnector(id, connector, params);
		}

		public String getId() {
			return id;
		}
		
		public Map<String, String> getParams() {
			return params;
		}
		
		public void init() throws IOException {
			if (index == null) {
				index = new SolrIndex(connector.connect());
			}
		}
		
		public SolrIndex getIndex() {
			if (index == null) {
				throw new IllegalStateException("Not initialized");
			}
			return index;
		}

		@Override
		public void close() throws IOException {
			connector.close();
		}
	}

}
