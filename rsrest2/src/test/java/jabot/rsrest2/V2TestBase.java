package jabot.rsrest2;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import jabot.common.props.PropsConfig;

abstract class V2TestBase extends JerseyTest {

	@Override
	protected Application configure() {
		// Enable parallel testing
		forceSet(TestProperties.CONTAINER_PORT, "0");
		
		enable(TestProperties.LOG_TRAFFIC);
		enable(TestProperties.DUMP_ENTITY);

		return registerResources();
	}

	abstract Application registerResources();

	@Override
	protected void configureClient(ClientConfig config) {
		RetroshareV2.configureClient(new PropsConfig(), config);
	}

}
