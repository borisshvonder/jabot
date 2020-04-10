package jabot.rsrest2;

import java.io.IOException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.junit.Test;

import jabot.rsapi.ApiError;

public class RestV2UtilsTest {

	@Test(expected=ApiError.class)
	public void testPropagateNotFound() throws IOException {
		throw RestV2Utils.propagate(new NotFoundException());
	}

	@Test(expected=IOException.class)
	public void testPropagateProcessingException() throws IOException {
		throw RestV2Utils.propagate(new ProcessingException("testPropagateProcessingException"));
	}
	
	@Test(expected=RuntimeException.class)
	public void testRuntimeException() throws IOException {
		throw RestV2Utils.propagate(new RuntimeException());
	}
}
