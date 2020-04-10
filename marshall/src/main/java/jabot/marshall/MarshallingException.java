package jabot.marshall;

/**
 * 
 * Exception during mashalling an object
 *
 */
public class MarshallingException extends MarshallException {
	private static final long serialVersionUID = 1L;
	private final Object javabean;
	
	public MarshallingException(final Throwable cause, final Object javabean) {
		super(cause);
		this.javabean = javabean;
	}

	public Object getJavabean() {
		return javabean;
	}
	
	
}
