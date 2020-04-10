package jabot.idxsolr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;

/**
 * Decouples {@link SolrIndex} from the actual Solr instance and enables unit testing
 */
public interface SolrClient {
	/**
	 * @param @notnull query
	 * @return @notnull response
	 * @throws IOException 
	 * @throws SolrServerException 
	 */
	QueryResponse search(SolrQuery query) throws SolrServerException, IOException;
	
	/**
	 * @param @notnull req
	 * @return @notnull response
	 */
	NamedList<Object> update(UpdateRequest req) throws SolrServerException, IOException;
}
