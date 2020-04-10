package jabot.idxapi;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;

public class FieldTest {
	@BeforeClass
	public static void enableAssetions() {
		ModuleAssertions.enable(true);
	}
	
	@Test(expected=FieldFormatException.class)
	public void testInvalidStorage() {
		new Field("basic_binary_!_mv").validate();
	}
	
	@Test(expected=FieldFormatException.class)
	public void testInvalidType() {
		new Field("basic_!").validate();
	}
	
	@Test(expected=FieldFormatException.class)
	public void testInvalidEmpty() {
		new Field("").validate();
	}
	
	@Test(expected=FieldFormatException.class)
	public void testInvalidNoType() {
		new Field("basic").validate();
	}
	
	@Test(expected=FieldFormatException.class)
	public void testInvalidNoName() {
		new Field("string").validate();
	}

	@Test
	public void testParseField() {
		assertEquals(new Field("basic", Type.BINARY, Storage.STORED_INDEXED,  true), new Field("basic_binary_f_mv"));
		assertEquals(new Field("basic", Type.UNTOKENIZED, Storage.INDEXED,  false), new Field("basic_unt"));
		assertEquals(new Field("with_delimeter", Type.LONG, Storage.INDEXED,  false), new Field("with_delimeter_long"));
		assertEquals(new Field("basic", Type.INT, Storage.STORED,  false), new Field("basic_int_s"));
		assertEquals(new Field("basic", Type.STRING, Storage.STORED_INDEXED,  true), new Field("basic_str_f_mv"));
		assertEquals(new Field("basic", Type.STRING_RU, Storage.STORED,  false), new Field("basic_strru_s"));
	}
	
	@Test
	public void testHashCodeEquals() {
		final Field field = new Field("basic_binary_f_mv");
		assertNotEquals(field, null);
		assertNotEquals(field, new Object());
		assertNotEquals(field, new Field("basic_binary"));
		assertEquals(field, field);
		assertEquals(field, new Field("basic_binary_f_mv"));
	}

	private void assertEquals(final Field expected, final Field actual) {
		Assert.assertEquals(expected, actual);
		Assert.assertEquals(expected.getBasicName(), actual.getBasicName());
		Assert.assertEquals(expected.getName(), actual.getName());
		Assert.assertEquals(expected.getStorage(), actual.getStorage());
		Assert.assertEquals(expected.getType(), actual.getType());
		Assert.assertEquals(expected.isMultivalued(), actual.isMultivalued());
		Assert.assertEquals(expected.hashCode(), actual.hashCode());
		Assert.assertEquals(expected.toString(), actual.toString());
	}

	private void assertNotEquals(final Field expected, final Object actual) {
		Assert.assertNotEquals(expected, actual);
		if (actual != null) {
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
			Assert.assertNotEquals(expected.toString(), actual.toString());
		}
	}

}
