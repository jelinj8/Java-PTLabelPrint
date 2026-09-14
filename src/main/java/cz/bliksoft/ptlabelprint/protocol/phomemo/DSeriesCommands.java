package cz.bliksoft.ptlabelprint.protocol.phomemo;

/**
 * Command bytes for Phomemo's {@code d-series} protocol (D30/D35/D50/D110/Q30/Q30S). Ported from
 * phomymo's {@code D_CMD} and shared {@code CMD.HEAT_SETTINGS} (src/web/printer.js). This protocol
 * is ESC/POS-derived and one-way: none of these commands expect a response.
 */
public final class DSeriesCommands {

	private static final int[] HEAT_TIMES = {40, 60, 80, 100, 120, 140, 160, 200};

	private DSeriesCommands() {
	}

	/** Map density 1-8 to a heat time value (higher = darker). Ported from phomymo's {@code densityToHeatTime}. */
	public static int densityToHeatTime(int density) {
		int idx = Math.max(0, Math.min(7, density - 1));
		return HEAT_TIMES[idx];
	}

	/** {@code ESC 7 maxDots heatTime heatInterval} - common on Chinese thermal printers. */
	public static byte[] heatSettings(int maxDots, int heatTime, int heatInterval) {
		return new byte[] {0x1b, 0x37, (byte) maxDots, (byte) heatTime, (byte) heatInterval};
	}

	/** {@code 1F 11 <0x0A gaps | 0x0B continuous>} - media type / gap detection. */
	public static byte[] mediaType(boolean continuous) {
		return new byte[] {0x1f, 0x11, (byte) (continuous ? 0x0b : 0x0a)};
	}

	/** {@code ESC @} (init) + {@code GS v 0 0} (raster bit image) with width/height, D-series inline variant. */
	public static byte[] header(int widthBytes, int rows) {
		return new byte[] {
				0x1b, 0x40,
				0x1d, 0x76, 0x30, 0x00,
				(byte) (widthBytes % 256), (byte) (widthBytes / 256),
				(byte) (rows % 256), (byte) (rows / 256),
		};
	}

	/** {@code ESC d 0} - print, no feed (gap detection for die-cut). */
	public static final byte[] END = {0x1b, 0x64, 0x00};

	/** {@code ESC J n} - feed n dots (for continuous tape). */
	public static byte[] feed(int dots) {
		return new byte[] {0x1b, 0x4a, (byte) (dots & 0xff)};
	}
}
