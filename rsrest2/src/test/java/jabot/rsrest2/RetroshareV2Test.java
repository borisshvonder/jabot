package jabot.rsrest2;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import jabot.rsapi.ApiError;
import jabot.rsapi.Lobby;

public class RetroshareV2Test extends V2TestBase {

	@Override
	Application registerResources() {
		return new ResourceConfig(MockResource.class, MockResource2.class);
	}
	
	@Test
	public void testGetSubscribedLobbies() throws IOException {
		final RetroshareV2 fixture = new RetroshareV2(target("/api/v2"));
		
		final List<Lobby> lobbies = fixture.getSubscribedLobbies();
		Assert.assertEquals(1, lobbies.size());
		
		final Lobby l = lobbies.get(0);
		Assert.assertEquals("BroadCast", l.getName());
		Assert.assertEquals("B", l.getChatId());
		Assert.assertEquals("00000000000000000000000000000000", l.getGxsId());
		Assert.assertEquals("0", l.getId());
		Assert.assertEquals(true, l.isSubscribed());
	}
	
	@Test(expected=ApiError.class)
	public void testExceptionInGetSubscribedLobbies() throws IOException {
		final RetroshareV2 fixture = new RetroshareV2(target("/missing"));
		
		fixture.getSubscribedLobbies();
	}
	
	@Test(expected=ApiError.class)
	public void testExceptionInGetSubscribedLobbiesWithCorrectTokenService() throws IOException {
		final RetroshareV2 fixture = new RetroshareV2(target("/api/v2-tokenonly"));
		
		fixture.getSubscribedLobbies();
	}

	@Path("/api/v2")
	public static class MockResource {

		@GET
		@Path("statetokenservice")
		public Response ok() {
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'ok'}");
		}

		@GET
		@Path("chat/lobbies")
		public Response error() throws IOException {
			return JerseyTestUtils.jsonFromResource("/RetroshareV2Test/lobbies.json");
		}
	}
	
	@Path("/api/v2-tokenonly")
	public static class MockResource2 {

		@GET
		@Path("statetokenservice")
		public Response ok() {
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'ok'}");
		}
	}

}
