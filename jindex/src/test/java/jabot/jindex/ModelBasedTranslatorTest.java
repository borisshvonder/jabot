package jabot.jindex;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.jindex.ModelIntrospector.Mapper;

public class ModelBasedTranslatorTest {
	private ModelBasedTranslator<Person> fixture;
	
	@Before
	public void setUp() {
		final ModelIntrospector<Person> intr = new ModelIntrospector<>(Person.class);
		intr.getMapping("nonDefaultField").setDefaultField(false);
		final Mapper<Person> mapper = intr.buildMapper();
		
		final Person inflated = new Person();
		inflated.setAliases(Arrays.asList("alias1", "alias2"));
		mapper.taste(inflated);
		
		fixture = new ModelBasedTranslator<>(mapper);
		fixture.setExpandDefaultField(false);
	}
	
	@Test
	public void test_oneField() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +name_str:john", 
				translate("name:john"));
	}
	
	@Test
	public void test_twoFields() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +(name_str:john age_int:22)", 
				translate("name:john age:22"));
	}
	
	@Test
	public void test_plusMinus() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +(+name_str:john -age_int:22)",
				translate("+name:john -age:22"));
	}
	
	@Test
	public void test_grouping() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "+
		                    "+(+(name_str:john aliases_str_mv:john) (age_int:22 age_int:23))", 
				translate("+(name:john aliases:john) age:(22 23)"));
	}
	
	@Test
	public void test_unknownFieldBypass() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +unknown_field:value", 
				translate("unknown_field:value"));
	}
	
	@Test
	public void test_translate_default() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+metadata:jonh", 
				translate("Jonh"));
	}
	
	@Test
	public void test_translate_default_expand() throws ParseException {
		fixture.setExpandDefaultField(true);
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+(name_str:jonh aliases_str_mv:jonh)", 
				translate("Jonh"));
	}
	
	@Test
	public void test_translate_default_expand_fixed() throws ParseException {
		fixture.setExpandDefaultField(true);
		fixture.setExpandDefaultFieldTo(Arrays.asList("field1", "field2"));
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+(field1:jonh field2:jonh)", 
				translate("Jonh"));
	}
	
	@Test
	public void test_translate_phraseQuery() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
				          + "+name_str:\"jonh smith\"~5", 
				translate("name: \"Jonh Smith\"~5"));
	}
	
	@Test
	public void test_translate_phraseQuery_default() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+metadata:\"jonh smith\"~3", 
				translate("\"Jonh Smith\"~3"));
	}
	
	@Test
	public void test_translate_phraseQuery_default_expand() throws ParseException {
		fixture.setExpandDefaultField(true);
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+(name_str:\"jonh smith\"~3 aliases_str_mv:\"jonh smith\"~3)", 
				translate("\"Jonh Smith\"~3"));
	}
	
	@Test
	public void test_translate_phraseQuery_default_expand_fixed() throws ParseException {
		fixture.setExpandDefaultField(true);
		fixture.setExpandDefaultFieldTo(Arrays.asList("field1", "field2"));
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person "
		          + "+(field1:\"jonh smith\"~3 field2:\"jonh smith\"~3)", 
				translate("\"Jonh Smith\"~3"));
	}

	@Test
	public void test_search_in_nonDefaultField() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +nonDefaultField_str:value", 
				translate("nonDefaultField:value"));
	}
	
	@Test
	public void test_praseQuery_in_nonDefaultField() throws ParseException {
		Assert.assertEquals("+class_unt_f:jabot.jindex.ModelBasedTranslatorTest$Person +nonDefaultField_str:\"somewhat value\"~2", 
				translate("nonDefaultField:\"somewhat value\"~2"));
	}
	
	private String translate(final String search) throws ParseException {
		return fixture.translate(search).toString();
	}


	static final class Person {
		private int age;
		private String name;
		private List<String> aliases;
		private String nonDefaultField;
		
		public int getAge() {
			return age;
		}
		
		public void setAge(int age) {
			this.age = age;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public List<String> getAliases() {
			return aliases;
		}
		
		public void setAliases(List<String> aliases) {
			this.aliases = aliases;
		}

		public String getNonDefaultField() {
			return nonDefaultField;
		}

		public void setNonDefaultField(String nonDefaultField) {
			this.nonDefaultField = nonDefaultField;
		}
		
	}
}
