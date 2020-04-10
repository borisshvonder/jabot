package jabot.idxsolr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.Validate;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.idxapi.Binary;
import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Document;
import jabot.idxapi.Field;
import jabot.idxapi.FieldValue;
import jabot.idxapi.FieldValueInvalidException;
import jabot.idxapi.Index;
import jabot.idxapi.IndexingException;
import jabot.idxapi.SearchException;
import jabot.idxapi.SearchResults;
import jabot.idxapi.Untokenized;

/**
 * @threadsafe
 */
public class SolrIndex implements Index {
	private static final Logger LOG = LoggerFactory.getLogger(SolrIndex.class);
	private static final int DEFAULT_BATCHSIZE=10240;
	private static final int DEFAULT_BATCHBYTES=10*1024*1024; // 10Mb
	private static final int DEFAULT_PAGESIZE=50;
	
	/** @visiblefortesting */
	static final int MAX_PAGESIZE=10000;
	
	private static final String BAD_PAGESIZEMSG="pageSize must be in range [1:"+MAX_PAGESIZE+"]";
	private final SolrClient solr;
	private final Batch batch = new Batch();
	private int batchSize = DEFAULT_BATCHSIZE;
	private int batchBytes = DEFAULT_BATCHBYTES;
	private AtomicInteger pageSize = new AtomicInteger(DEFAULT_PAGESIZE);
	private final AtomicReference<Exception> lastUpdateError = new AtomicReference<>();
	
	
	public SolrIndex(final SolrClient solr) {
		Validate.notNull(solr, "solr cannot be null");
		
		this.solr = solr;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	
	public int getBatchBytes() {
		return batchBytes;
	}

	public void setBatchBytes(int batchBytes) {
		this.batchBytes = batchBytes;
	}

	public int getPageSize() {
		return pageSize.get();
	}

	public void setPageSize(int pageSize) {
		Validate.isTrue(pageSize >= 1 && pageSize <= MAX_PAGESIZE, BAD_PAGESIZEMSG);
		
		this.pageSize.set(pageSize);
	}

	@Override
	public void store(final Document document) {
		Validate.notNull(document, "document cannot be null");
		ensureNotFaulted();
		
		final SolrInputDocument doc = toSolrInputDocument(document);

		synchronized(batch) {
			batch.add(doc);
			updateIfNeeded();
		}
	}

	@Override
	public void removeByKey(final Untokenized pk) {
		Validate.notNull(pk, "pk cannot be null");
		ensureNotFaulted();
		
		synchronized(batch) {
			batch.deleteById(pk.getText());
			updateIfNeeded();
		}
	}

	@Override
	public void removeByQuery(final String search) {
		Validate.notNull(search, "search cannot be null");
		ensureNotFaulted();

		synchronized(batch) {
			batch.deleteByQuery(search);
			updateIfNeeded();
		}
	}

	@Override
	public void commit() throws DelayedIndexingException {
		try {
			synchronized(batch) {
				batch.setCommitFlag();
				try {
					batch.dump(solr);
				} finally {
					batch.clear();
					lastUpdateError.set(null);
				}
			}
		} catch (final SolrServerException|IOException|RuntimeException ex) {
			LOG.warn("Error updating solr {}", ex);
			throw new DelayedIndexingException(ex);
		}
	}
	
	@Override
	public SearchResults search(final String search, final Collection<Field> returnFields, final int offset)
	{
		Validate.notNull(search, "search cannot be null");
		Validate.notNull(returnFields, "returnFields cannot be null");
				
		final SolrQuery solrQuery = toSolrQuery(search, returnFields);
		return new SearchResultsImpl(solr, solrQuery, offset, pageSize);
	}


	/**
	 * In order to prevent batch from growing and growing and growing till the time it simply won't come through the
	 * network, this method ensures that actual batch size (which can go over limit in case of, say, network problems)
	 * is no more than twice the limit
	 */
	private void ensureNotFaulted() {
		final Exception fault = lastUpdateError.get();
		if (fault != null && (batch.currentBatchBytes()>=2*batchBytes || batch.currentBatchSize()>=2*batchSize)) {
			throw new IndexingException("FAULTED", fault);
		}
	}
	
	private SolrQuery toSolrQuery(final String search, final Collection<Field> returnFields) {
		final SolrQuery query = new SolrQuery(search);
		final String [] fields = new String[1+returnFields.size()];
		int i=0;
		fields[i++]="pk";
		for (final Field field : returnFields) {
			fields[i++] = field.getName();
		}
		query.setFields(fields);
		return query;
	}

	private SolrInputDocument toSolrInputDocument(final Document document) {
		document.freeze();
		
		final SolrInputDocument ret = new SolrInputDocument();
		ret.setField("pk", document.getPk().toString());
		
		int totalBytes = 0;
		for (final FieldValue fv : document.getFields()) {
			final Field field = fv.getField();
			final String fieldName = field.getName();
			totalBytes += fieldName.length();
			
			final Object solrValue = toSolrValue(fv.getValue());
			if (ret.containsKey(fieldName)) {
				throw new IndexingException("Cannot add same field twice: "+fieldName);
			} else {
				ret.setField(fieldName, solrValue);
				totalBytes += fieldName.length();
				totalBytes += sizeof(solrValue);
			}
		}
		ret.setField("size", totalBytes);
		
		return ret;
	}
	
	private int sizeof(final Object solrValue) {
		if (solrValue == null) {
			return 0;
		} else if (solrValue instanceof String) {
			return ((String)solrValue).length()*2; //UTF_16
		} else if (solrValue instanceof Integer) {
			return 4;
		} else if (solrValue instanceof Long) {
			return 8;
		} else if (solrValue instanceof Float) {
			return 4;
		} else if (solrValue instanceof Double) {
			return 10;
		} else if (solrValue instanceof java.util.Date) {
			return 30; // ISO8601
		} else if (solrValue instanceof Collection) {
			final Collection<?> asColl = (Collection<?>) solrValue;
			int ret = 0;
			for (final Object o : asColl) {
				ret += sizeof(o);
			}
			return ret;
		} else {
			final String asStr = solrValue.toString();
			LOG.info("Cannot determine size of {} {}, using toString()", solrValue.getClass(), solrValue);
			return asStr.length()*2;
		}
	}

	private Object toSolrValue(final Object value) {
		if (value instanceof Collection) {
			final Collection<?> asColl = (Collection<?>)value;
			if (!asColl.isEmpty()) {
				// Presumably, all values have same type (enforced by FieldValue)
				final Object first = asColl.iterator().next();
				final Object firsTranslated = toScalarSolrValue(first);
				final boolean translationRequired = first != firsTranslated;
				
				if (translationRequired) {
					final List<Object> translated = new ArrayList<>(asColl.size());
					translated.add(firsTranslated);
					final Iterator<?> source = asColl.iterator();
					source.next(); // Skip first
					while (source.hasNext()) {
						translated.add(toScalarSolrValue(source.next()));
					}
					return translated;
				}
			}
			return asColl;
		} else {
			return toScalarSolrValue(value);
		}
	}
	
	private Object toScalarSolrValue(final Object value) {
		if (value instanceof Binary) {
			return ((Binary)value).getData();
		} else if (value instanceof Untokenized) {
			return ((Untokenized)value).getText();
		} else {
			return value;
		}
	}

	private void updateIfNeeded() {
		if (batch.currentBatchBytes() >= batchBytes || batch.currentBatchSize()>=batchSize) {
			try {
				batch.dump(solr);
				batch.clear();
			} catch (final SolrServerException|IOException|RuntimeException ex) {
				lastUpdateError.set(ex);
				LOG.warn("Error updating solr {}", ex);
				throw new IndexingException(ex);
			}
		}
	}
	
	private static final class Batch {
		private final UpdateRequest upd = new UpdateRequest();
		private int batchBytes;
		
		public void add(final SolrInputDocument doc) {
			upd.add(doc);
			final Integer size = (Integer)doc.getFieldValue("size");
			batchBytes += size;
		}

		public void dump(final SolrClient solr) throws SolrServerException, IOException {
			solr.update(upd);
			if (LOG.isDebugEnabled()) {
				LOG.debug("DUMP {} docs {} deleteIds {} deleteQueries {} bytes", 
						new Object[] {safeSize(upd.getDocuments()), safeSize(upd.getDeleteById()), 
						              safeSize(upd.getDeleteQuery()), batchBytes});
			}
		}

		public void clear() {
			upd.clear();
			upd.setAction(null, false, false);
			batchBytes = 0;
		}

		public void setCommitFlag() {
			upd.setAction(ACTION.COMMIT, true, true);
		}

		public void deleteByQuery(final String search) {
			upd.deleteByQuery(search);
			batchBytes += search.length();
		}

		public void deleteById(final String id) {
			upd.deleteById(id);
			batchBytes += id.length();
		}
		
		public int currentBatchSize() {
			return safeSize(upd.getDocuments()) + safeSize(upd.getDeleteById()) + safeSize(upd.getDeleteQuery());
		}
		
		public int currentBatchBytes() {
			return batchBytes;
		}
		
		private int safeSize(final Collection<?> coll) {
			return coll == null ? 0 : coll.size();
		}
	}
	
	/** @visiblefortesting */
	static int modifyPageSize_Safe(int pageSize, int grow) {
		int result = pageSize + grow;
		if (result <= 0) {
			result = 1;
		} else if (result > MAX_PAGESIZE) {
			result = MAX_PAGESIZE;
		}
		return result;
	}

	private static final class SearchResultsImpl implements SearchResults {
		private static final List<String> SOLR_SPECIAL_FIELDS = Arrays.asList("pk", "_version_");
		private final SolrClient solr;
		private final SolrQuery query;
		private final AtomicInteger globalPageSize;
		private int currentPageSize;
		private int offset;
		private int totalFetched;
		private QueryResponse lastResult;
		private boolean hasMore = true;
		private Iterator<SolrDocument> lastResultIterator;
		private boolean closed;


		public SearchResultsImpl(
				final SolrClient solr, 
				final SolrQuery query, 
				final int offset,
				final AtomicInteger pageSize
		) {
			this.solr = solr;
			this.query = query;
			this.offset = offset;
			this.globalPageSize = pageSize;
			
			setCurrentPageSize(globalPageSize.get());
			
		}
		
		private void setCurrentPageSize(int size) {
			currentPageSize = size;
			query.setRows(size);
		}

		@Override
		public long estimateTotalResults() {
			init();
			return lastResult.getResults().getNumFound();
		}

		@Override
		public Document next() {
			if (closed) {
				return null;
			}
			init();
			if (!lastResultIterator.hasNext()) {
				if (!hasMore) {
					closed = true;
					return null;
				} else {
					nextQuery();

					if (!lastResultIterator.hasNext()) {
						closed = true;
						return null;
					}
				}
			}
			Validate.isTrue(lastResultIterator.hasNext());
			final SolrDocument doc = lastResultIterator.next();
			totalFetched++;
			return toApiDocument(doc);
		}

		private void growCurrentPageSize(int grow) {
			currentPageSize = modifyPageSize_Safe(currentPageSize, grow);
			final int oldGlobal = globalPageSize.get();
			int globalGrow = grow / 2;
			if (globalGrow == 0) {
				globalGrow = grow;
			}
			final int newGlobal = modifyPageSize_Safe(oldGlobal, globalGrow);
			globalPageSize.compareAndSet(oldGlobal, newGlobal); // Don't care if fails
			
			setCurrentPageSize(currentPageSize);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			if (totalFetched<currentPageSize) {
				growCurrentPageSize(totalFetched-currentPageSize);
			}
		}
		
		public void init() {
			if (lastResult == null) {
				nextQuery();
			}
		}

		private void nextQuery() {
			if (lastResult != null) {
				growCurrentPageSize(currentPageSize);
			}
			query.setStart(offset);
			
			try {
				lastResult = solr.search(query);
			} catch (final SolrServerException | IOException ex) {
				throw new SearchException("error when fetching from offset="+offset+", pageSize="+
						query.getRows(), ex);
			}
			final SolrDocumentList documents = lastResult.getResults();
			hasMore = documents.size()>=currentPageSize;
			lastResultIterator = documents.iterator();
			offset += documents.size();
		}

		private Document toApiDocument(final SolrDocument doc) {
			final Untokenized pk = new Untokenized((String)doc.getFieldValue("pk"));
			final Document ret = new Document(pk);
			for (final String name: doc.getFieldNames()) {
				if (!SOLR_SPECIAL_FIELDS.contains(name)) {
					Object value = doc.getFieldValue(name);
					final Field field = new Field(name);
					if (field.getType().javaType.isAssignableFrom(Untokenized.class) && value instanceof String) {
						value = new Untokenized((String) value);
					}
					FieldValue fv;
					try {
						fv = value instanceof Collection ? new FieldValue(field, (Collection<?>)value) :
							new FieldValue(field, value);
						
						fv.validate();
					} catch (final FieldValueInvalidException ex) {
						LOG.warn("Can't create FieldValue from {}={}", name, value, ex);
						fv = null;
					}
					if (fv != null) {
						ret.add(fv);
					}
				}
			}
			return ret;
		}
	}
}
