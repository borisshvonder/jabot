package jabot.fileapi;

import java.io.IOException;
import java.net.URI;

public class FileNotFoundException extends FileApiException {
	private static final long serialVersionUID = 1L;
	
	public FileNotFoundException(final URI path) {
		super(path, "Not found");
	}
	
	public FileNotFoundException(final URI path, final IOException cause) {
		super(path, cause);
	}
}
