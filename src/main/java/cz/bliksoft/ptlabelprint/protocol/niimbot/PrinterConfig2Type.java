package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Which setting a {@link RequestCommandId#PRINTER_CONFIG_2} packet targets. */
public enum PrinterConfig2Type {

	TIME(8);

	private final int code;

	PrinterConfig2Type(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
