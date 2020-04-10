package jabot.common.props;

public class MissingPropertyException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String key;
	
	public MissingPropertyException(String key) {
		super(key);
		this.key = key;
	}

	public String getKey() {
		return key;
	}
	
}
