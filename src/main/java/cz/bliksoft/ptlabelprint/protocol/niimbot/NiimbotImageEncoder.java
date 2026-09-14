package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encodes a {@link PixelSource} into per-row packet data. Ported from niimbluelib's
 * {@code ImageEncoder} (src/image_encoder.ts) - the {@code printDirection}-aware rotation in
 * niimbluelib's own {@code CanvasImageSource} is not ported (no canvas here); see
 * {@link PixelSource}'s javadoc.
 */
public final class NiimbotImageEncoder {

	private NiimbotImageEncoder() {
	}

	public static EncodedImage encode(PixelSource source, PageColorType pageColor) {
		List<ImageRow> rowsData = new ArrayList<>();

		int originalCols = source.getWidth();
		int rows = source.getHeight();
		int cols = ((originalCols + 7) / 8) * 8;

		for (int row = 0; row < rows; row++) {
			boolean isVoid = true;
			int blackPixelsCount = 0;
			int redPixelsCount = 0;
			byte[] blackRowData = new byte[cols / 8];
			byte[] redRowData = new byte[cols / 8];

			for (int colOct = 0; colOct < cols / 8; colOct++) {
				int blackOctet = 0;
				int redOctet = 0;

				for (int colBit = 0; colBit < 8; colBit++) {
					int col = colOct * 8 + colBit;
					if (col >= originalCols) {
						continue;
					}

					if (pageColor == PageColorType.DOUBLE_COLOR && source.isRed(col, row)) {
						redOctet |= 1 << (7 - colBit);
						isVoid = false;
						redPixelsCount++;
					} else if (source.isBlack(col, row)) {
						blackOctet |= 1 << (7 - colBit);
						isVoid = false;
						blackPixelsCount++;
					}
				}
				blackRowData[colOct] = (byte) blackOctet;
				redRowData[colOct] = (byte) redOctet;
			}

			ImageRow newRow = new ImageRow(isVoid ? ImageRow.DataType.VOID : ImageRow.DataType.PIXELS, row, 1,
					blackPixelsCount, redPixelsCount, isVoid ? null : blackRowData, isVoid ? null : redRowData);

			if (rowsData.isEmpty()) {
				rowsData.add(newRow);
			} else {
				ImageRow last = rowsData.get(rowsData.size() - 1);
				boolean same = newRow.getDataType() == last.getDataType();

				if (same && newRow.getDataType() == ImageRow.DataType.PIXELS) {
					same = Arrays.equals(newRow.getRowDataBlack(), last.getRowDataBlack())
							&& Arrays.equals(newRow.getRowDataRed(), last.getRowDataRed());
				}

				if (same) {
					last.incrementRepeat();
				} else {
					rowsData.add(newRow);
				}

				// niimbluelib also emits a "check" row every 200 rows when enabled - not ported
				// (CLI test patterns here are far smaller than 200 rows; add if a real image
				// pipeline needs it).
			}
		}

		return new EncodedImage(pageColor, cols, rows, rowsData);
	}

	/**
	 * @param data pixels encoded by {@link #encode} (one byte = 8 pixels, MSB first)
	 * @return indexes of set (black) pixels, each as a big-endian 16-bit index
	 */
	public static byte[] indexPixels(byte[] data) {
		List<Byte> result = new ArrayList<>();

		for (int bytePos = 0; bytePos < data.length; bytePos++) {
			int b = data[bytePos] & 0xff;
			for (int bitPos = 0; bitPos < 8; bitPos++) {
				if ((b & (1 << (7 - bitPos))) != 0) {
					int idx = bytePos * 8 + bitPos;
					result.add((byte) ((idx >> 8) & 0xff));
					result.add((byte) (idx & 0xff));
				}
			}
		}

		byte[] out = new byte[result.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = result.get(i);
		}
		return out;
	}

	public static class PixelCountResult {
		public final int total;
		public final int[] parts;

		PixelCountResult(int total, int[] parts) {
			this.total = total;
			this.parts = parts;
		}
	}

	/**
	 * Counts set ("black") bits in {@code buf}, either as one total (big-endian 16-bit split across
	 * {@code parts[1]}/{@code parts[2]}) or per one-third-of-the-printhead chunk (used by
	 * {@link PacketGenerator#printBitmapRow}) - see niimbluelib's own {@code countPixelsForBitmapPacket}
	 * doc comment for the full split-vs-total rules ported here.
	 */
	public static PixelCountResult countPixelsForBitmapPacket(byte[] buf, int printheadPixels, String mode) {
		int total = 0;
		int[] parts = {0, 0, 0};
		int chunkSize = printheadPixels / 8 / 3;
		boolean split = buf.length <= chunkSize * 3;

		if ("total".equals(mode)) {
			split = false;
		} else if ("split".equals(mode)) {
			split = buf.length <= chunkSize * 3;
		}

		for (int byteN = 0; byteN < buf.length; byteN++) {
			int value = buf[byteN] & 0xff;
			int chunkIdx = chunkSize == 0 ? 0 : byteN / chunkSize;

			for (int bitN = 0; bitN < 8; bitN++) {
				if ((value & (1 << bitN)) != 0) {
					total++;
					if (!split || chunkIdx > 2) {
						continue;
					}
					parts[chunkIdx]++;
				}
			}
		}

		if (split) {
			return new PixelCountResult(total, parts);
		}

		return new PixelCountResult(total, new int[] {0, total & 0xff, (total >> 8) & 0xff});
	}
}
