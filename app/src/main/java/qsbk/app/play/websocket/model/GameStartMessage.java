package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class GameStartMessage extends BaseMessage {

	public int roomId;

	public GameStartMessage(int roomId) {
		super(Constants.MessageType.GAME_START);
		this.roomId = roomId;
	}

}
