package jabot.rsrest2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import jabot.rsapi.ApiError;
import jabot.rsapi.ReceivedMessage;

public class LobbyV2Test extends V2TestBase{
	@Override
	Application registerResources() {
		return new ResourceConfig(MockResource.class);
	}
	
	@Test
	public void testToString() {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setName("name1");
		fixture.setId("id1");
		
		Assert.assertEquals("name1[id=id1]", fixture.toString());
	}
	
	@Test
	public void testGetMessages() throws IOException {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setChatId("chat-id");
		fixture.setApi(new ApiV2(target("/api/v2")));
		
		List<ReceivedMessage> messages = fixture.readMessages();
		Assert.assertEquals(21, messages.size());

		ReceivedMessage m0 = messages.get(0);
		Assert.assertTrue(m0.isIncoming());
		Assert.assertEquals("Babushka", m0.getAuthorName());
		Assert.assertEquals("853967166", m0.getId());
		Assert.assertEquals("f06c3b7c4903d9b768054877aa5fe478", m0.getAuthorId());
		Assert.assertEquals(new Date(1489614332000L), m0.getRecvTime());
		Assert.assertEquals(new Date(1489614330000L), m0.getSendTime());
		Assert.assertEquals("i have no use for a stationary phone line", m0.getText());

		Assert.assertFalse(messages.get(1).isIncoming());
}
	
	@Test
	public void testBeginAfter() throws IOException {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setChatId("chat-id");
		fixture.setApi(new ApiV2(target("/api/v2")));
		
		List<ReceivedMessage> messages = fixture.readMessages();
		Assert.assertEquals(21, messages.size());
		
		fixture.readMessages();
		String beginAfter = target("/api/v2/chat/messages/chat-id/begin_after").request().get(String.class);
		Assert.assertEquals("{\"begin_after\":\"3346569043\"}", beginAfter);
	}
	
	@Test(expected=ApiError.class)
	public void testNotFoundExceptionWhileGetMessages() throws IOException {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setChatId("chat-id-does-not-exists");
		fixture.setApi(new ApiV2(target("/api/v2")));
		
		fixture.readMessages();
	}
	
	@Test
	public void testPostMessage() throws IOException {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setChatId("chat-id");
		fixture.setApi(new ApiV2(target("/api/v2")));
		
		fixture.post("Hello, world!");
		String message = target("/api/v2/chat/messages/send_message/posted").request().get(String.class);
		Assert.assertTrue(message.indexOf("Hello, world!")>0);
	}
	
	@Test
	public void testClearMessages() throws IOException {
		final LobbyV2 fixture = new LobbyV2();
		fixture.setChatId("chat-id");
		fixture.setId("lobby1");
		fixture.setApi(new ApiV2(target("/api/v2")));
		
		fixture.clearMessages();
		String message = target("/api/v2/chat/clear_lobby/cleared").request().get(String.class);
		Assert.assertTrue(message.indexOf("lobby1")>0);
	}

	@Path("/api/v2")
	@Singleton
	public static class MockResource {
		private final AtomicReference<JsonNode> beginAfter = new AtomicReference<>();
		private final AtomicReference<JsonNode> posted = new AtomicReference<>();
		private final AtomicReference<JsonNode> cleared = new AtomicReference<>();

		@POST
		@Path("chat/messages/chat-id")
		public Response messages(final InputStream stream) throws IOException {
			beginAfter.set(JerseyTestUtils.readUTF8JsonFromPost(stream));

			return JerseyTestUtils.jsonFromResource("/LobbyV2Test/messages.json");
		}
		
		@GET
		@Path("chat/messages/chat-id/begin_after")
		public String getBeginAfter() {
			return beginAfter.get().toString();
		}

		@POST
		@Path("chat/send_message")
		public Response post(final InputStream stream) {
			posted.set(JerseyTestUtils.readUTF8JsonFromPost(stream));
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'ok'}");
		}
		
		@GET
		@Path("chat/messages/send_message/posted")
		public String getPosted() {
			return posted.get().toString();
		}
		
		@POST
		@Path("chat/clear_lobby")
		public Response clear(final InputStream stream) {
			cleared.set(JerseyTestUtils.readUTF8JsonFromPost(stream));
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'ok'}");
		}
		
		@GET
		@Path("chat/clear_lobby/cleared")
		public String cleared(final InputStream stream) {
			return cleared.get().toString();
		}
	}
}
