package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Sent with {@link RequestCommandId#PRINTER_INFO}. */
public enum PrinterInfoType {

	DENSITY(1),
	SPEED(2),
	LABEL_TYPE(3),
	LANGUAGE(6),
	AUTO_SHUTDOWN_TIME(7),
	/** See {@link PrinterModels}. */
	PRINTER_MODEL_ID(8),
	SOFTWARE_VERSION(9),
	BATTERY_CHARGE_LEVEL(10),
	SERIAL_NUMBER(11),
	HARDWARE_VERSION(12),
	BLUETOOTH_ADDRESS(13),
	PRINT_MODE(14),
	AREA(15);

	private final int code;

	PrinterInfoType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
