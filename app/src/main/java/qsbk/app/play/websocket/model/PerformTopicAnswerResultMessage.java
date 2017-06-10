package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class PerformTopicAnswerResultMessage extends BaseMessage {

	public boolean result;
	public String answer;
	
	public PerformTopicAnswerResultMessage() {
		super(Constants.MessageType.PERFORM_TOPIC_ANSWER_RESULT);
	}

}
