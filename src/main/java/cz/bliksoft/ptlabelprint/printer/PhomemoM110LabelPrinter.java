package cz.bliksoft.ptlabelprint.printer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.IntConsumer;

import cz.bliksoft.ptlabelprint.image.BufferedImagePixelSource;
import cz.bliksoft.ptlabelprint.image.CanvasResize;
import cz.bliksoft.ptlabelprint.image.ImageRotation;
import cz.bliksoft.ptlabelprint.image.PixelSource;
import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;
import cz.bliksoft.ptlabelprint.protocol.Transport;
import cz.bliksoft.ptlabelprint.protocol.phomemo.M110Printer;
import cz.bliksoft.ptlabelprint.protocol.phomemo.M110PrinterModelMeta;
import cz.bliksoft.ptlabelprint.protocol.phomemo.M110PrinterModels;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#PHOMEMO_M110} (M110/M120, and the hardware-confirmed
 * M421). Connection lifecycle is inherited from {@link PhomemoLabelPrinter}. Exposes both the
 * low-level {@link #print(RasterImage, int, boolean, IntConsumer)} pass-through and the common
 * {@link #print(BufferedImage, PrintJob)} (see {@link LabelPrinter}'s own javadoc).
 *
 * <p>
 * <b>The M421 needs an OS-level bonded link</b> before {@link #connect()} can subscribe to its
 * notify characteristic - confirmed on real hardware (Windows reports "attribute requires
 * authentication" otherwise). Pair it through the OS first; unlike every other printer this project
 * has tested, it doesn't work unpaired.
 */
public class PhomemoM110LabelPrinter extends PhomemoLabelPrinter {

	/** The protocol's own default (10 on its 1-15 scale) - the density the M421 was confirmed at. */
	private static final int DEFAULT_DENSITY = 4;

	private M110Printer.LinkInfo linkInfo;

	public PhomemoM110LabelPrinter(PrinterDefinition definition, Transport transport) {
		super(definition, PrinterFamily.PHOMEMO_M110, transport);
	}

	/** Listens for the printer's flow-control announcement while connecting - see {@link M110Printer.LinkInfo}. */
	@Override
	public void connect() throws IOException {
		linkInfo = M110Printer.listenForLinkInfo(transport);
		super.connect();
	}

	/** See {@link M110Printer#print}, including what its return value means. */
	public boolean print(RasterImage image, int density, boolean continuous, IntConsumer onProgress) throws IOException {
		return M110Printer.print(transport, image, density, continuous, onProgress, linkInfo, null);
	}

	@Override
	public PrinterCapabilities getCapabilities() {
		M110PrinterModelMeta meta = requireModelMeta();
		return new PrinterCapabilities(meta.getDpi(), meta.getPrintheadPixels(), meta.getDensityMin(), meta.getDensityMax());
	}

	/**
	 * See {@link LabelPrinter}'s own javadoc for the full rotation/density/copies algorithm. No
	 * mandatory rotation, and - unlike {@code d-series} - rows are sent at the image's own width, not
	 * padded to the printhead's: only an image wider than the printhead is touched (center-cropped).
	 * The printer aligns media to the left, so the image starts at the first printhead dot unless
	 * {@link PrintJob#getMediaSideGapMm()} moves it further in; {@link PrintJob#getTopOffsetMm()}
	 * moves it along the feed direction.
	 * {@code m110} has no native multi-copy concept - each copy is an independent print, and each one
	 * waits for the printer to finish (see {@link M110Printer}) before the next starts or this returns.
	 */
	@Override
	public void print(BufferedImage image, PrintJob job) throws IOException {
		M110PrinterModelMeta meta = requireModelMeta();

		int density = job.getDensity() != null ? job.getDensity() : DEFAULT_DENSITY;
		if (density < meta.getDensityMin() || density > meta.getDensityMax()) {
			throw new IllegalArgumentException(
					"Density " + density + " out of range [" + meta.getDensityMin() + ", " + meta.getDensityMax() + "]");
		}

		PixelSource source = new BufferedImagePixelSource(image);
		PixelSource resolved = RotationResolver.resolve(source, false, meta.getPrintheadPixels(), job.getRotation());
		// The job's extra side gap, less whatever this model is taken to handle by itself (nothing,
		// for the models known so far - see M110PrinterModels).
		int shiftPixels = job.getMediaSideGapMm() == null ? 0
				: (int) Math.round((job.getMediaSideGapMm() - meta.getLeftOffsetMm()) * meta.getDpi() / 25.4);
		if (shiftPixels >= meta.getPrintheadPixels() || -shiftPixels >= resolved.getWidth()) {
			throw new IllegalArgumentException("Media side gap " + job.getMediaSideGapMm() + " mm moves the image off the printhead");
		}
		PixelSource fitted = cropWidth(resolved, meta.getPrintheadPixels() - Math.max(shiftPixels, 0));
		int topPixels = (int) Math.round(job.getTopOffsetMm() * meta.getDpi() / 25.4);
		if (Math.abs(topPixels) >= fitted.getHeight()) {
			throw new IllegalArgumentException("Top offset " + job.getTopOffsetMm() + " mm is longer than the image");
		}
		RasterImage raster = RasterImage.fromPixelSource(
				shiftDown(shiftRight(fitted, shiftPixels), topPixels, job.isTopOffsetKeepsLength()));

		for (int i = 0; i < job.getCopies(); i++) {
			M110Printer.print(transport, raster, density, job.isContinuousMedia(), null, linkInfo, null);
		}
	}

	/**
	 * {@code source} moved {@code pixels} columns to the right by adding blank columns on the left - or,
	 * if negative, to the left by dropping that many columns from its left edge.
	 */
	private static PixelSource shiftRight(PixelSource source, int pixels) {
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
	private static PixelSource shiftDown(PixelSource source, int pixels, boolean keepLength) {
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

	/** Center-crops {@code source} to at most {@code maxWidth} - {@link CanvasResize} only works on height, hence the rotate round trip. */
	private static PixelSource cropWidth(PixelSource source, int maxWidth) {
		if (source.getWidth() <= maxWidth) {
			return source;
		}
		PixelSource cropped = CanvasResize.resizeHeight(ImageRotation.rotate90Clockwise(source), maxWidth);
		for (int i = 0; i < 3; i++) {
			cropped = ImageRotation.rotate90Clockwise(cropped);
		}
		return cropped;
	}

	private M110PrinterModelMeta requireModelMeta() {
		return M110PrinterModels.findById(getDefinition().getId())
				.orElseThrow(() -> new IllegalStateException(
						"No m110 model metadata for " + getDefinition() + " - see M110PrinterModels"));
	}
}
