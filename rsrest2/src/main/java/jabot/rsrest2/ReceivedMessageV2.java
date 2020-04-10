package jabot.rsrest2;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import jabot.rsapi.ReceivedMessage;

public class ReceivedMessageV2 implements ReceivedMessage {
	//"author_id":"55cd4f9d46e3890f9447fcf0f534c37c","author_name":"dimqua","id":"3082740750","incoming":true,"links":[],"msg":"...","recv_time":"1489529772","send_time":"1489529771"
	private String id;

	@JsonProperty("author_id")
	private String authorId;
	
	@JsonProperty("author_name")
	private String authorName;
	
	@JsonProperty("recv_time")
	private long recvUnixTime;
	
	@JsonProperty("send_time")
	private long sendUnixTime;
	
	@JsonProperty("msg")
	private String text;

	private boolean incoming;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getAuthorId() {
		return authorId;
	}

	@Override
	public String getAuthorName() {
		return authorName;
	}

	@Override
	public Date getRecvTime() {
		return new Date(recvUnixTime*1000);
	}

	@Override
	public Date getSendTime() {
		return new Date(sendUnixTime*1000);
	}

	@Override
	public String getText() {
		return text;
	}
	
	@Override
	public boolean isIncoming() {
		return incoming;
	}

	public void setId(String id) {
		this.id = id;
	}


	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public long getRecvUnixTime() {
		return recvUnixTime;
	}

	public void setRecvUnixTime(long recvUnixTime) {
		this.recvUnixTime = recvUnixTime;
	}

	public long getSendUnixTime() {
		return sendUnixTime;
	}

	public void setSendUnixTime(long sendUnixTime) {
		this.sendUnixTime = sendUnixTime;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setIncoming(boolean incoming) {
		this.incoming = incoming;
	}

	@Override
	public String toString() {
		return "["+id+"] "+authorName+": "+text;
	}

}
