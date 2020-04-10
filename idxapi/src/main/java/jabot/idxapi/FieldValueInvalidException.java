package jabot.idxapi;

public class FieldValueInvalidException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final Field field;
	private final Object value;
	
	public FieldValueInvalidException(final Field field, final Object value) {
		super(field+":"+value);
		this.field = field;
		this.value = value;
	}

	public Field getField() {
		return field;
	}

	public Object getValue() {
		return value;
	}
}
