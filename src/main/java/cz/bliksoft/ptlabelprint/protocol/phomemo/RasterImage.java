package cz.bliksoft.ptlabelprint.protocol.phomemo;

/**
 * A 1bpp raster image: {@code widthBytes} bytes per row (MSB-first, bit=1 means "print/black"),
 * {@code heightLines} rows, top-to-bottom, {@code data.length == widthBytes * heightLines}.
 * Matches the raster shape phomymo's canvas/printer code passes around
 * ({@code { data, widthBytes, heightLines } }).
 */
public class RasterImage {

	private final byte[] data;
	private final int widthBytes;
	private final int heightLines;

	public RasterImage(byte[] data, int widthBytes, int heightLines) {
		if (data.length != widthBytes * heightLines) {
			throw new IllegalArgumentException(
					"data.length (" + data.length + ") must equal widthBytes*heightLines (" + (widthBytes * heightLines) + ")");
		}
		this.data = data;
		this.widthBytes = widthBytes;
		this.heightLines = heightLines;
	}

	public byte[] getData() {
		return data;
	}

	public int getWidthBytes() {
		return widthBytes;
	}

	public int getHeightLines() {
		return heightLines;
	}

	/**
	 * Rotate 90 degrees clockwise. Ported from phomymo's {@code rotateRaster90CW}
	 * (src/web/printer.js) - the D-series protocol prints labels sideways, so the print flow
	 * rotates a normally-oriented image before sending it.
	 */
	public RasterImage rotate90Clockwise() {
		int srcWidthPx = widthBytes * 8;
		int srcHeightPx = heightLines;

		int dstWidthPx = srcHeightPx;
		int dstHeightPx = srcWidthPx;
		int dstWidthBytes = (dstWidthPx + 7) / 8;

		byte[] rotated = new byte[dstWidthBytes * dstHeightPx];

		for (int srcY = 0; srcY < srcHeightPx; srcY++) {
			for (int srcX = 0; srcX < srcWidthPx; srcX++) {
				int srcByteIdx = srcY * widthBytes + (srcX / 8);
				int srcBitIdx = 7 - (srcX % 8);
				int pixel = (data[srcByteIdx] >> srcBitIdx) & 1;

				// 90 deg CW rotation: (x, y) -> (height - 1 - y, x)
				int dstX = srcHeightPx - 1 - srcY;
				int dstY = srcX;

				if (pixel != 0) {
					int dstByteIdx = dstY * dstWidthBytes + (dstX / 8);
					int dstBitIdx = 7 - (dstX % 8);
					rotated[dstByteIdx] |= (byte) (1 << dstBitIdx);
				}
			}
		}

		return new RasterImage(rotated, dstWidthBytes, dstHeightPx);
	}
}
