package jabot.frozen;

/**
 * Utility class for supporting {@Lin Freezing} objects
 *
 */
public final class Freeze {
	
	/**
	 * Use this at the start of any of your setter methods:
	 * <code>
	 *   ensureNotFrozen(this);
	 * </code>
	 * @param @notnull object
	 * @throws FrozenException when object is frozen
	 */
	public static void ensureNotFrozen(final Freezing<?> object) {
		if (object.isFrozen()) {
			throw new FrozenException(object);
		}
	}
	
	
	private Freeze() {}
}
