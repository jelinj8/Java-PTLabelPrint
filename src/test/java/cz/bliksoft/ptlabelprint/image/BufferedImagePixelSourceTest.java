package cz.bliksoft.ptlabelprint.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class BufferedImagePixelSourceTest {

	@Test
	void blackPixelIsBlack() {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xff000000); // opaque black
		PixelSource source = new BufferedImagePixelSource(image);
		assertTrue(source.isBlack(0, 0));
	}

	@Test
	void whitePixelIsNotBlack() {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xffffffff); // opaque white
		PixelSource source = new BufferedImagePixelSource(image);
		assertFalse(source.isBlack(0, 0));
	}

	@Test
	void fullyTransparentPixelIsNotBlackRegardlessOfColor() {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0x00000000); // fully transparent, RGB=black
		PixelSource source = new BufferedImagePixelSource(image);
		assertFalse(source.isBlack(0, 0));
	}

	@Test
	void dimensionsMatchTheUnderlyingImage() {
		BufferedImage image = new BufferedImage(7, 11, BufferedImage.TYPE_INT_ARGB);
		PixelSource source = new BufferedImagePixelSource(image);
		org.junit.jupiter.api.Assertions.assertEquals(7, source.getWidth());
		org.junit.jupiter.api.Assertions.assertEquals(11, source.getHeight());
	}
}
