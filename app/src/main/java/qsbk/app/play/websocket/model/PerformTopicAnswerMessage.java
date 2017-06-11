package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class PerformTopicAnswerMessage extends BaseMessage {

	public String answer;

	public PerformTopicAnswerMessage(String answer) {
		super(Constants.MessageType.PERFORM_TOPIC_ANSWER);
		this.answer = answer;
	}

}
