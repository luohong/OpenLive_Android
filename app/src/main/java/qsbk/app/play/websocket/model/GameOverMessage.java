package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class GameOverMessage extends BaseMessage {

	public GameOverMessage() {
		super(Constants.MessageType.GAME_OVER);
	}
	
}
