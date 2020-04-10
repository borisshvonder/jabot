package jabot.idxsolr;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jabot.idxapi.Binary;
import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Document;
import jabot.idxapi.Field;
import jabot.idxapi.FieldValue;
import jabot.idxapi.IndexingException;
import jabot.idxapi.SearchResults;
import jabot.idxapi.Untokenized;

public class SolrIndexTest {
	@Mock
	SolrClient client;
	
	private SolrIndex fixture;
	private Document sample1;
	private Document sample2;
	
	private ArgumentCaptor<SolrQuery> queryCaptor;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		initFixture(client);
		
		sample1 = new Document(new Untokenized("sample1"));
		sample1.add(new FieldValue("integer_int", 1));
		sample1.add(new FieldValue("lng_long", 100L));
		sample1.add(new FieldValue("str_unt", new Untokenized("string1")));
		sample1.add(new FieldValue("txt_str", "token token1"));
		sample1.add(new FieldValue("intList_int_mv", Arrays.asList(1)));
		sample1.add(new FieldValue("binList_binary_mv", Arrays.asList(new Binary(new byte[1]))));

		sample2 = new Document(new Untokenized("sample2"));
		sample2.add(new FieldValue("integer_int", 2));
		sample2.add(new FieldValue("lng_long", 200L));
		sample2.add(new FieldValue("str_unt", new Untokenized("string2")));
		sample2.add(new FieldValue("txt_str", "token token2"));
		sample2.add(new FieldValue("intList_int_mv", Arrays.asList(2, 3)));
		final List<Binary> binList = Arrays.asList(new Binary(new byte[2]), new Binary(new byte[3]));
		sample2.add(new FieldValue("binList_binary_mv", binList));
		
		queryCaptor = ArgumentCaptor.forClass(SolrQuery.class);
	}
	
	@Test
	public void test_store_delayed() throws SolrServerException, IOException, DelayedIndexingException {
		fixture.setBatchSize(100);
		fixture.store(sample1);
		verify(client, never()).update(any());
		fixture.commit();
		verify(client).update(notNull(UpdateRequest.class));
	}
	
	@Test
	public void test_store_immediate() throws SolrServerException, IOException {
		fixture.store(sample1);
		verify(client).update(notNull(UpdateRequest.class));
	}
	
	@Test
	public void test_removeByKey_delayed() throws SolrServerException, IOException, DelayedIndexingException {
		fixture.setBatchSize(100);
		fixture.removeByKey(new Untokenized("pk1"));
		verify(client, never()).update(any());
		fixture.commit();
		verify(client).update(notNull(UpdateRequest.class));
	}

	@Test
	public void test_removeByKey_immediate() throws SolrServerException, IOException {
		fixture.removeByKey(new Untokenized("pk1"));
		verify(client).update(notNull(UpdateRequest.class));
	}

	@Test
	public void test_removeByQuery_delayed() throws SolrServerException, IOException, DelayedIndexingException {
		fixture.setBatchSize(100);
		fixture.removeByQuery("query1");
		verify(client, never()).update(any());
		fixture.commit();
		verify(client).update(notNull(UpdateRequest.class));
	}

	@Test
	public void test_removeByQuery_immediate() throws SolrServerException, IOException {
		fixture.removeByQuery("query1");
		verify(client).update(notNull(UpdateRequest.class));
	}
	
	@Test
	public void test_store() throws SolrServerException, IOException {
		final TestSolrClient client = withTestClient();
		
		fixture.store(sample1);
		Assert.assertEquals(1, client.documents.size());
		fixture.store(sample2);
		Assert.assertEquals(2, client.documents.size());
		
		final SolrInputDocument doc1 = client.documents.get(0);
		for (final FieldValue fv : sample1.getFields()) {
			Assert.assertNotNull(fv.getField().getName()+" should be present", 
					doc1.getFieldValue(fv.getField().getName()));
		}
		Assert.assertEquals(1, doc1.getFieldValue("integer_int"));
		Assert.assertEquals(100L, doc1.getFieldValue("lng_long"));
		Assert.assertEquals("string1", doc1.getFieldValue("str_unt"));
		Assert.assertEquals("token token1", doc1.getFieldValue("txt_str"));

		final SolrInputDocument doc2 = client.documents.get(1);
		for (final FieldValue fv : sample1.getFields()) {
			Assert.assertNotNull(fv.getField().getName()+" should be present", 
					doc2.getFieldValue(fv.getField().getName()));
		}
		Assert.assertEquals(2, doc2.getFieldValue("integer_int"));
		Assert.assertEquals(200L, doc2.getFieldValue("lng_long"));
		Assert.assertEquals("string2", doc2.getFieldValue("str_unt"));
		Assert.assertEquals("token token2", doc2.getFieldValue("txt_str"));
	}
	
	@Test(expected=IndexingException.class)
	public void test_cannot_store_same_scalar_field_twice() throws SolrServerException, IOException {
		sample1.add(new FieldValue("integer_int", 2));
		fixture.store(sample1);
	}
	
	@Test(expected=IndexingException.class)
	public void test_cannot_store_same_list_field_twice() throws SolrServerException, IOException {
		sample1.add(new FieldValue("intList_int_mv", Arrays.asList(1, 3)));
		fixture.store(sample1);
	}
	
	@Test
	public void test_removeByKey() {
		final TestSolrClient client = withTestClient();

		fixture.removeByKey(new Untokenized("key1"));
		Assert.assertEquals(Arrays.asList("key1"), client.removeByKey);
	}
	
	@Test
	public void test_removeByQuery() {
		final TestSolrClient client = withTestClient();

		fixture.removeByQuery("query1");
		Assert.assertEquals(Arrays.asList("query1"), client.removeByQuery);
	}
	
	@Test(expected=IndexingException.class)
	public void test_exception_on_store() throws SolrServerException, IOException {
		when(client.update(any())).thenThrow(new IOException());
		fixture.store(sample1);
	}
	
	@Test(expected=DelayedIndexingException.class)
	public void test_exception_on_commit() throws SolrServerException, IOException, DelayedIndexingException {
		when(client.update(any())).thenThrow(new IOException());
		
		fixture.setBatchSize(100);
		fixture.store(sample1);
		fixture.commit();
	}
	
	@Test
	public void test_search() throws SolrServerException, IOException {
		final SolrDocument solrDoc = new SolrDocument();
		solrDoc.setField("pk", "pk1");

		final QueryResponse resp1 = buildResponse(solrDoc);
		resp1.getResults().setNumFound(2);
		
		when(client.search(solrQuery("q=search&fl=pk&rows=1&start=10"))).thenReturn(resp1);
		when(client.search(solrQuery("q=search&fl=pk&rows=2&start=11"))).thenReturn(buildResponse());
		
		SearchResults results = fixture.search("search", 10);
		final Document doc = results.next();
		
		Assert.assertEquals(2, results.estimateTotalResults());
		Assert.assertEquals(new Untokenized("pk1"), doc.getPk());
		Assert.assertNull(results.next());
	}
	
	@Test
	public void test_search_paging() throws SolrServerException, IOException {
		final SolrDocument solrDoc1 = new SolrDocument();
		solrDoc1.setField("pk", "pk1");

		final SolrDocument solrDoc2 = new SolrDocument();
		solrDoc2.setField("pk", "pk2");
		
		final SolrDocument solrDoc3 = new SolrDocument();
		solrDoc3.setField("pk", "pk3");
		
		fixture.setPageSize(2);

		when(client.search(solrQuery("q=search&fl=pk&rows=2&start=0"))).thenReturn(buildResponse(solrDoc1, solrDoc2));
		when(client.search(solrQuery("q=search&fl=pk&rows=4&start=2"))).thenReturn(buildResponse(solrDoc3));
		
		SearchResults results = fixture.search("search", 0);
		Assert.assertEquals(new Untokenized("pk1"), results.next().getPk());
		Assert.assertEquals(new Untokenized("pk2"), results.next().getPk());
		Assert.assertEquals(new Untokenized("pk3"), results.next().getPk());
		Assert.assertNull(results.next());

		verify(client, times(2)).search(any());
	}
	
	@Test
	public void test_increases_pageSize_when_moreDataAvailable_fetched() 
			throws SolrServerException, IOException 
	{
		final SolrDocument solrDoc1 = new SolrDocument();
		solrDoc1.setField("pk", "pk1");

		final SolrDocument solrDoc2 = new SolrDocument();
		solrDoc2.setField("pk", "pk2");
		
		final SolrDocument solrDoc3 = new SolrDocument();
		solrDoc3.setField("pk", "pk3");
		
		final SolrDocument solrDoc4 = new SolrDocument();
		solrDoc4.setField("pk", "pk4");

		fixture.setPageSize(1);

		when(client.search(solrQuery("q=search&fl=pk&rows=1&start=0"))).thenReturn(
				buildResponse(solrDoc1)
		);
		when(client.search(solrQuery("q=search&fl=pk&rows=2&start=1"))).thenReturn(
				buildResponse(solrDoc2, solrDoc3)
		);

		when(client.search(solrQuery("q=search&fl=pk&rows=4&start=3"))).thenReturn(
				buildResponse(solrDoc4)
		);
		
		try (SearchResults results = fixture.search("search", 0)) {
			Assert.assertEquals(new Untokenized("pk1"), results.next().getPk());
			Assert.assertEquals(1, fixture.getPageSize());
			Assert.assertEquals(new Untokenized("pk2"), results.next().getPk());
			Assert.assertEquals(2, fixture.getPageSize());
			Assert.assertEquals(new Untokenized("pk3"), results.next().getPk());
			Assert.assertEquals(2, fixture.getPageSize());
			Assert.assertEquals(new Untokenized("pk4"), results.next().getPk());
			Assert.assertEquals(3, fixture.getPageSize());
		}
		
		Assert.assertEquals(3, fixture.getPageSize());
	}
	
	@Test
	public void test_reduces_pageSize_when_notEverything_fetched() throws SolrServerException, IOException {
		final SolrDocument solrDoc1 = new SolrDocument();
		solrDoc1.setField("pk", "pk1");

		final SolrDocument solrDoc2 = new SolrDocument();
		solrDoc2.setField("pk", "pk2");
		
		final SolrDocument solrDoc3 = new SolrDocument();
		solrDoc3.setField("pk", "pk3");
		
		final SolrDocument solrDoc4 = new SolrDocument();
		solrDoc4.setField("pk", "pk4");

		fixture.setPageSize(4);

		when(client.search(solrQuery("q=search&fl=pk&rows=4&start=0"))).thenReturn(
				buildResponse(solrDoc1, solrDoc2, solrDoc3, solrDoc4)
		);
		try (SearchResults results = fixture.search("search", 0)) {
			Assert.assertEquals(new Untokenized("pk1"), results.next().getPk());
			Assert.assertEquals(new Untokenized("pk2"), results.next().getPk());
		}
		
		Assert.assertEquals(3, fixture.getPageSize());
	}
	
	@Test
	public void test_close_search_prematurely() throws SolrServerException, IOException {
		final SolrDocument solrDoc1 = new SolrDocument();
		solrDoc1.setField("pk", "pk1");

		final SolrDocument solrDoc2 = new SolrDocument();
		solrDoc2.setField("pk", "pk2");
		
		final SolrDocument solrDoc3 = new SolrDocument();
		solrDoc3.setField("pk", "pk3");
		
		fixture.setPageSize(2);

		when(client.search(solrQuery("q=search&fl=pk&rows=2&start=0"))).thenReturn(buildResponse(solrDoc1, solrDoc2));
		when(client.search(solrQuery("q=search&fl=pk&rows=2&start=2"))).thenReturn(buildResponse(solrDoc3));
		
		SearchResults results = fixture.search("search", 0);
		Assert.assertEquals(new Untokenized("pk1"), results.next().getPk());
		results.close();
		Assert.assertNull(results.next());

		verify(client, times(1)).search(any());
	}
	
	@Test
	public void test_search_with_fields() throws SolrServerException, IOException {
		final SolrDocument solrDoc = new SolrDocument();
		solrDoc.setField("pk", "pk1");
		solrDoc.setField("str_f_unt", new Untokenized("str1"));
		solrDoc.setField("list_f_int_mv", Arrays.asList(1, 2));

		when(client.search(solrQuery("q=search&fl=pk,str_f_unt,list_f_int_mv&rows=1&start=0")))
			.thenReturn(buildResponse(solrDoc));
		
		when(client.search(solrQuery("q=search&fl=pk,str_f_unt,list_f_int_mv&rows=2&start=1")))
			.thenReturn(buildResponse());
		
		List<Field> fields = Arrays.asList(new Field("str_f_unt"), new Field("list_f_int_mv"));
		SearchResults results = fixture.search("search", fields, 0);
		final Document doc = results.next();
		
		Assert.assertEquals(new Untokenized("pk1"), doc.getPk());
		Assert.assertEquals(new Untokenized("str1"), doc.getValue("str_f_unt").getValue());
		Assert.assertEquals(Arrays.asList(1, 2), doc.getValue("list_f_int_mv").getValue());
		Assert.assertNull(results.next());
	}
	
	
	@Test
	public void test_modifyPageSize_Safe() throws SolrServerException, IOException {
		Assert.assertEquals(1, SolrIndex.modifyPageSize_Safe(0, 0));
		Assert.assertEquals(1, SolrIndex.modifyPageSize_Safe(0, 1));
		Assert.assertEquals(3, SolrIndex.modifyPageSize_Safe(2, 1));
		Assert.assertEquals(1, SolrIndex.modifyPageSize_Safe(2, -100));
		
		Assert.assertEquals(SolrIndex.MAX_PAGESIZE, 
				SolrIndex.modifyPageSize_Safe(100, SolrIndex.MAX_PAGESIZE-10));
	}
	
	private QueryResponse buildResponse(final SolrDocument ... docs) {
		final QueryResponse ret = new QueryResponse();
		final SolrDocumentList documents = new SolrDocumentList();
		documents.setNumFound(docs.length);
		final NamedList<Object> response = new NamedList<>();
		response.add("response", documents);
		for (final SolrDocument doc : docs) {
			documents.add(doc);
		}
		
		ret.setResponse(response);
		return ret;
	}
	
	private static SolrQuery solrQuery(final String query) {
		return (SolrQuery)argThat(new SolrQueryMatcher(query));
	}

	private TestSolrClient withTestClient() {
		final TestSolrClient client = new TestSolrClient();
		initFixture(client);
		return client;
	}
	
	private void initFixture(SolrClient client) {
		fixture = new SolrIndex(client);
		fixture.setBatchSize(1);
		Assert.assertEquals(1,  fixture.getBatchSize());

		fixture.setPageSize(1);
		Assert.assertEquals(1,  fixture.getPageSize());
	}
	
	private static final class TestSolrClient implements SolrClient {
		private final List<String> queries = new LinkedList<>();
		private final List<SolrInputDocument> documents = new LinkedList<>();
		private final List<String> removeByKey = new LinkedList<>();
		private final List<String> removeByQuery = new LinkedList<>();
		
		@Override
		public QueryResponse search(final SolrQuery query) throws SolrServerException, IOException {
			queries.add(query.toString());
			return null;
		}

		@Override
		public NamedList<Object> update(final UpdateRequest req) throws SolrServerException, IOException {
			add(req.getDocuments(), documents);
			add(req.getDeleteById(), removeByKey);
			add(req.getDeleteQuery(), removeByQuery);
			return null;
		}
		
		private static<E> void add(Collection<? extends E> from, Collection<E> to) {
			if (from != null) {
				to.addAll(from);
			}
		}
	}
	
	private static class SolrQueryMatcher extends ArgumentMatcher {
		private final String queryStr;
		
		public SolrQueryMatcher(final String queryStr) {
			this.queryStr = queryStr;
		}

		@Override
		public boolean matches(final Object argument) {
			if (argument instanceof SolrQuery) {
				final SolrQuery q = (SolrQuery)argument;
				final String asStr = q.toString();
				return queryStr.equals(asStr);
			}
			return false;
		}
		
	}
}
