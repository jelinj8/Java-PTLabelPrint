package cz.bliksoft.ptlabelprint.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CanvasResizeTest {

	/** All-black 4x4 source (width x height), row 0 at the top. */
	private static PixelSource allBlack(int width, int height) {
		return new PixelSource() {
			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return true;
			}
		};
	}

	@Test
	void sameHeightReturnsSourceUnchanged() {
		PixelSource source = allBlack(4, 4);
		assertEquals(source, CanvasResize.resizeHeight(source, 4));
	}

	@Test
	void padsSymmetricallyWithWhiteWhenTargetIsTaller() {
		PixelSource source = allBlack(4, 4);
		PixelSource resized = CanvasResize.resizeHeight(source, 8);

		assertEquals(4, resized.getWidth());
		assertEquals(8, resized.getHeight());
		// offset = (8-4)/2 = 2: rows 0-1 padding (white), rows 2-5 source content (black), rows 6-7 padding (white)
		assertFalse(resized.isBlack(0, 0));
		assertFalse(resized.isBlack(0, 1));
		assertTrue(resized.isBlack(0, 2));
		assertTrue(resized.isBlack(0, 5));
		assertFalse(resized.isBlack(0, 6));
		assertFalse(resized.isBlack(0, 7));
	}

	@Test
	void cropsSymmetricallyWhenTargetIsShorter() {
		PixelSource source = allBlack(4, 8);
		PixelSource resized = CanvasResize.resizeHeight(source, 4);

		assertEquals(4, resized.getWidth());
		assertEquals(4, resized.getHeight());
		// All-black source stays all-black after cropping - just fewer rows.
		for (int y = 0; y < 4; y++) {
			assertTrue(resized.isBlack(0, y));
		}
	}

	@Test
	void widthIsNeverTouched() {
		PixelSource source = allBlack(12, 4);
		PixelSource resized = CanvasResize.resizeHeight(source, 20);
		assertEquals(12, resized.getWidth());
	}
}
