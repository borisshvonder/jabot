package jabot.jabotmodel;

import java.util.List;

import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;
import jabot.jindex.ModelIntrospector;
import jabot.jindex.ModelIntrospector.Mapper;
import jabot.jindex.ModelMappersInventory;

public class Book extends File {
	static {
		final ModelIntrospector<Book> introspector = new ModelIntrospector<>(Book.class);
		introspector.getMapping("filename").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("filenameRu").setStorage(Storage.INDEXED);
		introspector.getMapping("length").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("sha1").setStorage(Storage.STORED);
		introspector.getMapping("contentType").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("contentEncoding").setStorage(Storage.STORED);
		introspector.getMapping("isbn").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("years").setType(Type.INT).setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("title").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("titleRu").setType(Type.STRING_RU);
		introspector.getMapping("annotation").setStorage(Storage.INDEXED);
		introspector.getMapping("annotationRu").setType(Type.STRING_RU);
		introspector.getMapping("authors").setStorage(Storage.STORED_INDEXED).setType(Type.STRING);
		introspector.getMapping("authorsRu").setType(Type.STRING_RU);
		
		// Storing location is insecure: reveals host paths
		introspector.getMapping("location").setStorage(Storage.INDEXED);
		
		// Turn off searching in rawText by default (for now)
		introspector.getMapping("rawText").setDefaultField(false);
		introspector.getMapping("rawTextRu").setType(Type.STRING_RU).setDefaultField(false);
		
		final Mapper<Book> mapper = introspector.buildMapper();
		ModelMappersInventory.registerMapper(mapper);
	}

	private String title;
	private String titleRu;
	private List<String> authors;
	private List<String> authorsRu;
	private String annotation;
	private String annotationRu;
	private String isbn;
	private List<Integer> years;
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitleRu() {
		return titleRu;
	}

	public void setTitleRu(String titleRu) {
		this.titleRu = titleRu;
	}

	public List<String> getAuthors() {
		return authors;
	}
	
	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	
	public List<String> getAuthorsRu() {
		return authorsRu;
	}

	public void setAuthorsRu(List<String> authorsRu) {
		this.authorsRu = authorsRu;
	}

	public String getIsbn() {
		return isbn;
	}
	
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public List<Integer> getYears() {
		return years;
	}

	public void setYears(List<Integer> years) {
		this.years = years;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getAnnotationRu() {
		return annotationRu;
	}

	public void setAnnotationRu(String annotationRu) {
		this.annotationRu = annotationRu;
	}

	
}
