package jabot.rsrest2;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import jabot.rsapi.ApiError;

public class ApiV2Test extends V2TestBase {
	@Override
	protected Application registerResources() {
		return new ResourceConfig(MockResource.class);
	}
	
	@Test
	public void testOk() {
		final ApiV2 fixture = new ApiV2(target("/api/v2/ok"));
		Assert.assertEquals("/api/v2/ok", fixture.getRoot().getUri().getPath());
		fixture.update();
	}
	
	@Test(expected=ApiError.class)
	public void testFail() {
		final ApiV2 fixture = new ApiV2(target("/api/v2/error"));
		Assert.assertEquals("/api/v2/error", fixture.getRoot().getUri().getPath());
		fixture.update();
	}

	@Path("/api/v2")
	public static class MockResource {

		@GET
		@Path("ok/statetokenservice")
		public Response ok() {
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'ok'}");
		}

		@GET
		@Path("error/statetokenservice")
		public Response error() {
			return JerseyTestUtils.json("{'data':[],'debug_msg':'','returncode':'fail'}");
		}
	}

}
