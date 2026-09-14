package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Sent with {@link RequestCommandId#HEARTBEAT}. */
public enum HeartbeatType {

	ADVANCED_1(1),
	BASIC(2),
	PRINTER_INFO(3),
	ADVANCED_2(4);

	private final int code;

	HeartbeatType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
