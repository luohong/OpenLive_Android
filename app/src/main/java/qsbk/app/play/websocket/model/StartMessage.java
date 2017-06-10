package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class StartMessage extends BaseMessage {

	public StartMessage() {
		super(Constants.MessageType.GAME_START);
	}

}
