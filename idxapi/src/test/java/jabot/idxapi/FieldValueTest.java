package jabot.idxapi;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

public class FieldValueTest {
	@BeforeClass
	public static void enableAssetions() {
		ModuleAssertions.enable(true);
	}
	
	@Test
	public void testValidTypes() {
		new FieldValue("name_str", "text1").validate();
		new FieldValue("counts_int_mv", Arrays.asList(1, 2, 3)).validate();
		new FieldValue("name_long", 100L).validate();
		new FieldValue("name_double_mv", Arrays.asList(1.1)).validate();
		new FieldValue("name_str_f_mv", Arrays.asList("a", "b")).validate();
		new FieldValue("name_strru_s", "a").validate();
	}
	
	@Test(expected=FieldValueInvalidException.class)
	public void testInvalidType() {
		new FieldValue("name_unt", 1).validate();
	}
	
	@Test(expected=FieldValueInvalidException.class)
	public void testInvalidListType() {
		new FieldValue("name_unt_mv", Arrays.asList(1, 2)).validate();
	}
	
	@Test(expected=FieldValueInvalidException.class)
	public void test_invalid_not_a_multivalued_field() {
		new FieldValue("name_int", Arrays.asList(1, 2)).validate();
	}
	
	@Test(expected=FieldValueInvalidException.class)
	public void test_invalid_multivalued_field() {
		new FieldValue("name_int_mv", 1).validate();
	}
	
	@Test(expected=FieldValueInvalidException.class)
	public void test_invalid_null_elements_are_not_allowed_in_multivalued_field() {
		new FieldValue("name_int_mv", Arrays.asList(new Integer[]{null})).validate();
	}
}
