package jabot.rsapi;

import java.io.IOException;
import java.util.List;

/**
 * Retroshare lobby
 * @threadsafe
 */
public interface Lobby {
	
	String getId();

	String getName();
	
	String getChatId();
	
	/**
	 * @nullable
	 * @return
	 */
	String getGxsId();
	
	boolean isSubscribed();
	
	/**
	 * Read messages from the lobby. Messages are discarded after they're read, best effort is made to deduplicate them.
	 * Implementation may choose to return ALL messages, including those {@link #post(String)}ed by you. Such messages
	 * will have {@link ReceivedMessage#isIncoming()}==false.
	 * 
	 * @return @notnull messages in this lobby. 
	 * @throws ApiError
	 */
	List<ReceivedMessage> readMessages() throws IOException;
	
	/**
	 * @throws ApiError
	 */
	void post(String message) throws IOException;

	/**
	 * Clear all messages from the lobby so they won't show up on readMessages.
	 * 
	 * The implementation may choose to simply read all messages and discard them if no more effective alternative
	 * exists
	 * 
	 * @throws ApiError
	 */
	void clearMessages() throws IOException;
}
