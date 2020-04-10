package jabot.frozen;

/** Indicated a freezing type, the type which can be frozen and defrosted */
public interface Freezing<T> {
	
	/** @return true if this object has been frozen */
	boolean isFrozen();
	
	/** Freeze an object, don't allow any modifications to internal state */
	void freeze();
	
	/** Return a copy of the object which can be modified */
	T defrost();
}
