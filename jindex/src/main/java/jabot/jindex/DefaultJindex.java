package jabot.jindex;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.search.Query;

import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Document;
import jabot.idxapi.Index;
import jabot.idxapi.SearchResults;
import jabot.idxapi.Untokenized;
import jabot.jindex.ModelIntrospector.Mapper;

public class DefaultJindex implements Jindex {
	private static final List<String> METAFIELDS = Arrays.asList("metadata", "metadataRu");
	private final Index index;

	public DefaultJindex(final Index index) {
		Validate.notNull(index, "index cannot be null");
		
		this.index = index;
	}

	@Override
	public void store(final Untokenized pk, final Object bean) {
		Validate.notNull(pk, "pk cannot be null");
		Validate.notNull(bean, "bean cannot be null");
		
		final Class<?> model = bean.getClass();

		@SuppressWarnings("unchecked")
		final Mapper<Object> mapper = (Mapper<Object>)ModelMappersInventory.getMapper(model);
		
		final Document doc = mapper.bean2doc(pk, bean);
		index.store(doc);
	}

	@Override
	public void removeByKey(final Untokenized pk) {
		Validate.notNull(pk, "pk cannot be null");

		index.removeByKey(pk);
	}

	@Override
	public void removeByQuery(final Class<?> objectType, final Query search) {
		Validate.notNull(objectType, "objectType cannot be null");
		Validate.notNull(search, "search cannot be null");

		index.removeByQuery(toSearchQuery(objectType, search));
	}

	@Override
	public void commit() throws DelayedIndexingException {
		index.commit();
	}

	@Override
	public <T> JIndexResults<T> search(final Class<T> objectType, final Query search, final int offset) {
		final Mapper<T> mapper = ModelMappersInventory.getMapper(objectType);
		final String translatedQuery = toSearchQuery(mapper, search);
		
		// SearchResults MUST be closed in JIndexResultsImpl
		final SearchResults results = index.search(translatedQuery, mapper.getStoredFields(), offset);
		return new JIndexResultsImpl<>(mapper, results);
	}
	
	private<T> String toSearchQuery(final Class<T> objectType, final Query search) {
		final Mapper<T> mapper = ModelMappersInventory.getMapper(objectType);
		return toSearchQuery(mapper, search);
	}


	private<T> String toSearchQuery(final Mapper<T> mapper, final Query search) {
		final ModelBasedTranslator<T> translator = new ModelBasedTranslator<>(mapper);
		translator.setExpandDefaultField(true);
		translator.setExpandDefaultFieldTo(METAFIELDS);
		final Query translated = translator.translate(search);
		return translated.toString();
	}
	
	private static final class JIndexResultsImpl<T> implements JIndexResults<T> {
		private final Mapper<T> mapper;
		private final SearchResults results;
		
		public JIndexResultsImpl(final Mapper<T> mapper, final SearchResults results) {
			this.mapper = mapper;
			this.results = results;
		}

		@Override
		public T next() {
			final Document doc = results.next();
			if (doc == null) {
				return null;
			} else {
				return mapper.doc2bean(doc);
			}
		}

		@Override
		public void close() throws IOException {
			results.close();
		}

		@Override
		public long estimateTotalResults() {
			return results.estimateTotalResults();
		}
		
	}
}
