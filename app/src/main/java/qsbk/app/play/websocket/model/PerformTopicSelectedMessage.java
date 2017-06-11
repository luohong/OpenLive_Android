package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;
import qsbk.app.play.websocket.model.BaseMessage;

public class PerformTopicSelectedMessage extends BaseMessage {

	public String topic;
	
	public PerformTopicSelectedMessage(String topic) {
		super(Constants.MessageType.PERFORM_TOPIC_SELECTED);
		this.topic = topic;
	}

}
