package jabot.rsrest2;

import java.io.IOException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import jabot.rsapi.ApiError;

final class RestV2Utils {
	
	/**
	 * Convert Jersey exception to either IOException (transiet) or ApiError (permanent) exception
	 * @param ex
	 * @return returns nothing cause it rethrows an exception, but its easier to coerce compiler that way
	 */
	static IOException propagate(RuntimeException ex) throws IOException {
		if (ex instanceof WebApplicationException) {
			throw new ApiError(ex);
		} else if (ex instanceof ProcessingException) {
			throw new IOException(ex);
		} else {
			throw ex;
		}
	}

	private RestV2Utils() {}
}
