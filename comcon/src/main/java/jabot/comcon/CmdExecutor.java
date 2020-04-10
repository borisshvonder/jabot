package jabot.comcon;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import javolution.util.FastMap;

/**
 * 
 * @threadunsafe
 *
 */
public class CmdExecutor {
	private static final int CMD_LOOKEAHEAD=10;
	private static final Pattern TAGS=Pattern.compile("</?[^>]*>");
	private final ServiceCore services;
	private final FastMap<String, PrioritizedCmd> commands = new FastMap<>();
	private final FastMap<Lobby, Integer> lobbyPriorities = new FastMap<>();
	
	public CmdExecutor(ServiceCore services) {
		this.services = services;
	}

	/**
	 * Add command to the executor with the given priority
	 * @param priority 0-default, higher-more priority required, negative - less prioritized
	 * @param @notnull cmd 
	 * @throws IllegalArgumentException when trying to register same command twice
	 */
	public void addCmd(int priority, Cmd cmd) {
		Validate.notNull(cmd, "cmd cannot be null");
		
		final PrioritizedCmd prioritized = new PrioritizedCmd(cmd, priority);
		if (commands.put(prioritized.getFullName(), prioritized)!=null) {
			throw new IllegalArgumentException("You have tried to register command "+cmd.getName()+" twice");
		}
	}
	
	/**
	 * By default, all lobbies have priority=0. You can change it here.
	 * 
	 * The lobby is only allowed to execute a command if lobbyPriority >= cmdPriority
	 * @param @notnull lobby
	 * @param priority
	 */
	public void setLobbyPriority(Lobby lobby, int priority) {
		Validate.notNull(lobby, "lobby cannot be null");
		
		lobbyPriorities.put(lobby, priority);
	}
	
	public Collection<PrioritizedCmd> listCommands() {
		return Collections.unmodifiableCollection(commands.values());
	}
	
	public int getLobbyPriority(Lobby lobby) {
		Integer lobbyPriority = lobbyPriorities.get(lobby);
		return lobbyPriority == null ? 0 : lobbyPriority;
	}
	
	public void processMessage(Lobby lobby, ReceivedMessage message) throws Exception {
		if (message.isIncoming() || services.getConfig().isDebugMode()) {
			String text = message.getText();
			if (looksLikeCommand(text)) {
				text = detoxify(text);
				if (text.startsWith("/")) {
					processCommand(lobby, message, text);
				}
			}
		}
	}

	private void processCommand(Lobby lobby, ReceivedMessage message, String commandText) throws IOException, Exception {
		String [] commandAndArguments = commandText.split("\\s+", 2);
		String command = commandAndArguments[0].toUpperCase();
		
		PrioritizedCmd cmd = commands.get(command);
		if (cmd != null) {
			if (cmd.getPriority()>getLobbyPriority(lobby)) {
				lobby.post("ACCESS DENIED");
			} else {
				String [] args = commandAndArguments.length == 1 ? new String[0] : commandAndArguments[1].split("\\s+");
				cmd.getCmd().execute(services, lobby, message, Arrays.asList(args));
			}
		}
	}
	
	private boolean looksLikeCommand(final String text) {
		if (text == null) {
			return false;
		}
		// fail fast on most messages (conserve CPU)
		for (int i=0; i<CMD_LOOKEAHEAD && i<text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '/') {
				return true;
			}
		}
		return false;
	}
	
	private String detoxify(final String cmd) {
		if (cmd == null) {
			return "";
		}
		String ret = cmd;
		ret = TAGS.matcher(ret).replaceAll("");
		ret = ret.trim();
		return ret;
	}

	public static final class PrioritizedCmd {
		private final int priority;
		private final Cmd cmd;
	
		public PrioritizedCmd(Cmd cmd, int priority) {
			this.priority = priority;
			this.cmd = cmd;
		}

		public int getPriority() {
			return priority;
		}

		public Cmd getCmd() {
			return cmd;
		}
		
		public String getFullName() {
			return "/"+cmd.getName();
		}
	}
}
