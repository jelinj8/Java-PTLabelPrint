package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Sent with {@code PrintBitmapRowDoubleColor} (not yet ported); also appears in heartbeat printer info. */
public enum ResolutionClass {

	DPI_203(2),
	DPI_300(3);

	private final int code;

	ResolutionClass(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static ResolutionClass fromCode(int code) {
		for (ResolutionClass c : values()) {
			if (c.code == code) {
				return c;
			}
		}
		return null;
	}
}
