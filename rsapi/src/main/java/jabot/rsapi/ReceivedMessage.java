package jabot.rsapi;

import java.util.Date;

/**
 * message received from the lobby. MAY include messages send by you!
 * @threadsafe
 *
 */
public interface ReceivedMessage {
	String getId();

	String getAuthorId();

	String getAuthorName();
	
	Date getRecvTime();
	
	Date getSendTime();
	
	String getText();
	
	/**
	 * @return true if message was sent to lobby by someone else, false if it was sent by us.
	 */
	boolean isIncoming();
}
