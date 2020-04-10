package jabot.fileapi;

import java.io.IOException;
import java.net.URI;

public class NotADirectoryException extends FileApiException {
	private static final long serialVersionUID = 1L;

	public NotADirectoryException(final URI path, final IOException cause) {
		super(path, cause);
	}
	
	public NotADirectoryException(final URI path, final String message) {
		super(path, message);
	}
}
