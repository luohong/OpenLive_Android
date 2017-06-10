package qsbk.app.play.websocket.model;

import java.util.List;

import qsbk.app.play.common.Constants;
import qsbk.app.play.websocket.model.BaseMessage;

public class PerformMessage extends BaseMessage {

	public int who;
	public int count;
	public List<String> topics;

	public PerformMessage() {
		super(Constants.MessageType.PERFORM);
	}

}
