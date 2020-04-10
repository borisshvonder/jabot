package jabot.jabot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.rsapi.ReceivedMessage;

public class StreamLobbyTest {
	private PrintStream in;
	private BufferedReader out;
	private StreamLobby fixture;
	
	@Before
	public void setUp() throws IOException {
		final PipedInputStream inStream = new PipedInputStream();
		final PipedOutputStream outStream = new PipedOutputStream();
		
		fixture = new StreamLobby(new InputStreamReader(inStream, StandardCharsets.UTF_8), new PrintStream(outStream, true, StandardCharsets.UTF_8.name()));
		
		final PipedOutputStream inSource = new PipedOutputStream();
		inStream.connect(inSource);
		in = new PrintStream(inSource, true, StandardCharsets.UTF_8.name());
		
		final PipedInputStream outSink = new PipedInputStream();
		outStream.connect(outSink);
		out = new BufferedReader(new InputStreamReader(outSink, StandardCharsets.UTF_8));
	}
	
	@Test
	public void testDefaultValues() {
		Assert.assertNotNull(fixture.getChatId());
		Assert.assertNull(fixture.getGxsId());
		Assert.assertNotNull(fixture.getId());
		Assert.assertNotNull(fixture.getName());
		Assert.assertNotNull(fixture.getUserId());
		Assert.assertNotNull(fixture.getSystemId());
		
		Assert.assertNotEquals(fixture.getUserId(), fixture.getSystemId());
	}
	
	@Test
	public void testGettersSetters() {
		fixture.setChatId("setChatId1");
		Assert.assertEquals("setChatId1", fixture.getChatId());
		
		fixture.setId("setId1");
		Assert.assertEquals("setId1", fixture.getId());
		
		fixture.setName("setName1");
		Assert.assertEquals("setName1", fixture.getName());
		
		fixture.setUserId("setInUser1");
		Assert.assertEquals("setInUser1", fixture.getUserId());
		
		fixture.setSystemId("system1");
		Assert.assertEquals("system1", fixture.getSystemId());
	}
	
	@Test
	public void testReadMessages_empty() throws IOException {
		Assert.assertEquals(0, fixture.readMessages().size());
	}
	
	@Test
	public void testReadMessages_oneMessage() throws IOException {
		in.println("message1");
		Assert.assertEquals(1, fixture.readMessages().size());
	}
	
	@Test
	public void testReadMessages_oneMessage_oneIncomplete() throws IOException {
		in.print("message1\nincomplete");
		Assert.assertEquals(1, fixture.readMessages().size());
	}
	
	@Test
	public void testReadMessages_twpMessages_oneIncomplete() throws IOException {
		in.print("message1\nmessage2\nincomplete");
		Assert.assertEquals(2, fixture.readMessages().size());
	}

	@Test
	public void testReadMessages_messageValidity() throws IOException {
		in.println("message1");
		in.println("message2");
		
		final List<ReceivedMessage> messages = fixture.readMessages();
		Assert.assertEquals(2, messages.size());
		
		final ReceivedMessage m1 = messages.get(0);
		Assert.assertEquals("message1", m1.getText());
		Assert.assertEquals(fixture.getSystemId(), m1.getAuthorId());
		Assert.assertEquals(fixture.getSystemId(), m1.getAuthorName());
		Assert.assertNotNull(m1.getId());
		Assert.assertNotNull(m1.getRecvTime());
		Assert.assertNotNull(m1.getSendTime());
		Assert.assertTrue(m1.isIncoming());

		final ReceivedMessage m2 = messages.get(1);
		Assert.assertEquals("message2", m2.getText());
		
		Assert.assertNotEquals(m1.getId(), m2.getId());
	}
	
	@Test
	public void testClearMessages_none() throws IOException {
		fixture.clearMessages();
	}
	
	@Test
	public void testClearMessages_one() throws IOException {
		in.println("message1");
		fixture.clearMessages();
		Assert.assertEquals(0, fixture.readMessages().size());
	}

	@Test
	public void testPost() throws IOException {
		fixture.setSystemId("SYSTEM");
		fixture.post("message1");
		
		Assert.assertTrue(out.readLine().indexOf("message1")>=0);
	}
	
	@Test
	public void testPostedMessagesAreNotRead() throws IOException {
		fixture.post("message1");
		
		Assert.assertEquals(0, fixture.readMessages().size());
	}
}
