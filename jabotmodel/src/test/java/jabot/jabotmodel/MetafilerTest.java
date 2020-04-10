package jabot.jabotmodel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;
import jabot.metapi.NotableKeys;
import jabot.metika.TikaExtractor;

public class MetafilerTest {
	private Metafiler fixture;
	
	@Before
	public void setUp() {
		fixture = new MetafilerImpl(new TikaExtractor());
	}
	
	@Test
	public void test_01_helloworld_txt() throws IOException, ExtractionException {
		final File file = fixture.sample(file("01.helloworld.txt"));
		Assert.assertEquals("01.helloworld.txt", file.getFilename());
		Assert.assertNotNull(file.getLocation());
		Assert.assertTrue(file.getRawText().length()>10);
		Assert.assertNotNull(file.getSha1());
		Assert.assertEquals("text/plain; charset=ISO-8859-1", file.getContentType());
		Assert.assertEquals(13L, file.getLength());
	}
		
	@Test
	public void test_02_canonic_russian_fb2() throws IOException, ExtractionException {
		final Book book = (Book) fixture.sample(file("02.canonic.russian.fb2"));
		Assert.assertEquals("02.canonic.russian.fb2", book.getFilename());
		Assert.assertNotNull(book.getLocation());
		Assert.assertTrue(book.getRawText().length()>128);
		Assert.assertTrue(book.getAnnotation().length()>128);
		Assert.assertNotNull(book.getSha1());
		Assert.assertEquals("application/x-fictionbook+xml", book.getContentType());
		Assert.assertEquals("Война и мир", book.getTitle());
		Assert.assertEquals(Arrays.asList(1957), book.getYears());

		Assert.assertEquals("[Лев Николаевич Толстой, GribUser grib@gribuser.ru]", 
				book.getAuthors().toString());
		Assert.assertEquals(29821L, book.getLength());
	}
	
	@Test
	public void test_04_alice_in_wonderland_pdf() throws IOException, ExtractionException {
		final Book book = (Book) fixture.sample(file("04-alice-in-wonderland.pdf"));
		Assert.assertEquals("04-alice-in-wonderland.pdf", book.getFilename());
		Assert.assertNotNull(book.getLocation());
		Assert.assertTrue(book.getRawText().length()>128);
		Assert.assertTrue(book.getAnnotation().length()>128);
		Assert.assertNotNull(book.getSha1());
		Assert.assertEquals("application/pdf", book.getContentType());
		Assert.assertEquals("Alice's Adventures in Wonderland, by Lewis Carroll", book.getTitle());

		Assert.assertEquals("[Chuck]", book.getAuthors().toString());
		Assert.assertEquals(711671L, book.getLength());
	}
	
	@Test
	public void test_05_russian_scan_pdf() throws IOException, ExtractionException {
		final Metadata meta = new Metadata();
		meta.set(NotableKeys.LANGUAGES, "rus");
		
		final Book book = (Book) fixture.sample(file("05-russian-scan.pdf"), meta);
		Assert.assertEquals("05-russian-scan.pdf", book.getFilename());
		Assert.assertNotNull(book.getLocation());
		Assert.assertEquals(161911L, book.getLength());

		// only with tesseract 
		// Assert.assertTrue(book.getRawText().length()>128);
		// Assert.assertTrue(book.getAnnotation().length()>128);
	}
	
	@Test
	public void test_06_sample_djvu() throws IOException, ExtractionException {
		final Metadata meta = new Metadata();
		meta.set(NotableKeys.LANGUAGES, "eng");
		
		final Book book = (Book) fixture.sample(file("06-sample.djvu"), meta);
		Assert.assertEquals("06-sample.djvu", book.getFilename());
		Assert.assertNotNull(book.getLocation());
		Assert.assertEquals(462539L, book.getLength());
	}
	
	private URI file(String name) {
		final String fullPath = "/"+getClass().getSimpleName()+"/"+name;
		try {
			return getClass().getResource(fullPath).toURI();
		} catch (final URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}
}
