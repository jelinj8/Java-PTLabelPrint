package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@link ResponseCommandId#IN_CONNECT} status codes. */
public enum ConnectResult {

	DISCONNECT(0),
	CONNECTED(1),
	CONNECTED_NEW(2),
	CONNECTED_V3(3),
	FIRMWARE_ERRORS(90);

	private static final Map<Integer, ConnectResult> BY_CODE = new HashMap<>();

	static {
		for (ConnectResult r : values()) {
			BY_CODE.put(r.code, r);
		}
	}

	private final int code;

	ConnectResult(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Optional<ConnectResult> fromCode(int code) {
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
