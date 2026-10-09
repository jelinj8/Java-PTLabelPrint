package cz.bliksoft.ptlabelprint.image;

/**
 * Lazy whole-image shifts in the printer's own frame (rows = feed direction), for the media
 * position settings of {@code PrintJob}.
 */
public final class ImageShift {

	private ImageShift() {
	}

	/**
	 * {@code source} moved {@code pixels} columns to the right by adding blank columns on the left - or,
	 * if negative, to the left by dropping that many columns from its left edge.
	 */
	public static PixelSource shiftRight(PixelSource source, int pixels) {
		if (pixels == 0) {
			return source;
		}
		return new PixelSource() {
			@Override
			public int getWidth() {
				return source.getWidth() + pixels;
			}

			@Override
			public int getHeight() {
				return source.getHeight();
			}

			@Override
			public boolean isBlack(int x, int y) {
				return x >= pixels && source.isBlack(x - pixels, y);
			}

			@Override
			public boolean isRed(int x, int y) {
				return x >= pixels && source.isRed(x - pixels, y);
			}
		};
	}

	/**
	 * {@code source} moved {@code pixels} rows later in the feed direction (earlier if negative). With
	 * {@code keepLength} the result is as long as {@code source} - rows pushed past either end are
	 * lost and the vacated end is blank; without it the length changes by {@code pixels} instead.
	 */
	public static PixelSource shiftDown(PixelSource source, int pixels, boolean keepLength) {
		if (pixels == 0) {
			return source;
		}
		int height = keepLength ? source.getHeight() : source.getHeight() + pixels;
		return new PixelSource() {
			@Override
			public int getWidth() {
				return source.getWidth();
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public boolean isBlack(int x, int y) {
				int sy = y - pixels;
				return sy >= 0 && sy < source.getHeight() && source.isBlack(x, sy);
			}

			@Override
			public boolean isRed(int x, int y) {
				int sy = y - pixels;
				return sy >= 0 && sy < source.getHeight() && source.isRed(x, sy);
			}
		};
	}
}
