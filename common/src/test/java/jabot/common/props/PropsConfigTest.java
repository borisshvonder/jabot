package jabot.common.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.types.Interval;
import jabot.common.types.IntervalFormatException;

public class PropsConfigTest {
	private final Properties props = new Properties();
	
	@Test
	public void testRequireProperty() {
		props.setProperty("key", "value");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals("value", conf.requireString("key"));
	}
	
	@Test
	public void testRequireMissingProperty() {
		PropsConfig conf = new PropsConfig(props);
		try {
			conf.requireString("missing");
			Assert.fail();
		} catch (MissingPropertyException ex) {
			Assert.assertEquals("missing", ex.getKey());
		}
	}
	
	@Test
	public void testGetString() {
		props.setProperty("key", "value");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals("value", conf.getString("key", "def"));
		Assert.assertEquals("def", conf.getString("key2", "def"));
		Assert.assertNull(conf.getString("key2", null));
	}
	
	@Test
	public void getInt_normal() {
		props.setProperty("key", "100");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(100, (int)conf.getInt("key", null));
	}
	
	@Test(expected=NumberFormatException.class)
	public void getInt_parseError() {
		props.setProperty("key", "badformat");
		PropsConfig conf = new PropsConfig(props);

		conf.getInt("key", null);
	}
	
	@Test
	public void getInt_default() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(200, (int)conf.getInt("key", 200));
	}
	
	@Test
	public void getIntProperty_default_null() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertNull(conf.getInt("key", null));
	}
	
	@Test
	public void getLong_normal() {
		props.setProperty("key", "100");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(100L, (long)conf.getLong("key", null));
	}
	
	@Test(expected=NumberFormatException.class)
	public void getLong_parseError() {
		props.setProperty("key", "badformat");
		PropsConfig conf = new PropsConfig(props);
		conf.getLong("key", null);
	}
	
	@Test
	public void getLong_default() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(200L, (long)conf.getLong("key", 200L));
	}
	
	@Test
	public void getLongProperty_default_null() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertNull(conf.getLong("key", null));
	}
	
	@Test
	public void getInterval_normal() {
		props.setProperty("key", "1SECONDS");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(Interval.SECOND, conf.getInterval("key", null));
	}
	
	@Test(expected=IntervalFormatException.class)
	public void getInterval_parseError() {
		props.setProperty("key", "badformat");
		PropsConfig conf = new PropsConfig(props);
		conf.getInterval("key", null);
	}
	
	@Test
	public void getInterval_default() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(Interval.YEAR, conf.getInterval("key", Interval.YEAR));
	}
	
	@Test
	public void getIntervalProperty_default_null() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertNull(conf.getInterval("key", null));
	}
	
	@Test
	public void getBoolean_normal() {
		props.setProperty("key1", "true");
		props.setProperty("key2", "Yes");
		props.setProperty("key3", "y");
		props.setProperty("key4", "false");
		props.setProperty("key5", "NO");
		props.setProperty("key6", "n");
		PropsConfig conf = new PropsConfig(props);

		Assert.assertTrue(conf.getBoolean("key1", null));
		Assert.assertTrue(conf.getBoolean("key2", null));
		Assert.assertTrue(conf.getBoolean("key3", null));
		Assert.assertFalse(conf.getBoolean("key4", null));
		Assert.assertFalse(conf.getBoolean("key5", null));
		Assert.assertFalse(conf.getBoolean("key6", null));
	}
	
	@Test
	public void getBoolean_parseError() {
		props.setProperty("key", "badformat");
		PropsConfig conf = new PropsConfig(props);

		try {
			conf.getBoolean("key", null);
			Assert.fail();
		} catch (final BooleanFormatException ex) {
			Assert.assertEquals("badformat", ex.getText());
		}
	}
	
	@Test
	public void getBoolean_default() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertTrue(conf.getBoolean("key", true));
	}
	
	@Test
	public void getBooleanProperty_default_null() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertNull(conf.getBoolean("key", null));
	}

	@Test
	public void asProps() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertNotNull(conf.asProps());
		Assert.assertNotSame(props, conf.asProps());
		Assert.assertEquals(props, conf.asProps());
	}
	
	@Test
	public void test_keysWithPrefix_empty() {
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(0, conf.keysWithPrefix(null).size());
	}

	@Test
	public void test_keysWithPrefix_all() {
		props.setProperty("key1", "value1");
		PropsConfig conf = new PropsConfig(props);
		Assert.assertEquals(Arrays.asList("key1"), conf.keysWithPrefix(""));
	}

	@Test
	public void test_keysWithPrefix_some() {
		props.setProperty("key1", "value1");
		props.setProperty("otherKey", "otherValue");
		props.setProperty("key2.subkey1", "value3");
		PropsConfig conf = new PropsConfig(props);
		
		final List<String> keys = new ArrayList<>(conf.keysWithPrefix("key"));
		Collections.sort(keys);
		
		Assert.assertEquals(Arrays.asList("key1", "key2.subkey1"), keys);
	}
}
