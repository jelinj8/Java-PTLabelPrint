package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds request packets. Ported from niimbluelib's {@code PacketGenerator}
 * (src/packets/packet_generator.ts). Covers connect/info/heartbeat/misc config plus the two print
 * flows this project has ported so far - the {@code D110M_V4} print task's commands
 * ({@code printStart9b}/{@code setPageSize13b}/bitmap rows/{@code pageEnd}/{@code printEnd}; see
 * {@link D110V4PrintTask}) and the {@code B1} print task's commands ({@code printStart7b}/
 * {@code setPageSize6b}; see {@link B1PrintTask}). Other print tasks' packet variants
 * ({@code printStart1b/2b}, {@code setPageSize2b/4b/9b}, firmware upgrade) are not ported - add
 * them if/when a model needing a different print task shows up.
 */
public class PacketGenerator {

	/**
	 * Maps a request command ID to its corresponding response IDs and creates a packet object.
	 * Ported from niimbluelib's {@code commandsMap} - only entries this class' own methods need are
	 * present (a partial port, not the full ~50-command catalog).
	 */
	private static final Map<RequestCommandId, List<ResponseCommandId>> COMMANDS_MAP = new EnumMap<>(RequestCommandId.class);

	static {
		COMMANDS_MAP.put(RequestCommandId.CONNECT, Collections.singletonList(ResponseCommandId.IN_CONNECT));
		COMMANDS_MAP.put(RequestCommandId.PRINTER_STATUS_DATA, Collections.singletonList(ResponseCommandId.IN_PRINTER_STATUS_DATA));
		COMMANDS_MAP.put(RequestCommandId.RFID_INFO, Collections.singletonList(ResponseCommandId.IN_RFID_INFO));
		COMMANDS_MAP.put(RequestCommandId.RFID_INFO_2, Collections.singletonList(ResponseCommandId.IN_RFID_INFO_2));
		COMMANDS_MAP.put(RequestCommandId.HEARTBEAT, Arrays.asList(ResponseCommandId.IN_HEARTBEAT_BASIC,
				ResponseCommandId.IN_HEARTBEAT_PRINTER_INFO, ResponseCommandId.IN_HEARTBEAT_ADVANCED_1,
				ResponseCommandId.IN_HEARTBEAT_ADVANCED_2));
		COMMANDS_MAP.put(RequestCommandId.PRINTER_INFO, Arrays.asList(ResponseCommandId.IN_PRINTER_INFO_AREA,
				ResponseCommandId.IN_PRINTER_INFO_AUTO_SHUTDOWN_TIME, ResponseCommandId.IN_PRINTER_INFO_BLUETOOTH_ADDRESS,
				ResponseCommandId.IN_PRINTER_INFO_CHARGE_LEVEL, ResponseCommandId.IN_PRINTER_INFO_DENSITY,
				ResponseCommandId.IN_PRINTER_INFO_HARDWARE_VERSION, ResponseCommandId.IN_PRINTER_INFO_LABEL_TYPE,
				ResponseCommandId.IN_PRINTER_INFO_LANGUAGE, ResponseCommandId.IN_PRINTER_INFO_PRINTER_CODE,
				ResponseCommandId.IN_PRINTER_INFO_SERIAL_NUMBER, ResponseCommandId.IN_PRINTER_INFO_SOFTWARE_VERSION,
				ResponseCommandId.IN_PRINTER_INFO_SPEED, ResponseCommandId.IN_PRINTER_INFO_PRINT_MODE));
		COMMANDS_MAP.put(RequestCommandId.PRINTER_RESET, Collections.singletonList(ResponseCommandId.IN_PRINTER_RESET));
		COMMANDS_MAP.put(RequestCommandId.PRINT_STATUS, Collections.singletonList(ResponseCommandId.IN_PRINT_STATUS));
		COMMANDS_MAP.put(RequestCommandId.SET_AUTO_SHUTDOWN_TIME, Collections.singletonList(ResponseCommandId.IN_SET_AUTO_SHUTDOWN_TIME));
		COMMANDS_MAP.put(RequestCommandId.SET_DENSITY, Collections.singletonList(ResponseCommandId.IN_SET_DENSITY));
		COMMANDS_MAP.put(RequestCommandId.SET_LABEL_TYPE, Collections.singletonList(ResponseCommandId.IN_SET_LABEL_TYPE));
		COMMANDS_MAP.put(RequestCommandId.PAGE_START, Collections.singletonList(ResponseCommandId.IN_PAGE_START));
		COMMANDS_MAP.put(RequestCommandId.PAGE_END, Collections.singletonList(ResponseCommandId.IN_PAGE_END));
		COMMANDS_MAP.put(RequestCommandId.PRINT_START, Collections.singletonList(ResponseCommandId.IN_PRINT_START));
		COMMANDS_MAP.put(RequestCommandId.PRINT_END, Collections.singletonList(ResponseCommandId.IN_PRINT_END));
		COMMANDS_MAP.put(RequestCommandId.SET_PAGE_SIZE, Collections.singletonList(ResponseCommandId.IN_SET_PAGE_SIZE));
	}

	private PacketGenerator() {
	}

	/**
	 * Looks up {@code sendCmd}'s expected response IDs (see {@link #COMMANDS_MAP}) and builds a
	 * packet, marking it one-way if none are expected.
	 *
	 * @throws IllegalArgumentException if {@code sendCmd} has no entry in {@link #COMMANDS_MAP} -
	 *         meaning this class doesn't implement that command's response handling yet.
	 */
	public static NiimbotPacket mapped(RequestCommandId sendCmd, byte[] data) {
		List<ResponseCommandId> respIds = COMMANDS_MAP.get(sendCmd);

		if (respIds == null) {
			throw new IllegalArgumentException("No response mapping for " + sendCmd + " - not implemented yet");
		}

		NiimbotPacket p = new NiimbotPacket(sendCmd, data);
		p.setValidResponseIds(respIds);
		return p;
	}

	public static NiimbotPacket connect() {
		return mapped(RequestCommandId.CONNECT, new byte[] {1});
	}

	public static NiimbotPacket getPrinterStatusData() {
		return mapped(RequestCommandId.PRINTER_STATUS_DATA, new byte[] {1});
	}

	public static NiimbotPacket rfidInfo() {
		return mapped(RequestCommandId.RFID_INFO, new byte[] {1});
	}

	public static NiimbotPacket rfidInfo2() {
		return mapped(RequestCommandId.RFID_INFO_2, new byte[] {1});
	}

	public static NiimbotPacket setAutoShutDownTime(AutoShutdownTime time) {
		return mapped(RequestCommandId.SET_AUTO_SHUTDOWN_TIME, new byte[] {(byte) time.getCode()});
	}

	public static NiimbotPacket getPrinterInfo(PrinterInfoType type) {
		return mapped(RequestCommandId.PRINTER_INFO, new byte[] {(byte) type.getCode()});
	}

	public static NiimbotPacket heartbeat(HeartbeatType type) {
		return mapped(RequestCommandId.HEARTBEAT, new byte[] {(byte) type.getCode()});
	}

	public static NiimbotPacket setDensity(int value) {
		return mapped(RequestCommandId.SET_DENSITY, new byte[] {(byte) value});
	}

	public static NiimbotPacket setLabelType(int value) {
		return mapped(RequestCommandId.SET_LABEL_TYPE, new byte[] {(byte) value});
	}

	public static NiimbotPacket printStatus() {
		return mapped(RequestCommandId.PRINT_STATUS, new byte[] {1});
	}

	/** Reset printer settings (sound and maybe some other settings). */
	public static NiimbotPacket printerReset() {
		return mapped(RequestCommandId.PRINTER_RESET, new byte[] {1});
	}

	public static NiimbotPacket pageStart() {
		return mapped(RequestCommandId.PAGE_START, new byte[] {1});
	}

	public static NiimbotPacket pageEnd() {
		return mapped(RequestCommandId.PAGE_END, new byte[] {1});
	}

	public static NiimbotPacket printEnd() {
		return mapped(RequestCommandId.PRINT_END, new byte[] {1});
	}

	/**
	 * Used by {@link B1PrintTask} (B1, D110_M, B21_C2B, M2_H, N1, D101 in niimbluelib's own
	 * model-dispatch table - notably including the M2_H, the "D110M v4" {@link #printStart9b} does
	 * <b>not</b> cover despite the superficially similar naming).
	 */
	public static NiimbotPacket printStart7b(int totalPages, PageColorType pageColor) {
		byte[] pages = u16(totalPages);
		return mapped(RequestCommandId.PRINT_START,
				new byte[] {pages[0], pages[1], 0x00, 0x00, 0x00, 0x00, (byte) pageColor.getCode()});
	}

	/** First seen on D110M v4 - used by {@link D110V4PrintTask}. */
	public static NiimbotPacket printStart9b(int totalPages, PageColorType pageColor, int speed, boolean someFlag) {
		byte[] pages = u16(totalPages);
		return mapped(RequestCommandId.PRINT_START, new byte[] {
				pages[0], pages[1], 0x00, 0x00, 0x00, 0x00, (byte) pageColor.getCode(), (byte) speed,
				(byte) (someFlag ? 1 : 0),
		});
	}

	/** Used by {@link B1PrintTask}. */
	public static NiimbotPacket setPageSize6b(int rows, int cols, int copiesCount) {
		byte[] rowsB = u16(rows);
		byte[] colsB = u16(cols);
		byte[] copiesB = u16(copiesCount);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(rowsB, 0, 2);
		out.write(colsB, 0, 2);
		out.write(copiesB, 0, 2);

		return mapped(RequestCommandId.SET_PAGE_SIZE, out.toByteArray());
	}

	/**
	 * First seen on D110M v4 - used by {@link D110V4PrintTask}.
	 *
	 * @param serial "local template data id", exactly 32 bytes if provided, otherwise omitted
	 */
	public static NiimbotPacket setPageSize13b(int rows, int cols, int copiesCount, int cutHeight, int cutType,
			int sendAll, int partHeight, byte[] serial) {
		if (serial != null && serial.length != 0 && serial.length != 32) {
			throw new IllegalArgumentException("serial must be exactly 32 bytes if provided");
		}

		byte[] rowsB = u16(rows);
		byte[] colsB = u16(cols);
		byte[] copiesB = u16(copiesCount);
		byte[] cutHeightB = u16(cutHeight);
		byte[] partHeightB = u16(partHeight);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(rowsB, 0, 2);
		out.write(colsB, 0, 2);
		out.write(copiesB, 0, 2);
		out.write(cutHeightB, 0, 2);
		out.write(cutType);
		out.write(0x00);
		out.write(sendAll);
		out.write(partHeightB, 0, 2);
		if (serial != null) {
			out.write(serial, 0, serial.length);
		}

		return mapped(RequestCommandId.SET_PAGE_SIZE, out.toByteArray());
	}

	/** Not sent through {@link #mapped}: {@code PrintBitmapRow} never gets a response (one-way), per niimbluelib's own {@code commandsMap}. */
	public static NiimbotPacket printBitmapRow(int pos, int repeats, byte[] data, int printheadPixels, String countsMode) {
		NiimbotImageEncoder.PixelCountResult counts = NiimbotImageEncoder.countPixelsForBitmapPacket(data, printheadPixels, countsMode);
		byte[] posB = u16(pos);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(posB, 0, 2);
		out.write(counts.parts[0]);
		out.write(counts.parts[1]);
		out.write(counts.parts[2]);
		out.write(repeats);
		out.write(data, 0, data.length);

		NiimbotPacket p = new NiimbotPacket(RequestCommandId.PRINT_BITMAP_ROW, out.toByteArray());
		p.setOneWay(true);
		return p;
	}

	/** Printer powers off if black pixel count &gt; 6 - only call when {@code data}'s set-bit count is &lt;= 6. One-way, see {@link #printBitmapRow}. */
	public static NiimbotPacket printBitmapRowIndexed(int pos, int repeats, byte[] data, int printheadPixels, String countsMode) {
		NiimbotImageEncoder.PixelCountResult counts = NiimbotImageEncoder.countPixelsForBitmapPacket(data, printheadPixels, countsMode);

		if (counts.total > 6) {
			throw new IllegalArgumentException("Black pixel count > 6 (" + counts.total + ")");
		}

		byte[] indexes = NiimbotImageEncoder.indexPixels(data);
		byte[] posB = u16(pos);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(posB, 0, 2);
		out.write(counts.parts[0]);
		out.write(counts.parts[1]);
		out.write(counts.parts[2]);
		out.write(repeats);
		out.write(indexes, 0, indexes.length);

		NiimbotPacket p = new NiimbotPacket(RequestCommandId.PRINT_BITMAP_ROW_INDEXED, out.toByteArray());
		p.setOneWay(true);
		return p;
	}

	/** One-way, see {@link #printBitmapRow}. */
	public static NiimbotPacket printEmptySpace(int pos, int repeats) {
		byte[] posB = u16(pos);
		NiimbotPacket p = new NiimbotPacket(RequestCommandId.PRINT_EMPTY_ROW, new byte[] {posB[0], posB[1], (byte) repeats});
		p.setOneWay(true);
		return p;
	}

	/**
	 * Builds the one-way bitmap-row/empty-row packet stream for a single-color {@link EncodedImage}.
	 * Ported from niimbluelib's {@code writeImageDataSingleColor} - the "check line" packet
	 * (sent periodically on very tall images) isn't ported, matching {@link NiimbotImageEncoder}'s
	 * own note.
	 */
	public static List<NiimbotPacket> writeImageDataSingleColor(EncodedImage image, int printheadPixels) {
		List<NiimbotPacket> out = new ArrayList<>();

		for (ImageRow row : image.getRowsData()) {
			if (row.getDataType() == ImageRow.DataType.PIXELS) {
				if (row.getBlackPixelsCount() <= 6) {
					out.add(printBitmapRowIndexed(row.getRowNumber(), row.getRepeat(), row.getRowDataBlack(), printheadPixels, "auto"));
				} else {
					out.add(printBitmapRow(row.getRowNumber(), row.getRepeat(), row.getRowDataBlack(), printheadPixels, "auto"));
				}
			} else if (row.getDataType() == ImageRow.DataType.VOID) {
				out.add(printEmptySpace(row.getRowNumber(), row.getRepeat()));
			}
			// CHECK rows: not ported, see javadoc.
		}

		return out;
	}

	private static byte[] u16(int n) {
		return new byte[] {(byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
	}
}
