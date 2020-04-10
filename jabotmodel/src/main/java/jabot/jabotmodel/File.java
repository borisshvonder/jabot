package jabot.jabotmodel;

import java.net.URI;

import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;
import jabot.idxapi.Untokenized;
import jabot.jindex.ModelIntrospector;
import jabot.jindex.ModelIntrospector.Mapper;
import jabot.jindex.ModelMappersInventory;

public class File {
	static {
		final ModelIntrospector<File> introspector = new ModelIntrospector<>(File.class);
		introspector.getMapping("filename").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("filenameRu").setStorage(Storage.INDEXED);
		introspector.getMapping("length").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("sha1").setStorage(Storage.STORED);
		introspector.getMapping("contentType").setStorage(Storage.STORED_INDEXED);
		introspector.getMapping("contentEncoding").setStorage(Storage.STORED);

		// Storing location is insecure: reveals host paths
		introspector.getMapping("location").setStorage(Storage.INDEXED);
		
		// Turn off searching in rawText by default (for now)
		introspector.getMapping("rawText").setDefaultField(false);
		introspector.getMapping("rawTextRu").setType(Type.STRING_RU).setDefaultField(false);
		
		final Mapper<File> mapper = introspector.buildMapper();
		ModelMappersInventory.registerMapper(mapper);
	}
	
	private String filename;
	private String filenameRu;
	private long length;
	private URI location;
	private Untokenized sha1;
	private String contentType;
	private String contentEncoding;
	private String rawText;
	private String rawTextRu;
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFilenameRu() {
		return filenameRu;
	}

	public void setFilenameRu(String filenameRu) {
		this.filenameRu = filenameRu;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public URI getLocation() {
		return location;
	}
	
	public void setLocation(URI location) {
		this.location = location;
	}
	
	public Untokenized getSha1() {
		return sha1;
	}
	
	public void setSha1(Untokenized sha1) {
		this.sha1 = sha1;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public String getRawTextRu() {
		return rawTextRu;
	}

	public void setRawTextRu(String rawTextRu) {
		this.rawTextRu = rawTextRu;
	}
}
