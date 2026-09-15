package cz.bliksoft.ptlabelprint.image;

import java.awt.image.BufferedImage;

/**
 * Adapts a {@link BufferedImage} to {@link PixelSource} - the one shared, family-agnostic step in
 * the new image pipeline: turning arbitrary standard-Java image content into black/white pixel
 * judgments. Uses simple, fixed-threshold luminance thresholding (standard Rec. 601-ish luminance,
 * compared against a mid-range cutoff) - deliberately basic, not ordered/error-diffusion dithering,
 * matching this project's existing restraint pattern for genuinely complex nice-to-haves (e.g.
 * {@code NiimbotImageEncoder}'s un-ported check-line packets). Dithering is a documented future
 * improvement, not attempted here.
 */
public class BufferedImagePixelSource implements PixelSource {

	/** Out of 255 - a pixel darker than this counts as black. */
	private static final int THRESHOLD = 128;

	private final BufferedImage image;

	public BufferedImagePixelSource(BufferedImage image) {
		this.image = image;
	}

	@Override
	public int getWidth() {
		return image.getWidth();
	}

	@Override
	public int getHeight() {
		return image.getHeight();
	}

	@Override
	public boolean isBlack(int x, int y) {
		int argb = image.getRGB(x, y);
		int alpha = (argb >> 24) & 0xff;
		if (alpha == 0) {
			// Fully transparent - treat as background (white/not-printed), not black.
			return false;
		}

		int r = (argb >> 16) & 0xff;
		int g = (argb >> 8) & 0xff;
		int b = argb & 0xff;
		int luminance = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);

		return luminance < THRESHOLD;
	}
}
