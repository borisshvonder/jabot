package jabot.common.types;

public class IntervalFormatException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String text;
	
	public IntervalFormatException(String text) {
		this(text, null);
	}
	
	public IntervalFormatException(String text, Throwable cause) {
		super(text, cause);
		this.text = text;
	}

	public String getText() {
		return text;
	}

}
