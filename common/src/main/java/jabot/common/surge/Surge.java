package jabot.common.surge;

import java.io.Closeable;

/**
 * A very simple stream of non-null objects that may have underlying resources that needs to be closed by the client.
 * 
 * After calling {@link #close()} method behavior of the {@link #next()} is unpredictable.
 * @threadunsafe unless EXPLICITLY told otherwise by the implementation
 */
public interface Surge<E> extends Closeable {
	/**
	 * @return next element or null if no more elements
	 */
	E next();
}
