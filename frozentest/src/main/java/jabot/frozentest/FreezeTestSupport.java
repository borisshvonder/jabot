package jabot.frozentest;

import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import jabot.frozen.Freezing;
import jabot.frozen.FrozenException;

public class FreezeTestSupport {
	/**
	 * This method facilitates unit testing of frozen objects. It supposed to be used like this 
	 * (refer to {@link FreezeTestSupportTest#testFrozen()}):
	 * <code><pre>
	 * public void testFrozen() {
	 *   final FrozenObj obj = new FrozenObj();
	 *   final FrozenObj copy = Freeze.unitTest(obj, o -> o.setValue(100));
	 *   Assert.assertEquals(100, copy.getValue());
	 * }
	 * </pre></code>
	 * @param object the object to test
	 * @param modifier modifier function that applies some change to the object
	 * @return @notnull defrosted object with the modifier applied
	 */
	@SuppressWarnings("unchecked")
	public static<T> T unitTest(final Freezing<T> object, final Consumer<T> modifier) {
		Validate.notNull(object, "object cannot be null");
		
		object.freeze();
		try {
			modifier.accept((T)object);
			throw new AssertionError("Frozen object "+object+" does not throw FrozenException on modification");
		} catch (FrozenException ex) {
			// Ignore
		}
		
		final T defrosted = object.defrost();
		
		if (defrosted == null) {
			throw new AssertionError("Frozen object "+object+" returned null from .defrost()");
		}
		
		if (defrosted == object) {
			throw new AssertionError("Frozen object "+object+" returned same instance from .defrost()");
		}
	
		if (((Freezing<?>)defrosted).isFrozen()) {
			throw new AssertionError("Frozen object "+object+" returned frozen instance "+defrosted+" from .defrost()");
		}
		
		modifier.accept(defrosted);
		return defrosted;
	}

}
