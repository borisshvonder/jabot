package jabot.marshall;

public class UnmarshallingException extends MarshallException {
	private static final long serialVersionUID = 1L;
	private final String json;
	
	public UnmarshallingException(final Throwable cause, final String json) {
		super(cause);
		this.json = json;
	}

	public String getJson() {
		return json;
	}
	
}
