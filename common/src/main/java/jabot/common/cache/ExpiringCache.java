package jabot.common.cache;

import java.util.concurrent.atomic.AtomicReference;

import jabot.common.types.Interval;

/**
 * This class has two uses:
 * <ol>
 *   <li>Cache that holds certain value which expires overtime</li>
 *   <li>Simple tool that helps to perform some action with fixed delay</li>
 * </ol>
 * 
 * Sample usage (as cache):
 * <code><pre>
 * ExpiringCache&gt;String&gt; cache = new ExpiringCache&lt;&gt;(Interval.MINUTE);
 * ...
 * String value = cache.get();
 * if (value == null) {
 *   value = ... // recalculate value
 *   cache.set(value);
 * }
 * return value
 * </pre></code>
 * 
 * Sample usage (as fixed-delay limiter):
 * ExpiringCache&gt;Object&gt; cache = new ExpiringCache&lt;&gt;(Interval.SECOND);
 * cache.set(null);
 * ...
 * if (cache.isExpired()) {
 *   // perform an action
 *   cache.set(null);
 * }
 * 
 * @threadsafe
 */
public class ExpiringCache<V> {
	private final Interval refresh;
	private final AtomicReference<V> value = new AtomicReference<>();
	private volatile long refreshed = 0;
	
	public ExpiringCache(final Interval refresh) {
		this.refresh = refresh;
	}
	
	public V get() {
		final V ret = value.get();
		if (isExpired()) {
			// CAS to make sure we're not overwriting some other thread-refreshed value
			value.compareAndSet(ret, null);
			return null;
		}
		return ret;
	}

	public boolean isExpired() {
		final long elapsed = System.currentTimeMillis() - refreshed;
		return elapsed >= refresh.asMillis();
	}
	
	public void set(final V value) {
		this.value.set(value);
		refreshed = System.currentTimeMillis();
	}
}
