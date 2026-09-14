package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Whether a {@link RequestCommandId#PRINTER_CONFIG_2} packet sets or reads the targeted {@link PrinterConfig2Type} value. */
public enum PrinterConfig2Action {

	SET_VALUE(1),
	GET_VALUE(2);

	private final int code;

	PrinterConfig2Action(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
