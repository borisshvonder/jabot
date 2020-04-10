package jabot.common.props;

public class BooleanFormatException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String text;
	
	public BooleanFormatException(String text) {
		super(text);
		this.text = text;
	}

	public String getText() {
		return text;
	}
	
}
