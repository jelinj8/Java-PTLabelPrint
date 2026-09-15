package cz.bliksoft.ptlabelprint.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageRotationTest {

	@Test
	void swapsWidthAndHeight() {
		PixelSource source = new PixelSource() {
			@Override
			public int getWidth() {
				return 3;
			}

			@Override
			public int getHeight() {
				return 5;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return false;
			}
		};

		PixelSource rotated = ImageRotation.rotate90Clockwise(source);
		assertEquals(5, rotated.getWidth());
		assertEquals(3, rotated.getHeight());
	}

	@Test
	void rotatesAnAsymmetricMarkerCorrectly() {
		// 3-wide x 2-tall source, only the top-left pixel (0,0) is black - an asymmetric marker so
		// the rotation *direction* (not just the dimension swap) is actually verified, matching the
		// same asymmetric-test-pattern principle used for the real-hardware D11_H re-verification.
		PixelSource source = new PixelSource() {
			@Override
			public int getWidth() {
				return 3;
			}

			@Override
			public int getHeight() {
				return 2;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return x == 0 && y == 0;
			}
		};

		PixelSource rotated = ImageRotation.rotate90Clockwise(source);
		assertEquals(2, rotated.getWidth());
		assertEquals(3, rotated.getHeight());

		// A 90CW rotation moves the top-left corner to the top-right corner of the new frame.
		assertTrue(rotated.isBlack(1, 0));
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 2; x++) {
				if (x != 1 || y != 0) {
					assertFalse(rotated.isBlack(x, y), "unexpected black pixel at (" + x + "," + y + ")");
				}
			}
		}
	}

	@Test
	void rotatingFourTimesReturnsToOriginalDimensions() {
		PixelSource source = new PixelSource() {
			@Override
			public int getWidth() {
				return 4;
			}

			@Override
			public int getHeight() {
				return 7;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return (x + y) % 2 == 0;
			}
		};

		PixelSource rotated = source;
		for (int i = 0; i < 4; i++) {
			rotated = ImageRotation.rotate90Clockwise(rotated);
		}

		assertEquals(source.getWidth(), rotated.getWidth());
		assertEquals(source.getHeight(), rotated.getHeight());
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				assertEquals(source.isBlack(x, y), rotated.isBlack(x, y));
			}
		}
	}
}
