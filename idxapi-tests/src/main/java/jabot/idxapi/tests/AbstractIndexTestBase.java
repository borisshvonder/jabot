package jabot.idxapi.tests;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Document;
import jabot.idxapi.FieldValue;
import jabot.idxapi.Index;
import jabot.idxapi.SearchResults;
import jabot.idxapi.Untokenized;

/**
 * Base for Search API acceptance tests
 *
 */
public abstract class AbstractIndexTestBase {
	protected abstract Index create() throws Exception;
	protected abstract void destroy(Index idx) throws Exception;
	
	private Index fixture;
	private Document sample1;
	private Document sample2;
	
	@Before
	public void setUp() throws Exception {
		fixture = create();
		
		sample1 = new Document(new Untokenized("sample1"));
		sample1.add(new FieldValue("integer_int", 1));
		sample1.add(new FieldValue("lng_long", 100L));
		sample1.add(new FieldValue("str_unt", new Untokenized("string1")));
		sample1.add(new FieldValue("txt_str", "token tokan1"));
		sample1.add(new FieldValue("txtRu_strru", "русский язык"));

		sample2 = new Document(new Untokenized("sample2"));
		sample2.add(new FieldValue("integer_int", 2));
		sample2.add(new FieldValue("lng_long", 200L));
		sample2.add(new FieldValue("str_unt", new Untokenized("string2")));
		sample2.add(new FieldValue("txt_str", "token tokun2"));
		sample2.add(new FieldValue("txtRu_strru", "иэтотоже русский"));
	}
	
	@After
	public void tearDown() throws Exception {
		destroy(fixture);
	}
	
	@Test
	public void testStoreOneDocument() throws DelayedIndexingException {
		fixture.store(sample1);
		fixture.commit();
	}
	
	@Test
	public void testFindOneDocument() throws DelayedIndexingException, IOException {
		fixture.store(sample1);
		fixture.commit();
		try(final SearchResults sr = fixture.search("pk:"+sample1.getPk(), 0)) {
			Assert.assertEquals(sample1.getPk(), sr.next().getPk());
			Assert.assertNull(sr.next());
		}
	}

	@Test
	public void testFindDocumentByField() throws DelayedIndexingException, IOException {
		fixture.store(sample1);
		fixture.store(sample2);
		fixture.commit();
		
		assertDocumentsFound("*:*", sample1, sample2);
		assertDocumentsFound("pk:"+sample1.getPk(), sample1);
		assertDocumentsFound("pk:"+sample2.getPk(), sample2);
		
		assertDocumentsFound("integer_int:1", sample1);
		assertDocumentsFound("integer_int:2", sample2);
		assertDocumentsFound("integer_int:3");
		
		assertDocumentsFound("lng_long:100", sample1);
		assertDocumentsFound("lng_long:200", sample2);
		assertDocumentsFound("lng_long:300");
		
		assertDocumentsFound("str_unt:string1", sample1);
		assertDocumentsFound("str_unt:string2", sample2);
		assertDocumentsFound("str_unt:string3");

		assertDocumentsFound("txt_str:token", sample1, sample2);
		assertDocumentsFound("txt_str:tokan1", sample1);
		assertDocumentsFound("txt_str:tokun2", sample2);
		assertDocumentsFound("txt_str:tokeng3");
		
		assertDocumentsFound("txtRu_strru:русский", sample1, sample2);
		assertDocumentsFound("txtRu_strru:язык", sample1);
		assertDocumentsFound("txtRu_strru:иэтотоже", sample2);
		assertDocumentsFound("txtRu_strru:чертиче");
	}

	@Test
	public void test_removeByKey() throws DelayedIndexingException, IOException {
		fixture.store(sample1);
		fixture.store(sample2);
		fixture.commit();
		assertDocumentsFound("*:*", sample1, sample2);
		fixture.removeByKey(sample1.getPk());
		fixture.commit();
		assertDocumentsFound("*:*", sample2);
	}
	
	@Test
	public void test_removeByQuery() throws DelayedIndexingException, IOException {
		fixture.store(sample1);
		fixture.store(sample2);
		fixture.commit();
		assertDocumentsFound("*:*", sample1, sample2);
		fixture.removeByQuery("pk:"+sample1.getPk().getText());
		fixture.commit();
		assertDocumentsFound("*:*", sample2);
	}
	
	private void assertDocumentsFound(
			final String search, 
			final Document ... documents
	) throws IOException {
		final Set<Untokenized> actualPks = new HashSet<>();

		final long totalResults;
		try(final SearchResults sr = fixture.search(search, 0)) {
			totalResults = sr.estimateTotalResults();
			Document found = sr.next();
			while (found != null) {
				actualPks.add(found.getPk());
				found = sr.next();
			}
		}
		final Set<Untokenized> expectedPks = new HashSet<>();
		for (final Document doc : documents) {
			expectedPks.add(doc.getPk());
		}
		
		Assert.assertEquals(expectedPks, actualPks);
		Assert.assertEquals("Estimated total results dont't match", documents.length, totalResults);
	}
}
