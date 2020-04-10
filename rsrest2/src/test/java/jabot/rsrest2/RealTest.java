package jabot.rsrest2;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jabot.common.props.PropsConfig;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import jabot.rsapi.Retroshare;

public class RealTest {

	public static void main(String[] args) {
		try {
			run();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void run() throws IOException {
		final Retroshare rs = new RetroshareV2(new PropsConfig(), URI.create("http://127.0.0.1:9090/"));
		final List<Lobby> lobbies = rs.getSubscribedLobbies();
		for (Lobby lobby : lobbies) {
			System.out.println(lobby);
			final Set<String> messageIds = new HashSet<>();
			List<ReceivedMessage> messages = lobby.readMessages();
			while (!messages.isEmpty()) {
				for (ReceivedMessage m : messages) {
					if (!messageIds.add(m.getId())) {
						System.err.println("Message id="+m.getId()+" is duplicated");
					}
					System.out.println("\t"+m.toString().replaceAll("\n", "\n\t"));
				}
				messages = lobby.readMessages();
			}
			if (lobby.getId().equals("11742514967017937412")) {
				lobby.post("test9012");
			}
		}
	}


}
