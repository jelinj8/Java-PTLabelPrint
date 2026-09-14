package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum AutoShutdownTime {

	/** Usually 15 minutes. */
	SHUTDOWN_TIME_1(1),
	/** Usually 30 minutes. */
	SHUTDOWN_TIME_2(2),
	/** May be 45 or 60 minutes (depending on model). */
	SHUTDOWN_TIME_3(3),
	/** May be 60 minutes or never (depending on model). */
	SHUTDOWN_TIME_4(4);

	private static final Map<Integer, AutoShutdownTime> BY_CODE = new HashMap<>();

	static {
		for (AutoShutdownTime t : values()) {
			BY_CODE.put(t.code, t);
		}
	}

	private final int code;

	AutoShutdownTime(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Optional<AutoShutdownTime> fromCode(int code) {
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
