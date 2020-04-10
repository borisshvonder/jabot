package jabot.common.bytes;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.Validate;

/**
 * Byyytes! Let me at em!
 */
public final class Bts {
	private static final String HEXTABLE = "0123456789abcdef";

	public static String hex(final long val) {
		byte [] bytes = long2bytes(val);
		int firstNonZero = 0;
		while (firstNonZero<bytes.length && bytes[firstNonZero]==0) {
			firstNonZero++;
		}
		if (firstNonZero > 0) {
			if (firstNonZero==bytes.length) {
				return "0";
			}
			bytes = Arrays.copyOfRange(bytes, firstNonZero, bytes.length);
		}
		return hex(bytes);
	}

	public static long hex2long(final String hex) {
		String lowered = hex.trim().toLowerCase();
		Validate.notEmpty(lowered);
		Validate.isTrue(lowered.length()<=16, "Maximum length is {} which translates to {} bytes", lowered.length(), lowered.length()>>1);

		byte [] bytes = new byte[8];
		dehexFromLeftToRight(hex, lowered, bytes);
		return bytes2long(bytes);
	}

	public static String hex(byte[] bytes) {
		final char[] chars = new char[bytes.length << 1];

		int pos = 0;
		for (byte b : bytes) {
			chars[pos + 1] = HEXTABLE.charAt((byte) (b & 0xF));
			b >>= 4;
			chars[pos] = HEXTABLE.charAt((byte) (b & 0xF));
			pos += 2;
		}
		
		return new String(chars);
	}

	public static byte[] dehex(String hex) {
		final String lowered = hex.trim().toLowerCase();
		Validate.notEmpty(lowered);
		
		int bytesLen = (lowered.length()/2) + (lowered.length()%2);
		final byte[] ret = new byte[bytesLen];
		dehexFromLeftToRight(hex, lowered, ret);
		return ret;
	}

	private static void dehexFromLeftToRight(final String hex, final String lowered, final byte[] ret) {
		for (int i = ret.length-1, k=lowered.length()-1; k >= 0; i--, k-=2) {
			final char c1 = k>=1? lowered.charAt(k-1) : '0'; //padding
			final char c2 = lowered.charAt(k);

			int idx1 = HEXTABLE.indexOf(c1);
			Validate.isTrue(idx1>=0, "{} has non-hex chars: {} at pos {}", hex, c1, k-1);

			int idx2 = HEXTABLE.indexOf(c2);
			Validate.isTrue(idx2>=0, "{} has non-hex chars: {} at pos {}", hex, c2, k);

			int b = (idx1 << 4) | idx2;
			ret[i] = (byte) b;
		}
	}

	public static final byte[] long2bytes(final long l) {
		final byte[] bytes = new byte[8];
		ByteBuffer b = ByteBuffer.wrap(bytes);
		b.putLong(l);
		return bytes;
	}

	public static long bytes2long(byte[] eightBytes) {
		Validate.notNull(eightBytes, "eightBytes should not be null");
		Validate.isTrue(eightBytes.length == 8, "The length of eightBytes should be 8, actual={}", eightBytes.length);

		ByteBuffer b = ByteBuffer.wrap(eightBytes);
		return b.getLong();
	}

	private Bts(){}
}
