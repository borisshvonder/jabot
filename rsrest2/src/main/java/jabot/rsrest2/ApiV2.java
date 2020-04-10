package jabot.rsrest2;

import javax.ws.rs.client.WebTarget;

class ApiV2 {
	private final WebTarget root;
	private final WebTarget tokenService;

	public ApiV2(final WebTarget root) {
		this.root = root;
		this.tokenService = root.path("statetokenservice");
	}

	public void update() {
		final ApiObjectV2 msg = tokenService.request().get(ApiObjectV2.class);
		msg.validate();
	}

	public WebTarget getRoot() {
		return root;
	}
}