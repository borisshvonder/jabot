package jabot.marshall;

/**
 * Generic Marshall runtime exception
 */
public class MarshallException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MarshallException(final Throwable cause) {
		super(cause);
	}

}
