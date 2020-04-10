package jabot.rsrest2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import jabot.marshall.Marshall;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

class LobbyV2 implements Lobby {
	private static final Logger LOG = LoggerFactory.getLogger(LobbyV2.class);
	private static final int MAX_MESSAGE_LEN=2048;
	private final BeginAfter lastMessage = new BeginAfter();
	private String id;

	private String name;

	@JsonProperty("chat_id")
	private String chatId;
	
	@JsonProperty("gxs_id")
	private String gxsId;

	private boolean subscribed;
	
	private WebTarget messages;
	private WebTarget sendMessage;
	private WebTarget clearMessages;
	
	public void setApi(ApiV2 api) {
		messages = api.getRoot().path("chat/messages/"+chatId);
		sendMessage = api.getRoot().path("chat/send_message");
		clearMessages = api.getRoot().path("chat/clear_lobby");
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getChatId() {
		return chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	public String getGxsId() {
		return gxsId;
	}

	public void setGxsId(String gxsId) {
		this.gxsId = gxsId;
	}

	@Override
	public boolean isSubscribed() {
		return subscribed;
	}
	
	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}
	
	@Override
	public synchronized List<ReceivedMessage> readMessages() throws IOException {
		try {
			final ReceivedMessageData resp = messages.request().post(Entity.entity(lastMessage, MediaType.APPLICATION_JSON), 
					ReceivedMessageData.class);
			// Do not validate, will fail if no messages
			if (!resp.getData().isEmpty()) {
				ReceivedMessage last = resp.getData().get(resp.getData().size()-1);
				lastMessage.setBeginAfter(last.getId());
			}
			return new ArrayList<ReceivedMessage>(resp.getData());
		} catch (RuntimeException ex) {
			throw RestV2Utils.propagate(ex);
		}
	}

	@Override
	public void post(final String message) throws IOException {
		try {
			final SendMessage msg = new SendMessage();
			msg.setChatId(chatId);

			for (final String messageChunk : RSUtils.chopMessage(message, MAX_MESSAGE_LEN)) {
				msg.setMsg(messageChunk);
				
				final String marshalled = Marshall.get().toJson(msg);
				LOG.trace("POST {}", marshalled);
				
				final Entity<String> entity = Entity.entity(marshalled, MediaType.APPLICATION_JSON);
				final ApiObjectV2 resp = sendMessage.request().post(entity, ApiObjectV2.class);
				resp.validate();
			}
		} catch (RuntimeException ex) {
			throw RestV2Utils.propagate(ex);
		}
	}

	private Object chopMessage(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearMessages() throws IOException {
		try {
			LobbyId param = new LobbyId();
			param.setId(id);
			final ApiObjectV2 resp = clearMessages.request().post(Entity.entity(param, MediaType.APPLICATION_JSON), ApiObjectV2.class);
			resp.validate();
		} catch (RuntimeException ex) {
			throw RestV2Utils.propagate(ex);
		}
		// Clearing is not always reliable
		readMessages();
	}

	@Override
	public String toString() {
		return getName()+"[id="+id+"]";
	}

}

final class SendMessage {
	@JsonProperty("chat_id")
	private String chatId;
	
	private String msg;

	public String getChatId() {
		return chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
}

final class BeginAfter {
	@JsonProperty("begin_after")
	private String beginAfter;

	public String getBeginAfter() {
		return beginAfter;
	}

	public void setBeginAfter(String beginAfter) {
		this.beginAfter = beginAfter;
	}
}

final class LobbyId {
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

final class ReceivedMessageData {
	private List<ReceivedMessageV2> data;

	public List<ReceivedMessageV2> getData() {
		return data;
	}

	public void setData(List<ReceivedMessageV2> data) {
		this.data = data;
	}
}
