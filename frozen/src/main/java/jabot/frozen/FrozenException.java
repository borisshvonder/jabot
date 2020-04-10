package jabot.frozen;

/**
 * Signals the object is frozen and cannot be modified
 *
 */
public class FrozenException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final Object frozen;
	
	public FrozenException(Object obj) {
		super(obj.toString());
		this.frozen = obj;
	}

	public Object getFrozen() {
		return frozen;
	}
	
}
