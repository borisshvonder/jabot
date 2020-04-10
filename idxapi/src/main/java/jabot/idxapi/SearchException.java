package jabot.idxapi;

public class SearchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SearchException() {}

	public SearchException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public SearchException(final String message) {
		super(message);
	}

	public SearchException(final Throwable cause) {
		super(cause);
	}

	
}
