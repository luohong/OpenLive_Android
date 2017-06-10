package qsbk.app.play.websocket.model;

import qsbk.app.play.common.Constants;

public class MatchProgressMessage extends BaseMessage {

	public int progress;
	public int total;
	
	public MatchProgressMessage() {
		super(Constants.MessageType.MATCH_PROGRESS);
	}

	public MatchProgressMessage(int progress, int total) {
		this();
		this.progress = progress;
		this.total = total;
	}
	
}
