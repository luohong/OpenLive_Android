package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;
import qsbk.app.play.websocket.model.BaseMessage;

public class SelectedPerformTopicMessage extends BaseMessage {

	public String topic;
	
	public SelectedPerformTopicMessage() {
		super(Constants.MessageType.PERFORM_TOPIC_SELECTED);
	}

}
