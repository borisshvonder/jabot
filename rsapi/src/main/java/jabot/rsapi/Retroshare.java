package jabot.rsapi;

import java.io.IOException;
import java.util.List;

/**
 * High-level view of the retroshare application as it is seen by the bot.
 * 
 * The API is dumb and blindly does what is asked from it. It never masks messages or lobbies, it does not do any 
 * caching and/or throttling. 
 * 
 * More sophisticated deduping/caching/throttling techniques should applied on the client side. 
 * @threadsafe
 */
public interface Retroshare {
	
	/**
	 * @return all lobbies user subscribed to
	 * @throws ApiError
	 */
	List<Lobby> getSubscribedLobbies() throws IOException;
	
}
