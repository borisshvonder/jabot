package jabot.jindex;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Test;

import jabot.jindex.Parser;

public class ParserTest {
	@Test
	public void test() throws ParseException {
		assertParsesUniformly("*:*");
		assertParsesUniformly("field:value");
		assertParsesUniformly("default");
		assertParsesUniformly("field:value default");
	}
	
	@Test
	public void testMultiTerm() throws ParseException {
		assertParsesUniformly("multi term");
		assertParsesUniformly("\"multi term\" field:value");
	}
	
	@Test
	public void testStopWords() throws ParseException {
		final Query query = Parser.DEFAULT.parse("and or");
		Assert.assertEquals("", query.toString());
	}
	
	@Test
	public void testUnsafeChars() throws ParseException {
		Assert.assertEquals("Hello World ", Parser.normalize("Hello!World?"));
		
		final Query query = Parser.DEFAULT.parse("Hello!World?");
		Assert.assertEquals("metadata:hello metadata:world", query.toString());
	}
	
	private void assertParsesUniformly(final String search) throws ParseException {
		final Query query = Parser.DEFAULT.parse(search);
		Assert.assertEquals(search, Parser.DEFAULT.toString(query));
	}
}
