package jabot.rsrest2;

import jabot.rsapi.ApiError;

public class ApiObjectV2 {
	private String returncode;

	public void setReturncode(String returncode) {
		this.returncode = returncode;
	}
	
	public void validate() {
		if (!"ok".equals(returncode)) {
			throw new ApiError("returncode: "+returncode);
		}
	}
}
