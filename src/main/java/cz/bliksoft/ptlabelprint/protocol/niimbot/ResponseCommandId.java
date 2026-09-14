package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Command IDs sent from printer to client. Ported from niimbluelib's {@code ResponseCommandId}
 * (src/packets/commands.ts) for full reference, even though {@link PacketParser} only wires a
 * subset of these into typed parse methods so far - see that class for what is actually
 * implemented.
 */
public enum ResponseCommandId {

	NOT_SUPPORTED(0x00),
	IN_CONNECT(0xc2),
	IN_CALIBRATE_HEIGHT(0x69),
	IN_CANCEL_PRINT(0xd0),
	IN_ANTI_FAKE(0x0c),
	IN_HEARTBEAT_ADVANCED_1(0xdd),
	IN_HEARTBEAT_BASIC(0xde),
	IN_HEARTBEAT_PRINTER_INFO(0xdf),
	IN_HEARTBEAT_ADVANCED_2(0xd9),
	IN_LABEL_POSITIONING_CALIBRATION(0x8f),
	IN_PAGE_START(0x04),
	IN_PRINT_CLEAR(0x30),
	/** Sent by some printers after {@link RequestCommandId#PAGE_END} along with {@link #IN_PAGE_END}. */
	IN_PRINTER_CHECK_LINE(0xd3),
	IN_PRINT_END(0xf4),
	IN_PRINTER_CONFIG(0xbf),
	IN_PRINTER_LOG(0x06),
	IN_PRINTER_INFO_AUTO_SHUTDOWN_TIME(0x47),
	IN_PRINTER_INFO_BLUETOOTH_ADDRESS(0x4d),
	IN_PRINTER_INFO_SPEED(0x42),
	IN_PRINTER_INFO_DENSITY(0x41),
	IN_PRINTER_INFO_LANGUAGE(0x46),
	IN_PRINTER_INFO_CHARGE_LEVEL(0x4a),
	IN_PRINTER_INFO_HARDWARE_VERSION(0x4c),
	IN_PRINTER_INFO_LABEL_TYPE(0x43),
	IN_PRINTER_INFO_PRINTER_CODE(0x48),
	IN_PRINTER_INFO_SERIAL_NUMBER(0x4b),
	IN_PRINTER_INFO_SOFTWARE_VERSION(0x49),
	IN_PRINTER_INFO_PRINT_MODE(0x4e),
	IN_PRINTER_INFO_AREA(0x4f),
	IN_PRINTER_STATUS_DATA(0xb5),
	IN_PRINTER_RESET(0x38),
	IN_PRINT_STATUS(0xb3),
	/** For example, received after {@link RequestCommandId#SET_PAGE_SIZE} when page print is not started. */
	IN_PRINT_ERROR(0xdb),
	IN_PRINT_QUANTITY(0x16),
	IN_PRINT_START(0x02),
	IN_RFID_INFO(0x1b),
	IN_RFID_INFO_2(0x1d),
	IN_RFID_SUCCESS_TIMES(0x64),
	IN_SET_AUTO_SHUTDOWN_TIME(0x37),
	IN_SET_DENSITY(0x31),
	IN_SET_PRINT_SPEED(0x32),
	IN_SET_LABEL_TYPE(0x33),
	IN_SET_LANGUAGE_TYPE(0x36),
	IN_SET_VOLUME_LEVEL(0x3e),
	IN_SET_ANTI_SETTER(0x3f),
	IN_SET_PAGE_SIZE(0x14),
	IN_PRINT_MARGIN_TOP(0x1f),
	IN_SET_REVERSE_PRINTER_FEED(0x1e),
	IN_SOUND_SETTINGS(0x68),
	IN_PAGE_END(0xe4),
	IN_PRINTER_PAGE_INDEX(0xe0),
	IN_PRINT_TEST_PAGE(0x6a),
	IN_GET_VOLUME_LEVEL(0x71),
	IN_START_FIRMWARE_UPGRADE(0xf6),
	IN_REQUEST_FIRMWARE_CRC(0x90),
	IN_REQUEST_FIRMWARE_CHUNK(0x9a),
	IN_FIRMWARE_CHECK_RESULT(0x9d),
	IN_FIRMWARE_RESULT(0x9e),
	/** Sent before {@link #IN_PRINTER_CHECK_LINE}. */
	IN_RESET_TIMEOUT(0xc6),
	IN_SET_CURRENT_TIME_FORMAT(0x11),
	IN_GET_CURRENT_TIME_FORMAT_OR_TEMPLATE_INFO(0x12),
	IN_PRINTER_CONFIG_2(0x08),
	IN_GET_KEY_FUNCTION(0x0a),
	IN_GET_PRINT_QUALITY(0x0d),
	IN_GET_PRINTER_CONFIGURATION_WIFI(0xb2),
	IN_TUBE_TYPE_AND_WIDTH(0x0f),
	IN_HALF_CUT(0x6c),
	IN_GET_PRINTER_USAGE(0x19),
	IN_GET_TEMPLATE_HISTORY(0x17),
	IN_GET_COMPRESS(0x88),
	IN_GET_PRINTER_FREE(0xc4),
	IN_SET_LABEL_MATERIAL(0x3d),
	IN_TUBE_SETTINGS(0x0e),
	IN_PAUSE(0xb6);

	private static final Map<Integer, ResponseCommandId> BY_CODE = new HashMap<>();

	static {
		for (ResponseCommandId id : values()) {
			BY_CODE.put(id.code, id);
		}
	}

	private final int code;

	ResponseCommandId(int code) {
		this.code = code;
	}

	/** Raw wire value (0-255). */
	public int getCode() {
		return code;
	}

	/** Looks up a response command by its raw wire value, e.g. a just-parsed {@link NiimbotPacket#getCommand()}. */
	public static Optional<ResponseCommandId> fromCode(int code) {
		return Optional.ofNullable(BY_CODE.get(code));
	}
}
