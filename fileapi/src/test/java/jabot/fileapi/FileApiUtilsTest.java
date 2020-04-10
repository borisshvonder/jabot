package jabot.fileapi;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;

import org.junit.Assert;
import org.junit.Test;

public class FileApiUtilsTest {
	@Test
	public void test_createURI() {
		Assert.assertEquals("file:///path/to/file", FileApiUtils.createURI("file", "file:///path/to/file").toString());
		Assert.assertEquals("file:///path/to/file", FileApiUtils.createURI(null, "file:///path/to/file").toString());
		Assert.assertEquals("file:/path/to/file", FileApiUtils.createURI("file", "/path/to/file").toString());
	}
	
	@Test
	public void test_createURI_normalizes_path() {
		Assert.assertEquals("file:/path/to/file", FileApiUtils.createURI("file", "/path//to/file").toString());
		Assert.assertEquals("file:/path/to/file", FileApiUtils.createURI("file", "/path/./to/file").toString());
	}
	
	@Test
	public void test_createURI_can_be_read_back_by_FileSystem() throws MalformedURLException, UnsupportedEncodingException {
		final String badsample = "/ с пробелами !/и дурацкими символами,.?.file";
		final URI uri = FileApiUtils.createURI("file", badsample);
		final String restored = URLDecoder.decode(uri.toURL().getFile(), "UTF-8");
		Assert.assertEquals(badsample, restored);
	}

	@Test
	public void test_createURI_does_not_touch_encoded_urls() {
		Assert.assertEquals("file:/path//to/file", FileApiUtils.createURI(null, "file:/path//to/file").toString());
		Assert.assertEquals("file:///path/./to/file", FileApiUtils.createURI(null, "file:///path/./to/file").toString());
		Assert.assertEquals("file://привет/мир", FileApiUtils.createURI(null, "file://привет/мир").toString());
	}
	
	@Test
	public void test_create_http() {
		Assert.assertEquals("http:///path/to/file", FileApiUtils.createURI("http", "/path//to/file").toString());
		Assert.assertEquals("http:///path/to/file", FileApiUtils.createURI("http", "/path/./to/file").toString());
	}
	
	@Test
	public void testNormalizePath() {
		Assert.assertEquals("/path/to/file", FileApiUtils.normalizePath("/path//to/file").toString());
		Assert.assertEquals("/path/to/file", FileApiUtils.normalizePath("/path/./to/file").toString());
		Assert.assertEquals("path/to/file", FileApiUtils.normalizePath("path/to/file").toString());
		Assert.assertEquals("path/to/dir/", FileApiUtils.normalizePath("path/to/dir/").toString());
	}
	
}
