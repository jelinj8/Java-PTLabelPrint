package cz.bliksoft.ptlabelprint.image;

/**
 * Resizes a {@link PixelSource}'s printhead-axis (height) dimension to an exact target pixel count
 * - a canvas resize (pad/crop), never a scale, since stretching content to fit would distort it
 * (and, for Phomemo's {@code d-series}, a mismatched height produces a garbled print regardless -
 * see {@code DSeriesPrinter#print}'s own javadoc). Center-anchored: padding is added, or cropping
 * taken, equally from both ends - the more sensible default for a label where the printer's own
 * physical alignment already centers the printhead, rather than top/bottom-anchoring. Width is
 * never touched by this class - only the printhead-axis dimension a real print physically requires
 * to match exactly.
 */
public final class CanvasResize {

	private CanvasResize() {
	}

	/**
	 * Returns a lazy wrapping {@link PixelSource} of {@code source}'s content, padded (white) or
	 * cropped, centered, to exactly {@code targetHeight}. No buffer is materialized.
	 */
	public static PixelSource resizeHeight(PixelSource source, int targetHeight) {
		if (source.getHeight() == targetHeight) {
			return source;
		}

		int sourceHeight = source.getHeight();
		// Positive offset = padding added above the source content; negative = rows cropped off the top
		// (and, symmetrically, off the bottom too, since the same offset applies at both ends).
		int offset = (targetHeight - sourceHeight) / 2;

		return new PixelSource() {
			@Override
			public int getWidth() {
				return source.getWidth();
			}

			@Override
			public int getHeight() {
				return targetHeight;
			}

			@Override
			public boolean isBlack(int x, int y) {
				int sy = y - offset;
				return sy >= 0 && sy < sourceHeight && source.isBlack(x, sy);
			}

			@Override
			public boolean isRed(int x, int y) {
				int sy = y - offset;
				return sy >= 0 && sy < sourceHeight && source.isRed(x, sy);
			}
		};
	}
}
