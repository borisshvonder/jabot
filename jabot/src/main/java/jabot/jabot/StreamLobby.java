package jabot.jabot;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Validate;

import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import javolution.text.TextBuilder;

/**
 * Lobby based on input-output streams
 * 
 */
public class StreamLobby implements Lobby {
	private static final AtomicInteger MESSAGE_IDS = new AtomicInteger();
	private final Reader in;
	private final PrintStream out;
	private String id, name, chatId;
	private String userId, systemId;
	private TextBuilder incompleteLine = new TextBuilder();
	
	public StreamLobby(Reader in, PrintStream out) {
		Validate.notNull(in, "in cannot be null!");
		Validate.notNull(out, "out cannot be null!");
		
		this.in = in;
		this.out = out;
		
		userId = String.valueOf(System.identityHashCode(in));
		systemId = String.valueOf(System.identityHashCode(out));
		id = userId+"->"+systemId;
		name = id;
		chatId = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getChatId() {
		return chatId;
	}

	@Override
	public String getGxsId() {
		return null;
	}

	@Override
	public boolean isSubscribed() {
		return true;
	}

	@Override
	public synchronized List<ReceivedMessage> readMessages() throws IOException {
		final List<ReceivedMessage> ret = new LinkedList<>();
		String line = readLine();
		while (line != null) {
			ret.add(new SecureMessage(line));
			line = readLine();
		}
		return ret;
	}

	@Override
	public synchronized void post(String message) {
		String fullMsg = MessageFormat.format("{0}\t+{1}: {2}", systemId, MESSAGE_IDS.incrementAndGet(), message);
		out.println(fullMsg);
	}
	
	@Override
	public synchronized void clearMessages() throws IOException {
		String line = readLine();
		while (line != null) {
			line = readLine();
		}
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}
	
	/**
	 * non-blocking read line
	 */
	private String readLine() throws IOException {
		while (in.ready()) {
			final int c = in.read();
			if (c == -1) {
				break;
			} else if (c == '\n') {
				final String line = incompleteLine.toString();
				incompleteLine.delete(0, incompleteLine.length());
				return line;
			} else {
				incompleteLine.append((char)c);
			}
		}
		return null;
	}
	
	private final class SecureMessage implements ReceivedMessage {
		private final int id = MESSAGE_IDS.incrementAndGet();
		private final long timestamp = System.currentTimeMillis();
		private final String text;
		
		public SecureMessage(String text) {
			this.text = text;
		}

		@Override
		public String getId() {
			return String.valueOf(id);
		}

		@Override
		public String getAuthorId() {
			return systemId;
		}

		@Override
		public String getAuthorName() {
			return systemId;
		}

		@Override
		public Date getRecvTime() {
			return new Date(timestamp);
		}

		@Override
		public Date getSendTime() {
			return new Date(timestamp);
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public boolean isIncoming() {
			return true;
		}
		
	}

}
