package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class PerformTopicAnswerMessage extends BaseRoomMessage {

    public String answer;

    public PerformTopicAnswerMessage(int roomId, String answer) {
        super(Constants.MessageType.PERFORM_TOPIC_ANSWER, roomId);
        this.answer = answer;
    }

}
