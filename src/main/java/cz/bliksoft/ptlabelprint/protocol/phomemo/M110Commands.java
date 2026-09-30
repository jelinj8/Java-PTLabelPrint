package cz.bliksoft.ptlabelprint.protocol.phomemo;

/**
 * Command bytes for Phomemo's {@code m110} protocol (M110/M110S/M120, plus the M421 - see
 * {@link M110Printer}). Ported from phomymo's {@code M110_CMD} and shared {@code CMD.RASTER_HEADER}
 * (src/web/printer.js). Like {@code d-series}, the print commands themselves are one-way: none of
 * them expects a response.
 */
public final class M110Commands {

	private M110Commands() {
	}

	/** Map density 1-8 to the protocol's own ~6-15 scale (higher = darker, default 10). Ported from phomymo's {@code printM110}. */
	public static int densityToM110(int density) {
		int clamped = Math.max(1, Math.min(8, density));
		return (int) Math.round(5 + clamped * 1.25);
	}

	/** {@code ESC N 0x0D <speed>} - print speed, 1 (slow) to 5 (fast). */
	public static byte[] speed(int speed) {
		return new byte[] {0x1b, 0x4e, 0x0d, (byte) speed};
	}

	/** {@code ESC N 0x04 <density>} - print density in the protocol's own 1-15 scale, see {@link #densityToM110}. */
	public static byte[] density(int m110Density) {
		return new byte[] {0x1b, 0x4e, 0x04, (byte) m110Density};
	}

	/** {@code 1F 11 <0x0A gaps | 0x0B continuous>} - media type / gap detection. */
	public static byte[] mediaType(boolean continuous) {
		return new byte[] {0x1f, 0x11, (byte) (continuous ? 0x0b : 0x0a)};
	}

	/** {@code GS v 0 0} (raster bit image) with width in bytes and height in lines, both 16-bit little-endian. */
	public static byte[] rasterHeader(int widthBytes, int heightLines) {
		return new byte[] {
				0x1d, 0x76, 0x30, 0x00,
				(byte) (widthBytes % 256), (byte) (widthBytes / 256),
				(byte) (heightLines % 256), (byte) (heightLines / 256),
		};
	}

	/** Finalizes the print (feeds to the next gap on gapped media). */
	public static final byte[] FOOTER = {0x1f, (byte) 0xf0, 0x05, 0x00, 0x1f, (byte) 0xf0, 0x03, 0x00};
}
