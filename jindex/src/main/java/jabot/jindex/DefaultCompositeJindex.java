package jabot.jindex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Untokenized;

/**
 * Simple implementation of the CompositeJindex. It is @threadsafe only after all {@link #addComponent(String, Jindex, Map)} 
 * calls are made. It is unsafe to share the instance of this class with other threads before that.
 *
 */
public class DefaultCompositeJindex implements CompositeJindex {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultCompositeJindex.class);
	private final List<String> allIds, allIdsRO;
	private final Map<String, ConfiguredJindex> id2configured;
	private final Map<Class<?>, List<Jindex>> klass2jindex;
	private String defaultComponentName;
	private Jindex defaultComponent;
	private List<Jindex> defaultComponentAsList;
	
	public DefaultCompositeJindex(final int size) {
		this.id2configured = new HashMap<>(size);
		this.allIds = new ArrayList<>(size);
		this.allIdsRO = Collections.unmodifiableList(allIds);
		this.klass2jindex = new HashMap<>();
	}

	/**
	 * <ul>
	 *  <li>first component is always default</li>
	 *  <li>params['handles'] specifies comma-separated list of java classes that component handles</li>
	 * </ul>
	 */
	public void addComponent(final String id, final Jindex jindex, final Map<String, String> params) {
		final ConfiguredJindex newComponent = new ConfiguredJindex(id, jindex, params);
		final ConfiguredJindex old = id2configured.put(id, newComponent);
		Validate.isTrue(old == null, "Duplicated component id={}", id);
		
		allIds.add(id);
		if (defaultComponentName == null) {
			defaultComponentName = id;
			defaultComponent = jindex;
			defaultComponentAsList = Arrays.asList(jindex);
		}
		
		for (final Class<?> klass : newComponent.handles) {
			List<Jindex> jindexes = klass2jindex.get(klass);
			if (jindexes == null) {
				jindexes = new LinkedList<>();
				klass2jindex.put(klass, jindexes);
			}
			jindexes.add(jindex);
		}
	}

	@Override
	public List<String> getAllComponents() {
		return allIdsRO;
	}

	@Override
	public String getDefaultComponent() {
		return defaultComponentName;
	}

	@Override
	public Jindex getComponent(final String id) {
		return getConfigured(id).jindex;
	}

	@Override
	public List<Class<?>> handles(final String id) {
		return getConfigured(id).handles;
	}

	@Override
	public void store(final Untokenized pk, final Object bean) {
		Validate.notNull(pk, "pk cannot be null");
		Validate.notNull(bean, "bean cannot be null");
		
		final Jindex first = firstFor(bean.getClass());
		first.store(pk, bean);
	}

	@Override
	public void removeByKey(final Untokenized pk) {
		for (final ConfiguredJindex configured : id2configured.values()) {
			configured.jindex.removeByKey(pk);
		}
	}

	@Override
	public void removeByQuery(final Class<?> objectType, final Query search) {
		for (final Jindex jindex : allFor(objectType)) {
			jindex.removeByQuery(objectType, search);
		}
	}

	@Override
	public void commit() throws DelayedIndexingException {
		final StringBuilder errors = new StringBuilder();
		for (final ConfiguredJindex configured : id2configured.values()) {
			try {
				configured.jindex.commit();
			} catch (final DelayedIndexingException ex) {
				errors.append("component ").append(configured.id).append(": ").append(ex).append("\n");
			}
		}
		if (errors.length() > 0) {
			throw new DelayedIndexingException(errors.toString());
		}
	}

	@Override
	public <T> JIndexResults<T> search(final Class<T> objectType, final Query search, final int offset) {
		final JIndexResultsMixer<T> ret = new JIndexResultsMixer<>();
		try {
			for (final Jindex jindex : allFor(objectType)) {
				ret.addBackend(jindex.search(objectType, search, offset));
			}
		} catch (final RuntimeException ex) {
			LOG.error("Error performing search, closing all results");
			try {
				ret.close();
			} catch (final IOException ioEx) {
				throw new UncheckedIOException(ioEx);
			}
			throw new RuntimeException(ex);
		}
		return ret;
	}
	
	private List<Jindex> allFor(final Class<?> objectType) {
		final List<Jindex> jindexes = klass2jindex.get(objectType);
		if (jindexes == null) {
			return defaultComponentAsList;
		}
		return jindexes;
	}


	private Jindex firstFor(final Class<? extends Object> klass) {
		final List<Jindex> jindexes = klass2jindex.get(klass);
		if (jindexes == null) {
			return defaultComponent;
		}
		return jindexes.iterator().next();
	}
	
	private ConfiguredJindex getConfigured(final String id) {
		Validate.notNull(id, "component id cannot be null");
		
		final ConfiguredJindex ret = id2configured.get(id);
		Validate.isTrue(ret != null, "Component not found: {}", id);
		return ret;
	}
	
	private static final class JIndexResultsMixer<T> implements JIndexResults<T> {
		private final List<JIndexResults<T>> backends = new LinkedList<>();
		
		/** Backends that may still have results */
		private List<JIndexResults<T>> active;
		
		private Iterator<JIndexResults<T>> current;
		
		public void addBackend(final JIndexResults<T> backend) {
			backends.add(backend);
		}

		@Override
		public T next() {
			if (active == null) {
				active = new LinkedList<>(backends);
				current = active.iterator();
			}
			T ret = null;
			while (ret == null) {
				if (active.isEmpty()) {
					return null;
				}
				if (!current.hasNext()) {
					// restart
					current = active.iterator();
				}
				final JIndexResults<T> backend = current.next();
				ret = backend.next();
				if (ret == null) {
					current.remove(); // remove from active
				}
			}

			return ret;
		}

		@Override
		public void close() throws IOException {
			boolean faulted = false;
			for (final JIndexResults<?> backend : backends) {
				try {
					backend.close();
				} catch (final Exception ex) {
					LOG.error(
						"Exception while closing results {}, error is deferred in order to close remaining results",
						backend,
						ex
					);
					faulted = true;
				}
			}
			if (faulted) {
				throw new IllegalStateException("Some connectors failed to close, see log");
			}
			active.clear();
			current = active.iterator();
		}

		@Override
		public long estimateTotalResults() {
			long ret = 0;
			for (final JIndexResults<?> backend : backends) {
				ret += backend.estimateTotalResults();
			}
			return ret;
		}
	}


	private static final class ConfiguredJindex {
		private final String id;
		private final Jindex jindex;
		private final List<Class<?>> handles;
		
		public ConfiguredJindex(final String id, final Jindex jindex, final Map<String, String> params) {
			this.id = id;
			this.jindex = jindex;
			
			this.handles = Collections.unmodifiableList(readHandles(params.get("handles")));
		}

		private List<Class<?>> readHandles(final String handles) {
			String normalized = handles;
			if (handles == null) {
				normalized = "";
			}
			final String [] list = normalized.split(",");
			final List<Class<?>> ret = new ArrayList<>(list.length);
			for (String name : list) {
				name = name.trim();
				if (!name.isEmpty()) {
					Class<?> klass;
					try {
						klass = Class.forName(name);
					} catch (final ClassNotFoundException ex) {
						throw new IllegalArgumentException("Bad class"+name, ex);
					}
					if (ret.contains(klass)) {
						throw new IllegalArgumentException("Duplicated "+klass);
					}
					ret.add(klass);
				}
			}
			return ret;
		}
	}
}
