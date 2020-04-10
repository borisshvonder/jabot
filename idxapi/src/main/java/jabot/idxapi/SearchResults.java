package jabot.idxapi;

import jabot.common.surge.Surge;

/**
 * note that {@link Surge#next()} MAY throw SearchException when query is incorrect or IOException happened 
 * (user should analyse getCause())
 *
 */
public interface SearchResults extends Surge<Document> {
	/**
	 * Estimate total number of documents in the result. Typically, more results = less precise count
	 */
	long estimateTotalResults();
}
