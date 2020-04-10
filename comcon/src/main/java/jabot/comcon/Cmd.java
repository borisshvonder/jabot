package jabot.comcon;

import java.util.List;

import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public interface Cmd {
	/**
	 * @return @notnull command name, for ex "VERSION"
	 */
	String getName();
	
	/**
	 * @return @nullable long command description
	 */
	String getHelp();
	
	/**
	 * Execute a command with given arguments that was initiated by a message in a lobby
	 * 
	 * @param @notnull core service core for all required services
	 * @param @notnull lobby the lobby in which command was issued
	 * @param @notnull message message which contained a command
	 * @param @notnull args any parameters after the command name
	 */
	void execute(ServiceCore core, Lobby lobby, ReceivedMessage message, List<String> args) throws Exception;
}
