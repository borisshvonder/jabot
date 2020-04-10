package jabot.jindex;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jabot.idxapi.DelayedIndexingException;
import jabot.idxapi.Untokenized;

public class DefaultCompositeJindexTest {
	private static final Untokenized PK1 = new Untokenized("pk1");
			
	@Mock Jindex component1, component2, component3;
	
	@Mock Query query1, query2, query3, query4, query5;
	
	@Mock JIndexResults<Integer> results1, results2;
	
	private DefaultCompositeJindex fixture;
	
	

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		fixture = new DefaultCompositeJindex(2);
		
		final Map<String, String> params1 = new HashMap<>();
		fixture.addComponent("component1", component1, params1);
		
		final Map<String, String> params2 = new HashMap<>();
		params2.put("handles", "java.lang.Integer, java.lang.Long");
		fixture.addComponent("component2", component2, params2);

		final Map<String, String> params3 = new HashMap<>();
		params3.put("handles", "java.lang.Integer, java.lang.Float");
		fixture.addComponent("component3", component3, params3);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void cannot_add_two_components_with_same_id() {
		fixture.addComponent("component1", component1, Collections.emptyMap());
	}

	@Test
	public void testEmpty() {
		fixture = new DefaultCompositeJindex(2);
		Assert.assertEquals(0, fixture.getAllComponents().size());
		Assert.assertNull(fixture.getDefaultComponent());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_getComponent_missing_willThrow() {
		fixture = new DefaultCompositeJindex(2);
		fixture.getComponent("missing");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_getComponent_missing_willThrow_evenWhenNotEmpty() {
		fixture.getComponent("missing");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_handles_for_missing_component_will_throw() {
		fixture = new DefaultCompositeJindex(2);
		fixture.handles("missing");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_handles_for_missing_component_will_throw_even_if_not_empty() {
		fixture.handles("missing");
	}
	
	@Test
	public void test_store_default() {
		final Object toStore = new Object();
		fixture.store(PK1, toStore);
		
		verify(component1).store(PK1, toStore);
		verify(component2, never()).store(any(), any());
		verify(component3, never()).store(any(), any());
	}
	
	@Test
	public void test_store_specific() {
		final Float toStore = new Float(1.0f);
		fixture.store(PK1, toStore);
		
		verify(component1, never()).store(any(), any());
		verify(component2, never()).store(any(), any());
		verify(component3).store(PK1, toStore);
	}
	
	@Test
	public void test_store_specific_into_default() {
		final String toStore = new String("aaa");
		fixture.store(PK1, toStore);
		
		verify(component1).store(PK1, toStore);
		verify(component2, never()).store(any(), any());
		verify(component3, never()).store(any(), any());
	}
	
	@Test
	public void test_store_first() {
		final Integer toStore = new Integer(1);
		fixture.store(PK1, toStore);
		
		verify(component1, never()).store(any(), any());
		verify(component2).store(PK1, toStore);
		verify(component3, never()).store(any(), any());
	}
	
	@Test
	public void test_removeByKey() {
		fixture.removeByKey(PK1);
		
		verify(component1).removeByKey(PK1);
		verify(component2).removeByKey(PK1);
		verify(component3).removeByKey(PK1);
	}
	
	@Test
	public void test_removeByQuery() {
		fixture.removeByQuery(Object.class, query1);
		fixture.removeByQuery(String.class, query2);
		fixture.removeByQuery(Integer.class, query3);
		fixture.removeByQuery(Float.class, query4);
		
		verify(component1).removeByQuery(Object.class, query1);
		verify(component1).removeByQuery(String.class, query2);
		verify(component2).removeByQuery(Integer.class, query3);
		verify(component3).removeByQuery(Integer.class, query3);
		verify(component3).removeByQuery(Float.class, query4);
	}
	
	@Test
	public void test_commit() throws DelayedIndexingException {
		fixture.commit();
		
		verify(component1).commit();
		verify(component2).commit();
		verify(component3).commit();
	}
	
	@Test
	public void test_commitThrowsException() throws DelayedIndexingException {
		Mockito.doThrow(new DelayedIndexingException()).when(component1).commit();
		
		try {
			fixture.commit();
			Assert.fail("Did not throw exception");
		} catch (final DelayedIndexingException ex) {
			// ignore
		}
		
		verify(component1).commit();
		verify(component2).commit();
		verify(component3).commit();
	}
	
	@Test
	public void test_search_result_mixing() throws IOException {
		when(component2.search(Integer.class, query1, 10)).thenReturn(results1);
		when(component3.search(Integer.class, query1, 10)).thenReturn(results2);
		
		when(results1.next()).thenReturn(1).thenReturn(2).thenReturn(null);
		when(results2.next()).thenReturn(10).thenReturn(20).thenReturn(30).thenReturn(null);
		
		final JIndexResults<Integer> res = fixture.search(Integer.class, query1, 10);
		Assert.assertEquals(1, (int)res.next());
		Assert.assertEquals(10, (int)res.next());
		Assert.assertEquals(2, (int)res.next());
		Assert.assertEquals(20, (int)res.next());
		Assert.assertEquals(30, (int)res.next());
		Assert.assertNull(res.next());
		
		res.close();
		verify(results1).close();
		verify(results2).close();
	}
}
