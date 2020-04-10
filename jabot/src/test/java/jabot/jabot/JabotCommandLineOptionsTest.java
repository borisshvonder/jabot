package jabot.jabot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JabotCommandLineOptionsTest {
	private JabotCommandLineOptions fixture;
	
	@Before
	public void setUp() throws IOException, ParseException {
		fixture = new JabotCommandLineOptions();
	}
	
	@Test
	public void testDefaultHostPort() throws IOException, ParseException {
		fixture.parse(new String[0]);
		Assert.assertEquals("http://127.0.0.1:9090", fixture.getRsEndpoint().toString());
	}
	
	@Test
	public void testHostPort() throws IOException, ParseException {
		fixture.parse(new String[]{"-w", "https://somesite:9111"});
		Assert.assertEquals("https://somesite:9111", fixture.getRsEndpoint().toString());
	}
	
	@Test
	public void testDefaultDebugMode() throws IOException, ParseException {
		fixture.parse(new String[0]);
		Assert.assertFalse(fixture.isDebugMode());
	}
	
	@Test
	public void testDebugMode() throws IOException, ParseException {
		fixture.parse(new String[]{"-d"});
		Assert.assertTrue(fixture.isDebugMode());
	}
	
	@Test
	public void testHelp() throws IOException, ParseException {
		fixture.parse(new String[]{"-h"});
		Assert.assertTrue(fixture.helpRequested());
		final StringWriter w = new StringWriter();
		final PrintWriter pw = new PrintWriter(w);
		fixture.printHelp("jabot", pw);
		pw.flush();
		final String help = w.toString();
		Assert.assertNotNull(help);
	}
	
	@Test
	public void testConfigFile() throws IOException, ParseException {
		fixture.parse(new String[]{"-c", "/etc/jabot.conf"});
		Assert.assertEquals("/etc/jabot.conf", fixture.allConfig().getString("jabot.conf", null));
	}
	
	@Test
	public void testLogFile() throws IOException, ParseException {
		fixture.parse(new String[]{"-l", "/tmp/jabot.log"});
		Assert.assertEquals("/tmp/jabot.log", fixture.getLog4jConfiguration().getProperty("log4j.appender.file.File"));
	}
	
	@Test
	public void testLogLevel() throws IOException, ParseException {
		fixture.parse(new String[]{"-t", "TRACE"});
		Assert.assertEquals("TRACE", fixture.getLog4jConfiguration().getProperty("log4j.appender.file.Threshold"));
	}
	
	@Test
	public void testSolr() throws IOException, ParseException {
		fixture.parse(new String[]{"-s", "http://127.0.0.1:8983/solr/core0"});
		Assert.assertEquals("http://127.0.0.1:8983/solr/core0", fixture.allConfig().getString("solr.default", null));
	}
}
