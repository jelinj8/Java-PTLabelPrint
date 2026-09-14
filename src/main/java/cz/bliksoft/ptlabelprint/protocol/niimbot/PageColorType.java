package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Sent with {@link RequestCommandId#PRINT_START}. */
public enum PageColorType {
	SINGLE_COLOR(0),
	DOUBLE_COLOR(1),
	SINGLE_COLOR_ALT(2),
	MULTIORDER_COLOR(3);

	private final int code;

	PageColorType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
