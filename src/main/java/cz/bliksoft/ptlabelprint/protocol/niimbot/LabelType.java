package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Sent with {@link RequestCommandId#SET_LABEL_TYPE}. */
public enum LabelType {

	INVALID(0),
	/** Default for most label printers. */
	WITH_GAPS(1),
	BLACK(2),
	CONTINUOUS(3),
	PERFORATED(4),
	TRANSPARENT(5),
	PVC_TAG(6),
	BLACK_MARK_GAP(10),
	HEAT_SHRINK_TUBE(11);

	private static final Map<Integer, LabelType> BY_CODE = new HashMap<>();

	static {
		for (LabelType t : values()) {
			BY_CODE.put(t.code, t);
		}
	}

	private final int code;

	LabelType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Optional<LabelType> fromCode(int code) {
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
