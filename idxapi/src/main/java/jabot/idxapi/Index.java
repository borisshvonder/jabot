package jabot.idxapi;

import java.util.Arrays;
import java.util.Collection;

/** 
 * An index can store an efficiently search for records. 
 */
public interface Index {
	/**
	 * Store document to an index. There are no guarantees that is will be immediately visible through the search
	 * methods unless {@link #commit()} is called 
	 * @param @notnull document this document will be FROZEN prior to storing it 
	 * @throws IndexingException if something went wrong, for ex if document has same non-multivalued field specified
	 *                           twice
	 */
	void store(Document document);
	
	/**
	 * Remove single record by pk.
	 * 
	 * This operation may not be carried out immediately and can be delayed until {@link #commit()} is called.
	 * 
	 * @param @notnull pk
	 */
	void removeByKey(Untokenized pk);
	
	/**
	 * Remove all records conforming to a query. No guarantees are made that this request will actually be executed
	 * until {@link #commit()} method is called.
	 * 
	 * This operation may not be carried out immediately and can be delayed until {@link #commit()} is called. This also
	 * means the documents may still be visible even using the remval query
	 * 
	 * @param @notnull search lucene-compliant search
	 */
	void removeByQuery(String search);

	
	/**
	 * Commit all stored documents to hard medium, applies all requested removals.
	 * 
	 * This will incur perfomance penalties and should not be done unless client absolutely needs to guarantee documents 
	 * are stored.
	 * 
	 * @throws DelayedIndexingException if documents that supposed to be stored can't really hit the medium for some 
	 *                                  reason
	 */
	void commit() throws DelayedIndexingException;
	
	
	/** 
	 * Perform a search
	 * @param @notnull search lucene-compliant search
	 * @param @notnull returnFields return these stored fields with the document (MUST be stored fields)
	 * @throws throws SearchException when query is incorrect or IOException happened (user should analyse getCause())
	 */
	default SearchResults search(String search, int offset, Field ... returnFields) {
		return search(search, Arrays.asList(returnFields), offset);
	}
	
	/** 
	 * Perform a search
	 * @param @notnull search lucene-compliant search
	 * @param @notnull returnFields return these stored fields with the document (MUST be stored fields)
	 * @param @offset return results at given offset (for paging)
	 * @throws throws SearchException when query is incorrect or IOException happened (user should analyse getCause())
	 */
	SearchResults search(String search, Collection<Field> returnFields, int offset);
	
}
