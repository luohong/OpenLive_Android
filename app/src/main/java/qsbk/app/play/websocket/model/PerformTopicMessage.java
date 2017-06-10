package qsbk.app.play.websocket.model;

import java.util.List;

import qsbk.app.play.common.Constants;
import qsbk.app.play.websocket.model.BaseMessage;

public class PerformTopicMessage extends BaseMessage {

	public int wordCount;
	public List<String> words;
	
	public PerformTopicMessage() {
		super(Constants.MessageType.PERFORM_TOPIC);
	}

}
