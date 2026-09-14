package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Command IDs sent from client to printer. Ported from niimbluelib's {@code RequestCommandId}
 * (src/packets/commands.ts) for full reference, even though {@link PacketGenerator} only wires a
 * subset of these into typed methods so far - see that class for what is actually implemented.
 */
public enum RequestCommandId {

	/** Entire packet should be prefixed with 0x03 - see {@link NiimbotPacket#toBytes()}. */
	CONNECT(0xc1),
	CANCEL_PRINT(0xda),
	GET_PAPER_INFO(0x59),
	HEARTBEAT(0xdc),
	LABEL_POSITIONING_CALIBRATION(0x8e),
	PAGE_END(0xe3),
	PRINTER_LOG(0x05),
	PAGE_START(0x03),
	PRINT_BITMAP_ROW(0x85),
	/** Sent if black pixels &lt; 6. */
	PRINT_BITMAP_ROW_INDEXED(0x83),
	PRINT_CLEAR(0x20),
	PRINT_EMPTY_ROW(0x84),
	PRINT_END(0xf3),
	PRINTER_INFO(0x40),
	PRINTER_CONFIG(0xaf),
	PRINTER_STATUS_DATA(0xa5),
	PRINTER_RESET(0x28),
	PRINT_QUANTITY(0x15),
	PRINT_START(0x01),
	PRINT_STATUS(0xa3),
	RFID_INFO(0x1a),
	RFID_INFO_2(0x1c),
	RFID_SUCCESS_TIMES(0x54),
	SET_AUTO_SHUTDOWN_TIME(0x27),
	SET_DENSITY(0x21),
	SET_PRINT_SPEED(0x22),
	SET_LABEL_TYPE(0x23),
	SET_LANGUAGE_TYPE(0x26),
	SET_VOLUME_LEVEL(0x2e),
	SET_ANTI_SETTER(0x2f),
	/** 2, 4 or 6 bytes. */
	SET_PAGE_SIZE(0x13),
	SET_REVERSE_PRINTER_FEED_OR_MARGIN_TOP(0x1e),
	SOUND_SETTINGS(0x58),
	/** Some info request (niimbot app), 01 long 02 short. */
	ANTI_FAKE(0x0b),
	GET_VOLUME_LEVEL(0x70),
	PRINT_TEST_PAGE(0x5a),
	START_FIRMWARE_UPGRADE(0xf5),
	FIRMWARE_CRC(0x91),
	FIRMWARE_COMMIT(0x92),
	FIRMWARE_CHUNK(0x9b),
	FIRMWARE_NO_MORE_CHUNKS(0x9c),
	PRINTER_CHECK_LINE(0x86),
	SET_CURRENT_TIME_FORMAT(0x11),
	GET_CURRENT_TIME_FORMAT_OR_TEMPLATE_INFO(0x12),
	PRINTER_CONFIG_2(0x07),
	GET_KEY_FUNCTION(0x09),
	GET_PRINT_QUALITY(0x0d),
	GET_PRINTER_CONFIGURATION_WIFI(0xa2),
	TUBE_TYPE_AND_WIDTH(0x0f),
	HALF_CUT(0x5c),
	GET_PRINTER_USAGE(0x19),
	GET_TEMPLATE_HISTORY(0x17),
	GET_COMPRESS(0x89),
	GET_PRINTER_FREE(0xc3),
	PRINT_BITMAP_ROW_DOUBLE_COLOR(0x8a),
	SET_LABEL_MATERIAL(0x2d),
	TUBE_SETTINGS(0x0e),
	PAUSE(0xa6);

	private final int code;

	RequestCommandId(int code) {
		this.code = code;
	}

	/** Raw wire value (0-255). */
	public int getCode() {
		return code;
	}
}
