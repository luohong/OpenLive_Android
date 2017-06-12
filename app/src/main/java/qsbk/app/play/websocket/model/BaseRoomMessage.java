package qsbk.app.play.websocket.model;

public class BaseRoomMessage extends BaseMessage {

	public int roomId;
	
	public BaseRoomMessage(int type, int roomId) {
		super(type);
		this.roomId = roomId;
	}
	
}
