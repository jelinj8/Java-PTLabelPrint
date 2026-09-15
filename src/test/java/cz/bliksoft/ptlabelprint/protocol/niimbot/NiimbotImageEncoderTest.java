package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.bliksoft.ptlabelprint.image.PixelSource;

class NiimbotImageEncoderTest {

	@Test
	void allWhiteImageIsAllVoidCollapsedIntoOneRow() {
		PixelSource source = new PixelSource() {
			public int getWidth() {
				return 16;
			}

			public int getHeight() {
				return 10;
			}

			public boolean isBlack(int x, int y) {
				return false;
			}
		};

		EncodedImage image = NiimbotImageEncoder.encode(source, PageColorType.SINGLE_COLOR);

		assertEquals(16, image.getCols());
		assertEquals(10, image.getRows());
		List<ImageRow> rows = image.getRowsData();
		assertEquals(1, rows.size());
		assertEquals(ImageRow.DataType.VOID, rows.get(0).getDataType());
		assertEquals(10, rows.get(0).getRepeat());
	}

	@Test
	void singleBlackPixelProducesOnePixelsRow() {
		PixelSource source = new PixelSource() {
			public int getWidth() {
				return 8;
			}

			public int getHeight() {
				return 3;
			}

			public boolean isBlack(int x, int y) {
				return y == 1 && x == 0;
			}
		};

		EncodedImage image = NiimbotImageEncoder.encode(source, PageColorType.SINGLE_COLOR);

		List<ImageRow> rows = image.getRowsData();
		// row 0 (void), row 1 (pixels, one black pixel), row 2 (void)
		assertEquals(3, rows.size());
		assertEquals(ImageRow.DataType.VOID, rows.get(0).getDataType());
		assertEquals(ImageRow.DataType.PIXELS, rows.get(1).getDataType());
		assertEquals(1, rows.get(1).getBlackPixelsCount());
		assertArrayEquals(new byte[] {(byte) 0x80}, rows.get(1).getRowDataBlack());
		assertEquals(ImageRow.DataType.VOID, rows.get(2).getDataType());
	}

	@Test
	void indexPixelsFindsLeftmostBit() {
		byte[] indexes = NiimbotImageEncoder.indexPixels(new byte[] {(byte) 0x80});
		// leftmost pixel (bit 7) is absolute index 0, encoded big-endian 16-bit
		assertArrayEquals(new byte[] {0x00, 0x00}, indexes);
	}

	@Test
	void indexPixelsFindsMultipleBitsAcrossBytes() {
		// byte0 = 0x01 (rightmost pixel, index 7), byte1 = 0x80 (leftmost pixel of 2nd byte, index 8)
		byte[] indexes = NiimbotImageEncoder.indexPixels(new byte[] {0x01, (byte) 0x80});
		assertArrayEquals(new byte[] {0x00, 0x07, 0x00, 0x08}, indexes);
	}

	@Test
	void countPixelsForBitmapPacketTotalsAllSetBits() {
		NiimbotImageEncoder.PixelCountResult result =
				NiimbotImageEncoder.countPixelsForBitmapPacket(new byte[] {(byte) 0xff, (byte) 0xff}, 144, "total");
		assertEquals(16, result.total);
	}
}
