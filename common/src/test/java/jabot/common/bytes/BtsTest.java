package jabot.common.bytes;

import org.junit.Assert;
import org.junit.Test;

public class BtsTest {
	@Test
	public void testHex() {
		Assert.assertEquals("", Bts.hex(bytes()));
		Assert.assertEquals("00", Bts.hex(bytes(0)));
		Assert.assertEquals("0000", Bts.hex(bytes(0,0)));
		Assert.assertEquals("000000", Bts.hex(bytes(0,0,0)));
		Assert.assertEquals("00000000", Bts.hex(bytes(0,0,0,0)));
		Assert.assertEquals("01020304", Bts.hex(bytes(1,2,3,4)));
	}
	

	@Test
	public void testDeHex() {
		Assert.assertArrayEquals(bytes(0), Bts.dehex("0"));
		Assert.assertArrayEquals(bytes(0), Bts.dehex("00"));
		Assert.assertArrayEquals(bytes(0,0), Bts.dehex("000"));
		Assert.assertArrayEquals(bytes(0,0), Bts.dehex("0000"));
		Assert.assertArrayEquals(bytes(0,0,0), Bts.dehex("00000"));
		Assert.assertArrayEquals(bytes(0,0,0), Bts.dehex("000000"));
		
		Assert.assertArrayEquals(bytes(0x0c), Bts.dehex("c"));
		Assert.assertArrayEquals(bytes(0xca), Bts.dehex("ca"));
		Assert.assertArrayEquals(bytes(0xc, 0xaf), Bts.dehex("caf"));
		Assert.assertArrayEquals(bytes(0xca, 0xfe), Bts.dehex("cafe"));
	}
	
	@Test
	public void testLongToHex() {
		Assert.assertEquals("0", Bts.hex(0L));
		Assert.assertEquals("ffffffffffffffff", Bts.hex(0xFFFFFFFFFFFFFFFFL));
		Assert.assertEquals("aa55aa55aa55aa55", Bts.hex(0xAA55AA55AA55AA55L));
		Assert.assertEquals("cafe0091cafe", Bts.hex(0xCAFE0091CAFEL));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeHexinvalidChars1() {
		Assert.assertArrayEquals(bytes(0), Bts.dehex("0g"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeHexinvalidChars2() {
		Assert.assertArrayEquals(bytes(0), Bts.dehex("g0"));
	}

	@Test
	public void testHexToLong() {
		Assert.assertEquals(0L, Bts.hex2long("           0    "));
		Assert.assertEquals(0L, Bts.hex2long("0000000000000000"));
		Assert.assertEquals(0xffffffffffffffffL, Bts.hex2long("FFFFFFFFFFFFFFFF"));
		Assert.assertEquals(0xaa55aa55aa55aa55L, Bts.hex2long("AA55AA55AA55AA55"));
		Assert.assertEquals(0x0000cafe0091cafeL, Bts.hex2long("CAFE0091CAFE"));
		Assert.assertEquals(0x00000cafe007cafeL, Bts.hex2long("CAFE007CAFE"));
	}
	
	@Test
	public void testBytesToLong() {
		Assert.assertEquals(0L, Bts.bytes2long(bytes(0,0,0,0,0,0,0,0)));
		
		Assert.assertEquals(0xffffffffffffffffL, Bts.bytes2long(Bts.dehex("FFFFFFFFFFFFFFFF")));
		Assert.assertEquals(0xaa55aa55aa55aa55L, Bts.bytes2long(Bts.dehex("AA55AA55AA55AA55")));
		Assert.assertEquals(0x0000cafe0091cafeL, Bts.bytes2long(Bts.dehex("0000CAFE0091CAFE")));
		Assert.assertEquals(0x00000cafe007cafeL, Bts.bytes2long(Bts.dehex("00000CAFE007CAFE")));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testHexToLong_argument_too_long() {
		Bts.hex2long("00000000000000000000000000000000000000000000000000000000000000");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBytesToLong_argument_too_long() {
		Bts.bytes2long(Bts.dehex("00000000000000000000000000000000000000000000000000000000000000"));
	}
	
	private byte [] bytes(int ... bytes) {
		final byte[] ret = new byte[bytes.length];
		for (int i=0; i<bytes.length; i++) {
			ret[i] = (byte) bytes[i];
		}
		return ret;
	}
}
