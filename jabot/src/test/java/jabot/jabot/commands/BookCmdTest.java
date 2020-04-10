package jabot.jabot.commands;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class BookCmdTest {
	@Test
	public void testNormalizeString() {
		Assert.assertEquals("", BookCmd.normalizeString(null));
		Assert.assertEquals("", BookCmd.normalizeString(""));
		Assert.assertEquals("T0KEN", BookCmd.normalizeString("t0ken"));
		Assert.assertEquals("TOKEN", BookCmd.normalizeString("  token  "));
		Assert.assertEquals("TWO TOKENS WITH DELIMETERS", BookCmd.normalizeString("!two ? tokens . with - delimeters!"));
	}
	
	@Test
	public void testTokenizeString() {
		Assert.assertEquals(set(), BookCmd.tokenizeString(null));
		Assert.assertEquals(set(), BookCmd.tokenizeString(""));
		Assert.assertEquals(set("T0KEN"), BookCmd.tokenizeString("t0ken"));
		Assert.assertEquals(set("TOKEN"), BookCmd.tokenizeString("  token  "));
		
		Assert.assertEquals(set("TWO", "TOKENS", "WITH", "DELIMETERS"), 
				BookCmd.tokenizeString("!two ? tokens . with - delimeters!"));
		
	}
	
	@Test
	public void test_normalizeQueryTerm() {
		Assert.assertEquals("", BookCmd.normalizeQueryTerm(null));
		Assert.assertEquals("", BookCmd.normalizeQueryTerm(""));
		Assert.assertEquals("abc", BookCmd.normalizeQueryTerm("abc"));
		Assert.assertEquals("c\\+\\+", BookCmd.normalizeQueryTerm("c++"));
	}

	private Set<String> set(final String ... strings) {
		final Set<String> ret = new HashSet<>();
		for (final String s : strings) {
			ret.add(s);
		}
		return ret;
	}
}
