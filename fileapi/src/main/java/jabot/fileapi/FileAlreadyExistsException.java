package jabot.fileapi;

import java.io.IOException;
import java.net.URI;

public class FileAlreadyExistsException extends FileApiException {
	private static final long serialVersionUID = 1L;

	public FileAlreadyExistsException(final URI path) {
		super(path, "File already exists");
	}
	
	public FileAlreadyExistsException(final URI path, final IOException cause) {
		super(path, cause);
	}

}
