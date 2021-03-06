package qsbk.app.play.websocket.model;

import java.util.List;

import qsbk.app.play.common.Constants;

public class PerformMessage extends BaseMessage {

	public String who;
	public int count;
	public List<String> topics;

	public PerformMessage() {
		super(Constants.MessageType.PERFORM);
	}

}
