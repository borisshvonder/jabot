package jabot.jindex;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Document;
import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;
import jabot.idxapi.Index;
import jabot.idxapi.SearchResults;
import jabot.idxapi.Untokenized;

public class DefaultJindexTest {
	@Mock
	Index index;
	
	@Mock
	SearchResults results;
	
	private DefaultJindex fixture;
	private TestBean bean;
	private ArgumentCaptor<Document> documentCaptor;
	private ArgumentCaptor<String> queryCaptor;
	private Query query;
	
	@Before
	public void setUp() throws ParseException {
		MockitoAnnotations.initMocks(this);
		fixture = new DefaultJindex(index);
		
		bean = new TestBean();
		documentCaptor = ArgumentCaptor.forClass(Document.class);
		queryCaptor = ArgumentCaptor.forClass(String.class);
		query = Parser.DEFAULT.parse("search");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test_store_and_search() {
		bean.setIntVal(100);
		bean.setUri(URI.create("file:/some/file"));
		fixture.store(new Untokenized("pk1"), bean);
		verify(index).store(documentCaptor.capture());
		
		final Document doc = documentCaptor.getValue();
		
		when(index.search(queryCaptor.capture(), notNull(Collection.class), eq(10))).thenReturn(results);
		when(results.next()).thenReturn(doc).thenReturn(null);
		
		final JIndexResults<TestBean> searchResults = fixture.search(TestBean.class, query, 10);
		final TestBean restored = searchResults.next();
		Assert.assertEquals(bean.getIntVal(), restored.getIntVal());
		Assert.assertEquals(bean.getUri(), restored.getUri());
		Assert.assertNull(searchResults.next());
		
		Assert.assertEquals("+class_unt_f:jabot.jindex.DefaultJindexTest$TestBean "
				          + "+(metadata:search metadataRu:search)", 
				queryCaptor.getValue());
	}
	
	@Test
	public void test_JIndexResults() throws IOException {
		when(index.search(queryCaptor.capture(), notNull(Collection.class), eq(0))).thenReturn(results);
		when(results.estimateTotalResults()).thenReturn(100L);
		
		final JIndexResults<TestBean> searchResults = fixture.search(TestBean.class, query, 0);
		Assert.assertEquals(100L, searchResults.estimateTotalResults());
		searchResults.close();
		
		verify(results).close();
	}
	
	@Test
	public void test_removeByKey() throws ParseException {
		fixture.removeByKey(new Untokenized("primaryKey"));
		verify(index).removeByKey(new Untokenized("primaryKey"));
	}
	
	@Test
	public void test_removeByQuery() throws ParseException {
		fixture.removeByQuery(TestBean.class, query);
		verify(index).removeByQuery(queryCaptor.capture());
		
		Assert.assertEquals("+class_unt_f:jabot.jindex.DefaultJindexTest$TestBean "
				          + "+(metadata:search metadataRu:search)", 
				queryCaptor.getValue());
	}
	
	@Test
	public void test_commit() throws ParseException, DelayedIndexingException {
		fixture.commit();
		verify(index).commit();
	}
	
	static final class TestBean {
		static {
			final ModelIntrospector<TestBean> introspector = new ModelIntrospector<>(TestBean.class);
			introspector.getMapping("intVal").setStorage(Storage.STORED);
			introspector.getMapping("stringVal").setStorage(Storage.STORED_INDEXED);
			introspector.getMapping("stringValRu").setStorage(Storage.STORED_INDEXED).setType(Type.STRING_RU);
			introspector.getMapping("uri").setStorage(Storage.STORED_INDEXED);
			ModelMappersInventory.registerMapper(introspector.buildMapper());
		}
		private int intVal;
		private String stringVal;
		private String stringValRu;
		private URI uri;

		public int getIntVal() {
			return intVal;
		}

		public void setIntVal(int intVal) {
			this.intVal = intVal;
		}

		public String getStringVal() {
			return stringVal;
		}

		public void setStringVal(String stringVal) {
			this.stringVal = stringVal;
		}

		public String getStringValRu() {
			return stringValRu;
		}

		public void setStringValRu(String stringValRu) {
			this.stringValRu = stringValRu;
		}

		public URI getUri() {
			return uri;
		}

		public void setUri(URI uri) {
			this.uri = uri;
		}
		
	}
}
