package cz.bliksoft.ptlabelprint.printer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.IntConsumer;

import cz.bliksoft.ptlabelprint.image.BufferedImagePixelSource;
import cz.bliksoft.ptlabelprint.image.CanvasResize;
import cz.bliksoft.ptlabelprint.image.PixelSource;
import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;
import cz.bliksoft.ptlabelprint.protocol.Transport;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinterModelMeta;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinterModels;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#PHOMEMO_D_SERIES}. Connection lifecycle is entirely
 * inherited from {@link PhomemoLabelPrinter} (this protocol family has no connect-handshake/
 * info-query concept at all - see {@link DSeriesPrinter}'s javadoc). Exposes both the low-level
 * {@link #print(RasterImage, int, boolean, int, IntConsumer)} pass-through (full control) and the
 * common {@link #print(BufferedImage, PrintJob)} (see {@link LabelPrinter}'s own javadoc).
 */
public class PhomemoDSeriesLabelPrinter extends PhomemoLabelPrinter {

	public PhomemoDSeriesLabelPrinter(PrinterDefinition definition, Transport transport) {
		super(definition, PrinterFamily.PHOMEMO_D_SERIES, transport);
	}

	/** See {@link DSeriesPrinter#print} - the image height constraint documented there still applies. */
	public void print(RasterImage image, int density, boolean continuous, int feedDots, IntConsumer onProgress) throws IOException {
		DSeriesPrinter.print(transport, image, density, continuous, feedDots, onProgress);
	}

	@Override
	public PrinterCapabilities getCapabilities() {
		DSeriesPrinterModelMeta meta = requireModelMeta();
		return new PrinterCapabilities(meta.getDpi(), meta.getPrintheadPixels(), meta.getDensityMin(), meta.getDensityMax());
	}

	/**
	 * See {@link LabelPrinter}'s own javadoc for the full rotation/density/copies algorithm. `d-series`
	 * has no native multi-copy concept - each copy is sent as an independent, fire-and-forget print.
	 */
	@Override
	public void print(BufferedImage image, PrintJob job) throws IOException {
		DSeriesPrinterModelMeta meta = requireModelMeta();

		int density = job.getDensity() != null ? job.getDensity() : 6;
		if (density < meta.getDensityMin() || density > meta.getDensityMax()) {
			throw new IllegalArgumentException(
					"Density " + density + " out of range [" + meta.getDensityMin() + ", " + meta.getDensityMax() + "]");
		}

		PixelSource source = new BufferedImagePixelSource(image);
		// mandatoryRotate90=true: d-series always rotates - RotationResolver returns the image
		// *before* that mandatory rotation, which DSeriesPrinter.print (via RasterImage's own
		// existing, unchanged rotate90Clockwise()) still applies below, exactly as it always has.
		PixelSource resolved = RotationResolver.resolve(source, true, meta.getPrintheadPixels(), job.getRotation());
		PixelSource resized = CanvasResize.resizeHeight(resolved, meta.getPrintheadPixels());
		RasterImage raster = RasterImage.fromPixelSource(resized);

		for (int i = 0; i < job.getCopies(); i++) {
			DSeriesPrinter.print(transport, raster, density, job.isContinuousMedia(), 0, null);
		}
	}

	private DSeriesPrinterModelMeta requireModelMeta() {
		return DSeriesPrinterModels.findById(getDefinition().getId())
				.orElseThrow(() -> new IllegalStateException(
						"No d-series model metadata for " + getDefinition() + " - see DSeriesPrinterModels"));
	}
}
