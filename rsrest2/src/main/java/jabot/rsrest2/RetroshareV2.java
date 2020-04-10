package jabot.rsrest2;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import jabot.common.props.PropsConfig;
import jabot.rsapi.Lobby;
import jabot.rsapi.Retroshare;

public class RetroshareV2 implements Retroshare {
	private final ApiV2 api;
	private final WebTarget lobbies;
	
	public RetroshareV2(final PropsConfig config, final URI webAddr) {
		this(createClient(config), webAddr);
	}
	
	private RetroshareV2(Client client, final URI webAddr) {
		this(client.target(webAddr).path("/api/v2"));
	}

	/**
	 * @VisibleForTesting
	 */
	RetroshareV2(final WebTarget apiRoot) {
		this.api = new ApiV2(apiRoot);
		this.lobbies = api.getRoot().path("chat/lobbies");
	}
	
	
	private static Client createClient(final PropsConfig config) {
		final ClientConfig clientConfig = new ClientConfig();
		configureClient(config, clientConfig);
		
		return ClientBuilder.newBuilder()
				.withConfig(clientConfig)
				.build();		
	}
	
	/**
	 * @VisibleForTesting
	 */
	static void configureClient(final PropsConfig config, final ClientConfig clientConfig) {
		clientConfig.register(LoggingFeature.class);
		for (final String key: config.keysWithPrefix("jersey.")) {
			String value = config.getString(key, null);
			if (value != null) {
				if (key.equals(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT)) {
					final LoggingFeature.Verbosity verbosity = LoggingFeature.Verbosity.valueOf(value);
					clientConfig.property(key, verbosity);
				} else {
					clientConfig.property(key, value);
				}
			}
		}
		clientConfig.register(new JacksonJsonProvider());
		clientConfig.register(ObjectMapperV2.class);
		clientConfig.register(JacksonFeature.class);
	}

	public List<Lobby> getSubscribedLobbies() throws IOException {
		try {
			api.update();

			LobbyData data = lobbies.request().get(LobbyData.class);
			data.validate();
			
			List<Lobby> result = new LinkedList<>();
			for (LobbyV2 lobby : data.getData()) {
				if (lobby.isSubscribed()) {
					lobby.setApi(api);
					result.add(lobby);
				}
			}
			
			return result;
		} catch (RuntimeException ex) {
			throw RestV2Utils.propagate(ex);
		}
	}
}

final class LobbyData extends ApiObjectV2 {
	private List<LobbyV2> data;
	
	public List<LobbyV2> getData() {
		return data;
	}

	public void setData(List<LobbyV2> data) {
		this.data = data;
	}
}
