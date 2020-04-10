package jabot.jindex;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import jabot.idxapi.Document;
import jabot.idxapi.Field;
import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;
import jabot.idxapi.FieldValue;
import jabot.idxapi.Untokenized;
import jabot.jindex.ModelIntrospector.FieldMapping;
import jabot.jindex.ModelIntrospector.Mapper;

public class ModelIntrospectorTest {
	
	@Test
	public void test_EmptyBean() {
		final Mapper<EmptyBean> mapper = new ModelIntrospector<>(EmptyBean.class).buildMapper();
		final EmptyBean empty = new EmptyBean();
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), empty);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals(1, doc.getFields().size());
		
		final EmptyBean restored = (EmptyBean) mapper.doc2bean(doc);
		Assert.assertNotNull(restored);
	}

	@Test
	public void test_OneFieldBean() {
		final ModelIntrospector<OneFieldBean> intr = new ModelIntrospector<>(OneFieldBean.class);
		intr.getMapping("str").setStorage(Storage.STORED_INDEXED);
		Assert.assertEquals("str", intr.getMapping("str").getBasicName());
		Assert.assertEquals(Type.STRING, intr.getMapping("str").getType());
		Assert.assertEquals(Storage.STORED_INDEXED, intr.getMapping("str").getStorage());
		Assert.assertFalse(intr.getMapping("str").isMultivalued());
		
		final Mapper<OneFieldBean> mapper = intr.buildMapper();
		final OneFieldBean bean = new OneFieldBean();
		bean.setStr("value1");
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(2, doc.getFields().size());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals("str", doc.getFields().get(1).getField().getBasicName());
		Assert.assertEquals(bean.getStr(), doc.getFields().get(1).getValue());
				
		final OneFieldBean restored = mapper.doc2bean(doc);
		Assert.assertNotNull(restored);
		Assert.assertEquals(bean.getStr(), restored.getStr());
		
		final FieldMapping map = mapper.getMapping("str");
		Assert.assertEquals("str", map.getBasicName());
		Assert.assertEquals(Type.STRING, map.getType());
		Assert.assertEquals(Storage.STORED_INDEXED, map.getStorage());
		Assert.assertFalse(map.isMultivalued());
		Assert.assertEquals("str_str_f", map.getField().getName());
	}
	
	@Test
	public void test_UriBean() {
		final ModelIntrospector<UriBean> intr = new ModelIntrospector<>(UriBean.class);
		intr.getMapping("uri").setStorage(Storage.STORED_INDEXED);
		Assert.assertEquals("uri", intr.getMapping("uri").getBasicName());
		Assert.assertEquals(Type.STRING, intr.getMapping("uri").getType());
		Assert.assertEquals(Storage.STORED_INDEXED, intr.getMapping("uri").getStorage());
		Assert.assertFalse(intr.getMapping("uri").isMultivalued());
		
		final Mapper<UriBean> mapper = intr.buildMapper();
		final UriBean bean = new UriBean();
		bean.setUri(URI.create("scheme://path"));
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(2, doc.getFields().size());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals("uri", doc.getFields().get(1).getField().getBasicName());
		Assert.assertEquals(bean.getUri().toString(), doc.getFields().get(1).getValue());
				
		final UriBean restored = mapper.doc2bean(doc);
		Assert.assertNotNull(restored);
		Assert.assertEquals(bean.getUri(), restored.getUri());
		
		final FieldMapping map = mapper.getMapping("uri");
		Assert.assertEquals("uri", map.getBasicName());
		Assert.assertEquals(Type.STRING, map.getType());
		Assert.assertEquals(Storage.STORED_INDEXED, map.getStorage());
		Assert.assertFalse(map.isMultivalued());
		Assert.assertEquals("uri_str_f", map.getField().getName());
	}

	@Test
	public void test_UriList() {
		final ModelIntrospector<UriBean> intr = new ModelIntrospector<>(UriBean.class);
		intr.getMapping("list").setCustomAdaptor(UriCustomTypeAdaptor.INSTANCE).setStorage(Storage.STORED_INDEXED);
		
		final Mapper<UriBean> mapper = intr.buildMapper();
		final UriBean bean = new UriBean();
		bean.setList(Arrays.asList(URI.create("scheme://path1"), null, URI.create("scheme://path2")));
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(2, doc.getFields().size());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals("list", doc.getFields().get(1).getField().getBasicName());
		Assert.assertTrue(intr.getMapping("list").isMultivalued());
		
		Assert.assertEquals(Arrays.asList("scheme://path1", "scheme://path2"), doc.getFields().get(1).getValue());
				
		final UriBean restored = mapper.doc2bean(doc);
		Assert.assertNotNull(restored);
		Assert.assertEquals(Arrays.asList(URI.create("scheme://path1"), URI.create("scheme://path2")), 
				restored.getList());
	}

	@Test
	public void test_ingnore_field() {
		final ModelIntrospector<OneFieldBean> intr = new ModelIntrospector<>(OneFieldBean.class);
		intr.getMapping("str").ignore();
		
		final Mapper<OneFieldBean> mapper = intr.buildMapper();
		final OneFieldBean bean = new OneFieldBean();
		bean.setStr("value1");
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals(1, doc.getFields().size());
	}
	
	@Test
	public void test_CustomizeDocumentFieldName() {
		final ModelIntrospector<OneFieldBean> intr = new ModelIntrospector<>(OneFieldBean.class);
		intr.getMapping("str").setBasicName("myString");
		final Mapper<OneFieldBean> mapper = intr.buildMapper();
		final OneFieldBean bean = new OneFieldBean();
		bean.setStr("value1");
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals("myString", doc.getFields().get(1).getField().getBasicName());
	}
	
	@Test
	public void test_IntBean() {
		final ModelIntrospector<IntBean> intr = new ModelIntrospector<>(IntBean.class);
		intr.getMapping("i").setStorage(Storage.STORED_INDEXED);
		final Mapper<IntBean> mapper = intr.buildMapper();
		final IntBean bean = new IntBean();
		bean.setI(123);
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		final IntBean restored = (IntBean) mapper.doc2bean(doc);
		Assert.assertEquals(bean.getI(), restored.getI());
		
		Assert.assertEquals("i_int_f", mapper.getMapping("i").getField().getName());
	}
	
	@Test
	public void test_typeHint() {
		final ModelIntrospector<IntBean> intr = new ModelIntrospector<>(IntBean.class);
		intr.getMapping("i").setType(Type.INT);
		final Mapper<IntBean> mapper = intr.buildMapper();
		final IntBean bean = new IntBean();
		bean.setI(123);
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals("i_int", doc.getFields().get(1).getField().getName());
	}
	
	@Test
	public void test_IntegerBean() {
		final ModelIntrospector<IntegerBean> intr = new ModelIntrospector<>(IntegerBean.class);
		intr.getMapping("i").setStorage(Storage.STORED_INDEXED);
		intr.getMapping("list").setStorage(Storage.STORED);
		final Mapper<IntegerBean> mapper = intr.buildMapper();
		Assert.assertEquals(new Field("class_unt_f"), mapper.getClassFieldValue().getField());
		Assert.assertEquals(new Untokenized(IntegerBean.class.getName()), mapper.getClassFieldValue().getValue());
		final IntegerBean bean = new IntegerBean();
		bean.setI(123);
		bean.setList(Arrays.asList(1, null, 2, 3));
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		final IntegerBean restored = mapper.doc2bean(doc);
		
		Assert.assertEquals(bean.getI(), restored.getI());
		Assert.assertEquals(Arrays.asList(1, 2, 3), restored.getList());
		
		Assert.assertEquals("i_int_f", mapper.getMapping("i").getField().getName());
		Assert.assertEquals(2, mapper.getAllMappings().size());
		
		Set<Field> expectedFields = new HashSet<>(Arrays.asList(new Field("list_int_s_mv"), new Field("i_int_f")));
		Assert.assertEquals(expectedFields, new HashSet<>(mapper.getStoredFields()));
	}
	
	@Test
	public void test_DontPutNullLists() {
		final ModelIntrospector<IntegerBean> intr = new ModelIntrospector<>(IntegerBean.class);
		final Mapper<IntegerBean> mapper = intr.buildMapper();
		final IntegerBean bean = new IntegerBean();
		bean.setList(Arrays.<Integer>asList(null, null));
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(mapper.getClassFieldValue().getField(), doc.getFields().get(0).getField());
		Assert.assertEquals(1, doc.getFields().size());
	}

	@Test
	public void test_ComplexBean() {
		final ModelIntrospector<ComplexBean> intr = new ModelIntrospector<>(ComplexBean.class);
		intr.getMapping("s").setStorage(Storage.STORED);
		intr.getMapping("i").setStorage(Storage.STORED_INDEXED);
		intr.getMapping("stringList").setStorage(Storage.STORED_INDEXED);
		
		final Mapper<ComplexBean> mapper = intr.buildMapper();

		final ComplexBean bean = new ComplexBean();
		bean.setI(1);
		bean.setL(100L);
		bean.setS("s1");
		bean.setStringList(Arrays.asList("l1", "l2"));
		bean.setIntList(Arrays.asList(10, 20));
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertEquals(new Untokenized("pk"), doc.getPk());
		
		final ComplexBean restored = mapper.doc2bean(doc);
		Assert.assertEquals(bean.getS(), restored.getS());
		Assert.assertEquals(bean.getI(), restored.getI());
		Assert.assertEquals(bean.getStringList(), restored.getStringList());
		
		Assert.assertEquals("s_str_s", mapper.getMapping("s").getField().getName());
		Assert.assertEquals("i_int_f", mapper.getMapping("i").getField().getName());
		Assert.assertEquals("l_long", mapper.getMapping("l").getField().getName());
		Assert.assertEquals("stringList_str_f_mv", mapper.getMapping("stringList").getField().getName());
		Assert.assertEquals("intList_int_mv", mapper.getMapping("intList").getField().getName());
	}
	
	@Test
	public void test_DateBean() {
		final ModelIntrospector<DateBean> intr = new ModelIntrospector<>(DateBean.class);
		intr.getMapping("date").setStorage(Storage.STORED_INDEXED);
		final Mapper<DateBean> mapper = intr.buildMapper();
		final DateBean bean = new DateBean();
		bean.setDate(new Date());
		
		final Document doc = mapper.bean2doc(new Untokenized("pk"), bean);
		Assert.assertTrue(doc.getFields().get(0).getValue() instanceof Untokenized);
		
		final DateBean restored = mapper.doc2bean(doc);
		
		Assert.assertNull(restored.getDate());
		
		Assert.assertEquals("date_str_f", mapper.getMapping("date").getField().getName());
	}
	
	@Test
	public void test_taste_model() {
		final Document doc = new Document(new Untokenized("pk"));
		doc.add(new FieldValue("list_int_mv", Arrays.asList(100)));
		
		final IntegerBean inflated = new IntegerBean();
		inflated.setList(Arrays.asList(1));
		final ModelIntrospector<IntegerBean> intr = new ModelIntrospector<>(IntegerBean.class);
		intr.getMapping("list").setStorage(Storage.STORED_INDEXED);
		final Mapper<IntegerBean> mapper = intr.buildMapper();
		
		final IntegerBean restored = mapper.doc2bean(doc);
		Assert.assertEquals(Arrays.asList(100), restored.getList());
		
	}

	static final class EmptyBean{}

	static final class OneFieldBean {
		private String str;

		public String getStr() {
			return str;
		}

		public void setStr(String str) {
			this.str = str;
		}
	}
	
	static final class IntBean {
		private int i;
		public int getI() { return i; }
		public void setI(int i) { this.i = i; }
	}
	
	static final class IntegerBean {
		private Integer i;
		private List<Integer> list;
		public Integer getI() { return i; }
		public void setI(Integer i) { this.i = i; }
		public List<Integer> getList() { return list; }
		public void setList(List<Integer> list) { this.list = list; }
	}
	
	static final class ComplexBean {
		private String s;
		private int i;
		private long l;
		private List<String> stringList;
		private List<Integer> intList;
		
		public String getS() {
			return s;
		}
		
		public void setS(String s) {
			this.s = s;
		}
		
		public int getI() {
			return i;
		}
		
		public void setI(int i) {
			this.i = i;
		}
		
		public long getL() {
			return l;
		}
		
		public void setL(long l) {
			this.l = l;
		}
		
		public List<String> getStringList() {
			return stringList;
		}

		public void setStringList(List<String> stringList) {
			this.stringList = stringList;
		}

		public List<Integer> getIntList() {
			return intList;
		}

		public void setIntList(List<Integer> intList) {
			this.intList = intList;
		}
	}
	
	static final class DateBean {
		private Date date; // Dates will NEVER be supported since they are non-UTC
		public Date getDate() { return date; }
		public void setDate(Date date) { this.date = date; }
	}
	
	static final class UriBean {
		private URI uri; // URIs will get automatically converted to/from String
		private List<URI> list;
		
		public URI getUri() { return uri; }
		public void setUri(URI uri) { this.uri = uri; }

		public List<URI> getList() { return list; }
		public void setList(List<URI> list) { this.list = list; }
	}
}
