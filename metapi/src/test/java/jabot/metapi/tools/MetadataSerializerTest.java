package jabot.metapi.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.metapi.Metadata;

public class MetadataSerializerTest {
	private ByteArrayOutputStream out;
	private Metadata meta;
	
	@Before
	public void setUp() {
		out = new ByteArrayOutputStream();
		meta = new Metadata();
	}
	
	@Test
	public void test_read_empty() throws IOException {
		meta = MetadataSerializer.read(stream(""));
		Assert.assertEquals(0, meta.entrySet().size());
	}
	
	@Test
	public void test_read_simple_key() throws IOException {
		final Metadata meta = MetadataSerializer.read(stream("key=value"));
		Assert.assertEquals(1, meta.entrySet().size());
		Assert.assertEquals(Arrays.asList("value"), meta.get("key"));
	}
	
	@Test
	public void test_read_list() throws IOException {
		final Metadata meta = MetadataSerializer.read(stream("key=[v1 , v2]"));
		Assert.assertEquals(1, meta.entrySet().size());
		Assert.assertEquals(Arrays.asList("v1", "v2"), meta.get("key"));
	}
	
	@Test
	public void test_write_empty() throws IOException {
		MetadataSerializer.write(meta, out);
		Assert.assertEquals("", getOutput(out));
	}
	
	@Test
	public void test_write_scalar() throws IOException {
		meta.add("key", "value");
		MetadataSerializer.write(meta, out);
		Assert.assertEquals("key=value\n", getOutput(out));
	}
	
	@Test
	public void test_write_list() throws IOException {
		meta.add("key", "v1");
		meta.add("key", "v2");
		MetadataSerializer.write(meta, out);
		Assert.assertEquals("key=[v1, v2]\n", getOutput(out));
	}
	
	private String getOutput(ByteArrayOutputStream o) {
		final String str = new String(o.toByteArray(), StandardCharsets.UTF_8);
		final StringBuilder b = new StringBuilder();
		for (String line : str.split("\n")) {
			line = line.replaceAll("#.*", "").trim();
			if (!line.isEmpty()) {
				b.append(line).append("\n");
			}
		}
		return b.toString();
	}
	
	private InputStream stream(final String stream) {
		final byte [] ascii = stream.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(ascii);
	}
}
