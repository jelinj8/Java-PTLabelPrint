package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RasterImageTest {

	@Test
	void constructorRejectsMismatchedDataLength() {
		assertThrows(IllegalArgumentException.class, () -> new RasterImage(new byte[3], 2, 2));
	}

	@Test
	void rotatesSinglePixelToExpectedPosition() {
		// 8px wide (1 byte), 1 row tall; only the leftmost pixel (bit 7) is set.
		RasterImage image = new RasterImage(new byte[] {(byte) 0x80}, 1, 1);

		RasterImage rotated = image.rotate90Clockwise();

		// After 90 deg CW: new width = old height = 1px (1 byte), new height = old width = 8 rows.
		assertEquals(1, rotated.getWidthBytes());
		assertEquals(8, rotated.getHeightLines());

		byte[] expected = new byte[8];
		expected[0] = (byte) 0x80; // (dstX=0,dstY=0) per the (x,y) -> (h-1-y, x) mapping for srcX=0,srcY=0
		assertArrayEquals(expected, rotated.getData());
	}

	@Test
	void rotatingTwiceMoreRotatesAnAsymmetricPattern() {
		// 8px wide, 2 rows: row0 = 0xF0 (left half set), row1 = 0x00
		RasterImage image = new RasterImage(new byte[] {(byte) 0xf0, 0x00}, 1, 2);

		RasterImage rotated = image.rotate90Clockwise();

		// new width = old height = 2px -> 1 byte; new height = old width = 8 rows
		assertEquals(1, rotated.getWidthBytes());
		assertEquals(8, rotated.getHeightLines());
		assertEquals(8, rotated.getData().length);
	}
}
