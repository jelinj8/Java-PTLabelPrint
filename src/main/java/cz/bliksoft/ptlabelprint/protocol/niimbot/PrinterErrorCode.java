package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@link ResponseCommandId#IN_PRINT_ERROR} status codes. */
public enum PrinterErrorCode {

	COVER_OPEN(0x01),
	/** No paper. */
	LACK_PAPER(0x02),
	LOW_BATTERY(0x03),
	BATTERY_EXCEPTION(0x04),
	USER_CANCEL(0x05),
	DATA_ERROR(0x06),
	OVERHEAT(0x07),
	PAPER_OUT_EXCEPTION(0x08),
	PRINTER_BUSY(0x09),
	NO_PRINTER_HEAD(0x0a),
	TEMPERATURE_LOW(0x0b),
	PRINTER_HEAD_LOOSE(0x0c),
	NO_RIBBON(0x0d),
	WRONG_RIBBON(0x0e),
	USED_RIBBON(0x0f),
	WRONG_PAPER(0x10),
	SET_PAPER_FAIL(0x11),
	SET_PRINT_MODE_FAIL(0x12),
	SET_PRINT_DENSITY_FAIL(0x13),
	WRITE_RFID_FAIL(0x14),
	SET_MARGIN_FAIL(0x15),
	COMMUNICATION_EXCEPTION(0x16),
	DISCONNECT(0x17),
	CANVAS_PARAMETER_ERROR(0x18),
	ROTATION_PARAMETER_EXCEPTION(0x19),
	JSON_PARAMETER_EXCEPTION(0x1a),
	B3S_ABNORMAL_PAPER_OUTPUT(0x1b),
	E_CHECK_PAPER(0x1c),
	RFID_TAG_NOT_WRITTEN(0x1d),
	SET_PRINT_DENSITY_NO_SUPPORT(0x1e),
	SET_PRINT_MODE_NO_SUPPORT(0x1f),
	SET_PRINT_LABEL_MATERIAL_ERROR(0x20),
	SET_PRINT_LABEL_MATERIAL_NO_SUPPORT(0x21),
	NOT_SUPPORT_WRITTEN_RFID(0x22),
	ILLEGAL_PAGE(0x32),
	ILLEGAL_RIBBON_PAGE(0x33),
	RECEIVE_DATA_TIMEOUT(0x34),
	NON_DEDICATED_RIBBON(0x35);

	private static final Map<Integer, PrinterErrorCode> BY_CODE = new HashMap<>();

	static {
		for (PrinterErrorCode c : values()) {
			BY_CODE.put(c.code, c);
		}
	}

	private final int code;

	PrinterErrorCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Optional<PrinterErrorCode> fromCode(int code) {
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
