package cz.bliksoft.ptlabelprint.image;

/**
 * Rotates a {@link PixelSource} 90 degrees clockwise - a lazy wrapping transform, no buffer
 * materialized. Applied 0-3 times by callers to compose any multiple of 90 degrees (see
 * {@code printer.LabelPrinter}'s rotation algorithm, and {@code protocol.niimbot.PrinterModelMeta#getPrintDirection()}
 * for where a mandatory rotation comes from per Niimbot model). Same rotation direction as the
 * already-proven {@code protocol.phomemo.RasterImage#rotate90Clockwise()}, which keeps operating on
 * its own packed byte format independently - this class is for the pre-pack, {@link PixelSource}
 * level only.
 */
public final class ImageRotation {

	private ImageRotation() {
	}

	public static PixelSource rotate90Clockwise(PixelSource source) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		return new PixelSource() {
			@Override
			public int getWidth() {
				return sourceHeight;
			}

			@Override
			public int getHeight() {
				return sourceWidth;
			}

			@Override
			public boolean isBlack(int x, int y) {
				return source.isBlack(y, sourceHeight - 1 - x);
			}

			@Override
			public boolean isRed(int x, int y) {
				return source.isRed(y, sourceHeight - 1 - x);
			}
		};
	}
}
