package jabot.metapi.tools;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.lang3.Validate;

/**
 * InputStream that transparently calculates digest over contents read 
 */
public class HashedStream extends InputStream {
	private final InputStream backend;
	private final MessageDigest digest;
	private byte [] hash;
	private long length;
	
	public HashedStream(final InputStream backend, final String digestAlgorithm) throws NoSuchAlgorithmException {
		Validate.notNull(backend, "backend cannot be null");
		Validate.notBlank(digestAlgorithm, "digestAlgorithm cannot be blank");
		
		this.backend = backend;
		this.digest = MessageDigest.getInstance(digestAlgorithm);
	}
	
	/**
	 * Consume remaining bytes in input stream and return the digest.
	 * 
	 * @return get the digest of the stream
	 * @throws IOException 
	 */
	public byte [] hash() throws IOException {
		if (hash == null) {
			if (read()>=0) {
				final byte [] buffer = new byte[4096];
				int read = read(buffer);
				while (read>=0) {
					read = read(buffer);
				}
			}
			hash = digest.digest();
		}
		return Arrays.copyOf(hash, hash.length);
	}
	
	/**
	 * @return number of bytes read so far
	 */
	public long getLength() {
		return length;
	}

	@Override
	public int read() throws IOException {
		final int ret = backend.read();
		if (ret>=0) {
			digest.update((byte) (ret & 0xFF));
			length++;
		}
		return ret;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		final int ret = backend.read(b, off, len);
		if (ret>0) {
			digest.update(b, off, ret);
			length+=ret;
		}
		return ret;
	}

	@Override
	public int available() throws IOException {
		return backend.available();
	}

	@Override
	public void close() throws IOException {
		backend.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

}
