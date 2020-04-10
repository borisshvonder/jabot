package jabot.common;

import org.junit.Test;

import org.junit.Assert;

public class TexterTest {
	private static final Texter fixture = new Texter();
	
	@Test
	public void testAdd() {
		Texter.Builder b = fixture.build();
		b.append("some long long long long string");
		Assert.assertEquals("some long long long long string", b.toString());
		Assert.assertEquals("some long long long long string", b.toText().toString());
		Assert.assertEquals(31, b.length());
	}
	
	@Test
	public void testAddAtPos() {
		Texter.Builder b = fixture.build();
		b.append("str", 1, 2);
		b.append(null, 1, 2);
		b.append("str", 1, 1);
		Assert.assertEquals("tnull", b.toString());
		Assert.assertEquals("tnull", b.toText().toString());
	}
	
	@Test
	public void testAddChar() {
		Texter.Builder b = fixture.build();
		b.append('c');
		Assert.assertEquals("c", b.toString());
		Assert.assertEquals("c", b.toText().toString());
	}
	
	@Test
	public void testAddInt() {
		Texter.Builder b = fixture.build();
		b.append(123);
		b.append(-456);
		Assert.assertEquals("123-456", b.toString());
		Assert.assertEquals("123-456", b.toText().toString());
	}
	
	@Test
	public void testAddLong() {
		Texter.Builder b = fixture.build();
		b.append(123L);
		b.append(-456L);
		Assert.assertEquals("123-456", b.toString());
		Assert.assertEquals("123-456", b.toText().toString());
	}
	
	@Test
	public void testClear() {
		Texter.Builder b = fixture.build();
		b.append("aaa");
		b.clear();
		Assert.assertEquals("", b.toString());
	}
	
	@Test
	public void testDelete() {
		Texter.Builder b = fixture.build();
		b.append("Hello, world!");
		b.delete(1, 5);
		Assert.assertEquals("H, world!", b.toString());
		b.delete(0, 3);
		Assert.assertEquals("world!", b.toString());
		b.delete(5, 6);
		Assert.assertEquals("world", b.toString());
		b.delete(0, 5);
		Assert.assertEquals("", b.toString());
	}
}
