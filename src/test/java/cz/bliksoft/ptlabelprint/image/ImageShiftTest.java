package cz.bliksoft.ptlabelprint.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageShiftTest {

	/** 2 columns x 4 rows, black only in row {@code blackRow}. */
	private static PixelSource rowMarked(int blackRow) {
		return new PixelSource() {
			@Override
			public int getWidth() {
				return 2;
			}

			@Override
			public int getHeight() {
				return 4;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return y == blackRow;
			}
		};
	}

	@Test
	void zeroShiftIsTheSourceItself() {
		PixelSource source = rowMarked(0);
		assertSame(source, ImageShift.shiftDown(source, 0, true));
	}

	@Test
	void shiftDownKeepingLengthMovesRowsAndCutsThePushedOutEnd() {
		PixelSource shifted = ImageShift.shiftDown(rowMarked(1), 2, true);
		assertEquals(4, shifted.getHeight());
		assertTrue(shifted.isBlack(0, 3));
		assertFalse(shifted.isBlack(0, 1));

		PixelSource pushedOut = ImageShift.shiftDown(rowMarked(3), 1, true);
		for (int y = 0; y < 4; y++) {
			assertFalse(pushedOut.isBlack(0, y));
		}
	}

	@Test
	void negativeShiftDownMovesEarlierAndBlanksTheEnd() {
		PixelSource shifted = ImageShift.shiftDown(rowMarked(2), -1, true);
		assertTrue(shifted.isBlack(1, 1));
		assertFalse(shifted.isBlack(1, 3));
	}

	@Test
	void shiftDownWithoutKeepingLengthChangesTheLength() {
		assertEquals(6, ImageShift.shiftDown(rowMarked(0), 2, false).getHeight());
		PixelSource shorter = ImageShift.shiftDown(rowMarked(1), -1, false);
		assertEquals(3, shorter.getHeight());
		assertTrue(shorter.isBlack(0, 0));
	}
}
