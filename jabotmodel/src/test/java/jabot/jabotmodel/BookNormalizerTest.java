package jabot.jabotmodel;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class BookNormalizerTest {
	
	@Test
	public void testEmptyBook() {
		final Book book = new Book();
		BookNormalizer.normalize(book);
	}
	
	@Test
	public void testAllNormalizations() {
		final Book book = new Book();
		book.setTitle("<b>Title</b> <a href=\"somelink\">link</a> http://another-link");
		book.setTitleRu(book.getTitle());
		
		book.setAuthors(new LinkedList<>());
		book.setAuthorsRu(book.getAuthors());
		
		book.getAuthors().add("Diversus Retroshare");
		book.getAuthors().add("Bill, Mister <a href=\\\"somelink\\\">link</a> http://another-link");
		book.getAuthors().add("Diversus");
		book.getAuthors().add(",");
		book.getAuthors().add(",Diversus");
		book.getAuthors().add("Diversus,");
		book.getAuthors().add("неизвестен јвтор");
		book.getAuthors().add("неизвестен Автор");
		book.getAuthors().add("Автор Неизвестен");
		book.getAuthors().add("librusec Yesaul11");
		book.getAuthors().add("Warlock666 for Librusec");
		book.getAuthors().add("LibRusEc kit");
		book.getAuthors().add(null);
		book.getAuthors().add("Diversus Diversus Retroshare");
		book.getAuthors().add("look for Diversus Retroshare in the list of identities");
		book.getAuthors().add(UUID.randomUUID().toString());
		
		BookNormalizer.normalize(book);
		
		Assert.assertEquals("Title", book.getTitle());
		Assert.assertEquals("Title", book.getTitleRu());
		Assert.assertEquals(Arrays.asList("Mister Bill"), book.getAuthors());
		Assert.assertEquals(Arrays.asList("Mister Bill"), book.getAuthorsRu());
	}
	

	@Test
	public void testCanNormalizeUnmodifiable() {
		final Book book = new Book();
		book.setAuthors(Arrays.asList("Bill Man"));
		
		BookNormalizer.normalize(book);
		
		Assert.assertEquals(Arrays.asList("Bill Man"), book.getAuthors());
	}
}
