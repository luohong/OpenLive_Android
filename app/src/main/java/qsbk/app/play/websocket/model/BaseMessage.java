package qsbk.app.play.websocket.model;

import java.io.Serializable;

public class BaseMessage implements Serializable {
	
	public int type;
	
	public BaseMessage() {
		
	}
	
	public BaseMessage(int type) {
		this.type = type;
	}
	
}
