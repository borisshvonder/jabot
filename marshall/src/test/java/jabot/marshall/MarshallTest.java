package jabot.marshall;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MarshallTest {
	private Marshall fixture;
	
	private TestBean bean;
	
	@Before
	public void setUp() {
		fixture = new Marshall();
		// For test simplicity
		fixture.configure().allowSingleQuotes().allowUnquotedFields();
		bean = new TestBean();
	}
	
	@Test
	public void testGetDefaultInstance() {
		Assert.assertNotNull(Marshall.get());
	}
	
	@Test
	public void testMarshall() {
		final TestBean bean = new TestBean();
		final String marshalled = fixture.toJson(bean);
		Assert.assertTrue(marshalled.startsWith("{"));
		Assert.assertTrue(marshalled.endsWith("}"));
	}

	@Test
	public void testMarshallToStream() throws IOException {
		final TestBean bean = new TestBean();
		final String marshalled = fixture.toJson(bean);
		
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		fixture.toStream(bean, out);
		final String streamAsString = new String(out.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertEquals(marshalled, streamAsString);
	}
	
	@Test
	public void testUnmarshall() {
		final TestBean bean = fixture.fromJson("{\"integer\":100}", TestBean.class);
		Assert.assertNotNull(bean);
		Assert.assertEquals(100, bean.getInteger());
	}
	
	@Test
	public void testUnmarshallStream() throws IOException {
		final String marshalled = "{\"integer\":100}";
		final ByteArrayInputStream in = new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8));
		final TestBean bean = fixture.fromStream(in, TestBean.class);
		Assert.assertNotNull(bean);
		Assert.assertEquals(100, bean.getInteger());
	}

	@Test
	public void testSingleQuotes() {
		fixture = new Marshall();
		final String json="{'str':'val'}";
		
		try {
			fixture.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
		
		fixture.configure().allowSingleQuotes();
		Assert.assertEquals("val", fixture.fromJson(json, TestBean.class).getStr());
		
		Marshall clone = fixture.clone().allowSingleQuotes().clone();
		Assert.assertEquals("val", clone.fromJson(json, TestBean.class).getStr());
		
		clone = clone.clone().setSingleQuotes(false).clone();
		try {
			clone.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
	}
	
	@Test
	public void testUnquotedFields() {
		fixture = new Marshall();
		final String json="{str:\"val\"}";
		
		try {
			fixture.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
		
		fixture.configure().allowUnquotedFields();
		Assert.assertEquals("val", fixture.fromJson(json, TestBean.class).getStr());
		
		Marshall clone = fixture.clone().allowUnquotedFields().clone();
		Assert.assertEquals("val", clone.fromJson(json, TestBean.class).getStr());
		
		clone = clone.clone().setUnquotedFields(false).clone();
		try {
			clone.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
	}
	
	@Test
	public void testUnknownProperties() {
		fixture = new Marshall();
		final String json="{\"unknownProperty\":\"val\"}";
		
		try {
			fixture.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
		
		fixture.configure().allowUnknownProperties();
		Assert.assertNotNull(fixture.fromJson(json, TestBean.class));
		
		Marshall clone = fixture.clone().allowUnknownProperties().clone();
		Assert.assertNotNull(clone.fromJson(json, TestBean.class));
		
		clone = clone.clone().setUnknownProperties(false).clone();
		try {
			clone.fromJson(json, TestBean.class);
			Assert.fail();
		} catch (final UnmarshallingException ex) {
			Assert.assertEquals(json, ex.getJson());
		}
	}
	
	@Test
	public void testSupressNullFields() {
		fixture = new Marshall().clone().supressNullFields(true).clone();
		String json = fixture.toJson(new TestBean());
		Assert.assertTrue(json.indexOf("date")<0);
	}
	
	@Test
	public void testSupressNullFieldsInConfigurator() {
		fixture.configure().supressNullFields(true);
		String json = fixture.toJson(new TestBean());
		Assert.assertTrue(json.indexOf("date")<0);
	}
	
	@Test
	public void testTransientProperties() {
		fixture = new Marshall().clone()
				.setIngnoreTransient(true)
				.clone();
		
		final String json="{\"trans\":\"val\"}";
		
		final TestBean bean = fixture.fromJson(json, TestBean.class);
		Assert.assertNull(bean.getTrans());
		
		bean.setTrans("trans");
		String marshalled = fixture.toJson(bean);
		Assert.assertTrue(marshalled.indexOf("\"trans\"")<0);
	}
	
	@Test
	public void testMarshallUnmarshall() {
		bean.setDate(new Date());
		bean.setFloating(1.44f);
		bean.setInteger(123);
		bean.setStr("val");
		
		final String json = fixture.toJson(bean);
		final TestBean restored = fixture.fromJson(json, TestBean.class);
		
		Assert.assertEquals(bean.getFloating(), restored.getFloating(), 0.001f);
		Assert.assertEquals(bean.getStr(), restored.getStr());
		Assert.assertEquals(bean.getDate(), restored.getDate());
		Assert.assertEquals(bean.getInteger(), restored.getInteger());
	}
	
	@Test
	public void testDateMarshalling() {
		bean.setDate(new Date(100));
		
		Assert.assertTrue(fixture.toJson(bean).indexOf("\"date\":100")>0);
	}
	
	@Test
	public void testDateUnmarshalling() {
		bean.setDate(new Date(100));
		
		Assert.assertEquals(new Date(100L), fixture.fromJson("{date:100}", TestBean.class).getDate());
	}
	
	public static final class TestBean {
		private int integer;
		private float floating;
		private Date date;
		private String str;
		private transient String trans;
		
		public int getInteger() {
			return integer;
		}

		public void setInteger(int integer) {
			this.integer = integer;
		}

		public float getFloating() {
			return floating;
		}

		public void setFloating(float floating) {
			this.floating = floating;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public String getStr() {
			return str;
		}

		public void setStr(String str) {
			this.str = str;
		}

		public String getTrans() {
			return trans;
		}

		public void setTrans(String trans) {
			this.trans = trans;
		}

		
	}
}
