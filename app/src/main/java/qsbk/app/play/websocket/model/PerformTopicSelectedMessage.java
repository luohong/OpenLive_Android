package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class PerformTopicSelectedMessage extends BaseRoomMessage {

    public String topic;

    public PerformTopicSelectedMessage(int roomId, String topic) {
        super(Constants.MessageType.PERFORM_TOPIC_SELECTED, roomId);
        this.topic = topic;
    }

}
