package jabot.jindex;

import jabot.common.surge.Surge;

public interface JIndexResults<T> extends Surge<T> {
	/**
	 * Estimate total number of documents in the result. Typically, more results = less precise count
	 */
	long estimateTotalResults();
}
