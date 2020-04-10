package jabot.idxapi;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

/** Safe wrapper around byte[] array 
 * @immutable
 * */
public class Binary {
	/** @notnull binary array */
	private final byte [] data;

	public Binary(final byte[] data) {
		Validate.notNull(data, "data cannot be null");
		
		this.data = Arrays.copyOf(data, data.length);
	}
	
	/**
	 * @param @nullable data
	 * @return null if data==null or Binary otherwise
	 */
	public static Binary of(final byte[] data) {
		return data == null ? null : new Binary(data);
	}

	public byte[] getData() {
		return Arrays.copyOf(data, data.length);
	}
	
	
}
