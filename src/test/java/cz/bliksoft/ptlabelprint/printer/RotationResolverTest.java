package cz.bliksoft.ptlabelprint.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import cz.bliksoft.ptlabelprint.image.PixelSource;
import org.junit.jupiter.api.Test;

class RotationResolverTest {

	private static PixelSource sized(int width, int height) {
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
				return false;
			}
		};
	}

	@Test
	void noneNeverRotatesRegardlessOfFit() {
		PixelSource source = sized(200, 50);
		// mandatoryRotate90=false, so effective axis (width=200) doesn't fit printheadPixels=96 -
		// AUTO would rotate, but NONE must not.
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.NONE);
		assertSame(source, result);
	}

	@Test
	void explicitCw90PreRotatesRegardlessOfFit() {
		PixelSource source = sized(200, 50);
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.CW_90);
		assertEquals(50, result.getWidth());
		assertEquals(200, result.getHeight());
	}

	@Test
	void explicitCw180DoesNotSwapDimensions() {
		PixelSource source = sized(200, 50);
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.CW_180);
		assertEquals(200, result.getWidth());
		assertEquals(50, result.getHeight());
	}

	@Test
	void autoLeavesImageAloneWhenItAlreadyFitsNoMandatoryRotation() {
		// mandatoryRotate90=false -> effective axis = width. width=80 fits printheadPixels=96.
		PixelSource source = sized(80, 200);
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.AUTO);
		assertSame(source, result);
	}

	@Test
	void autoRotatesWhenTheSwappedAxisWouldFitNoMandatoryRotation() {
		// mandatoryRotate90=false -> effective axis = width = 200, doesn't fit 96.
		// Rotated candidate's effective axis = width of rotated = original height = 80, fits 96.
		PixelSource source = sized(200, 80);
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.AUTO);
		assertEquals(80, result.getWidth());
		assertEquals(200, result.getHeight());
	}

	@Test
	void autoGivesUpWhenNeitherOrientationFits() {
		PixelSource source = sized(300, 300);
		PixelSource result = RotationResolver.resolve(source, false, 96, Rotation.AUTO);
		assertSame(source, result);
	}

	@Test
	void autoRespectsMandatoryRotationWhenComputingTheEffectiveAxis() {
		// mandatoryRotate90=true -> effective axis = height. height=80 fits printheadPixels=96,
		// even though width=200 would not - candidate A must be chosen without any extra rotation.
		PixelSource source = sized(200, 80);
		PixelSource result = RotationResolver.resolve(source, true, 96, Rotation.AUTO);
		assertSame(source, result);
	}

	@Test
	void autoAddsExtraRotationOnTopOfMandatoryWhenNeeded() {
		// mandatoryRotate90=true -> effective axis = height = 200, doesn't fit 96.
		// Extra-rotated candidate's effective axis = height of rotated = original width = 80, fits 96.
		PixelSource source = sized(80, 200);
		PixelSource result = RotationResolver.resolve(source, true, 96, Rotation.AUTO);
		assertEquals(200, result.getWidth());
		assertEquals(80, result.getHeight());
	}
}
