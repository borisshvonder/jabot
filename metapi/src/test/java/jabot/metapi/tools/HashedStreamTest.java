package jabot.metapi.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("resource")
public class HashedStreamTest {
	private static final String BROWN_FOX="A quik brown for jumped over something not so brown.";
	
	@Test
	public void test_empty_MD5() throws NoSuchAlgorithmException, IOException {
		doTest("", "MD5");
	}
	
	@Test
	public void test_empty_SHA1() throws NoSuchAlgorithmException, IOException {
		doTest("", "SHA1");
	}

	@Test
	public void test_A_MD5() throws NoSuchAlgorithmException, IOException {
		doTest("A", "MD5");
	}
	
	@Test
	public void test_A_SHA1() throws NoSuchAlgorithmException, IOException {
		doTest("A", "SHA1");
	}
	
	@Test
	public void test_Hello_MD5() throws NoSuchAlgorithmException, IOException {
		doTest("Hello", "MD5");
	}
	
	@Test
	public void test_Hello_SHA1() throws NoSuchAlgorithmException, IOException {
		doTest("Hello", "SHA1");
	}
	
	@Test
	public void test_BROWN_FOX_MD5() throws NoSuchAlgorithmException, IOException {
		doTest(BROWN_FOX, "MD5");
	}
	
	@Test
	public void test_BROWN_FOX_SHA1() throws NoSuchAlgorithmException, IOException {
		doTest(BROWN_FOX, "SHA1");
	}
	
	@Test
	public void test_rawbytes_MD5() throws NoSuchAlgorithmException, IOException {
		doTest(bytes(0xCA, 0xFE, 0xAA, 0x55), "MD5");
	}
	
	@Test
	public void test_rawbytes_SHA1() throws NoSuchAlgorithmException, IOException {
		doTest(bytes(0xCA, 0xFE, 0xAA, 0x55), "SHA1");
	}
	
	@Test
	public void test_cannedAnswer_MD5() throws NoSuchAlgorithmException, IOException {
		final byte [] data = "Hello".getBytes(StandardCharsets.UTF_8);
		// $  echo -n 'Hello' | openssl md5
		// (stdin)= 8b1a9953c4611296a827abf8c47804d7
		doTest(data, "MD5", bytes(0x8b, 0x1a, 0x99, 0x53, 0xc4, 0x61, 0x12, 0x96, 0xa8, 0x27, 0xab, 0xf8, 0xc4, 0x78, 0x04, 0xd7));
	}
	
	@Test
	public void test_cannedAnswer_SHA1() throws NoSuchAlgorithmException, IOException {
		final byte [] data = "Hello".getBytes(StandardCharsets.UTF_8);
		// $ echo -n 'Hello' | openssl sha1
		// (stdin)= f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0
		doTest(data, "SHA1", bytes(0xf7, 0xff, 0x9e, 0x8b, 0x7b, 0xb2, 0xe0, 0x9b, 0x70, 0x93, 0x5a, 0x5d, 0x78, 0x5e, 0x0c, 0xc5, 0xd9, 0xd0, 0xab, 0xf0));
	}

	private void doTest(final String text, final String algo) throws NoSuchAlgorithmException, IOException {
		final byte [] data = text.getBytes(StandardCharsets.UTF_8);
		doTest(data, algo);
	}
	
	private void doTest(final byte [] data, final String algo) throws NoSuchAlgorithmException, IOException {
		final MessageDigest digest = MessageDigest.getInstance(algo);
		digest.update(data);
		final byte [] expectedDigest = digest.digest();

		final MessageDigest digest2 = MessageDigest.getInstance(algo);
		digest2.update(Arrays.copyOf(data, data.length+10), 0, data.length);
		Assert.assertArrayEquals("Algoritm is misbehaving", expectedDigest, digest2.digest());
		
		doTest(data, algo, expectedDigest);
	}
	
	private void doTest(final byte [] data, final String algo, final byte [] expectedDigest) 
			throws IOException, NoSuchAlgorithmException 
	{
		for (int i=0; i<10; i++) {
			final ByteArrayInputStream in = new ByteArrayInputStream(data);
			final HashedStream stream = new HashedStream(in, algo);
			if (i > 0) {
				stream.read();
				final byte [] buffer = new byte[i];
				stream.read(buffer);
				stream.skip(i/2);
			}
			final byte [] actual = stream.hash();
			Assert.assertArrayEquals("At i="+i, expectedDigest, actual);
			Assert.assertEquals("length At i="+i, data.length, stream.getLength());
		}
	}
	
	private static byte [] bytes (int ... bytes) {
		final byte [] ret = new byte [bytes.length];
		int i=0;
		for (final int b : bytes) {
			ret[i++] = (byte) (b & 0xFF);
		}
		return ret;
	}
	
	@Test
	public void testMarkNotSupported() throws NoSuchAlgorithmException, IOException {
		final ByteArrayInputStream in = new ByteArrayInputStream(new byte[100]);
		final HashedStream stream = new HashedStream(new BufferedInputStream(in), "MD5");
		Assert.assertFalse(stream.markSupported());
		stream.mark(1);
		try {
			stream.reset();
			Assert.fail();
		} catch (final IOException ex){}
	}
	
}
