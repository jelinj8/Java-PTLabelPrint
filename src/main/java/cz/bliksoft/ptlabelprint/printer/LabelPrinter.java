package cz.bliksoft.ptlabelprint.printer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;

/**
 * Common connection lifecycle across protocol families, plus a minimal, deliberately-scoped common
 * print entry point: {@link #print(BufferedImage, PrintJob)} and {@link #getCapabilities()}.
 *
 * <p>
 * This is <b>not</b> a full unification of each family's real capabilities -
 * {@code protocol.niimbot} and {@code protocol.phomemo} still have genuinely different image
 * representations, options, and richer per-family APIs (e.g. Niimbot exposes printer info -
 * model/battery/serial/RFID - that Phomemo's fire-and-forget {@code d-series} protocol has no
 * equivalent for at all; Niimbot supports page color and tube/half-cut options this common surface
 * doesn't expose; Phomemo has a lower-level raw {@code RasterImage} call for full control). Use
 * {@link #getDefinition()} (or an {@code instanceof} check, or {@code NiimbotLabelPrinter#getDevice()})
 * to reach each family's own real API for anything beyond what's covered here.
 *
 * <p>
 * What {@link #print} covers: an image (a standard {@link BufferedImage} - no manufacturer-specific
 * type appears in this signature; each family converts it to its own native representation
 * internally, exactly like a future ZPL-generating Zebra family would convert to ZPL text rather
 * than a raster), copy count, gapped-vs-continuous media, and density (passed through in whichever
 * family-native scale applies to the connected printer - not cross-family-normalized - and
 * validated against that family's real range, throwing a clear exception naming the valid range if
 * out of bounds), plus rotation (see {@link Rotation}'s own javadoc for the full algorithm and why
 * {@link Rotation#NONE} - mandatory per-model orientation only, no auto-fit - is the default rather
 * than {@link Rotation#AUTO}: gapped/die-cut media has both axes fixed by the physical label stock,
 * so auto-fitting the printhead axis by rotating can just move a minor overage onto the
 * equally-fixed feed axis instead of fixing it - safe only for continuous media, or when a caller
 * explicitly opts in per print). {@link #getCapabilities()} exposes what's needed to reason about
 * fit before printing: DPI, the printhead's physical pixel width, and the density range.
 */
public interface LabelPrinter extends AutoCloseable {

	PrinterDefinition getDefinition();

	void connect() throws IOException, TimeoutException;

	@Override
	void close() throws IOException;

	boolean isConnected();

	/**
	 * Prints {@code image}, converting it internally to this family's own native wire format and
	 * running its own real print pipeline underneath - see this interface's own javadoc for exactly
	 * what's covered and what isn't.
	 */
	void print(BufferedImage image, PrintJob job) throws IOException, TimeoutException;

	/**
	 * Prints a sequence of images, collapsing any run of consecutive identical images into a single
	 * {@link #print} call with its copy count multiplied by the run length, instead of one
	 * {@link #print} call per image. Identity ({@code ==}) is checked first - the normal case, since a
	 * caller decoding from a source that already reuses one instance per logical copy (e.g. a ZPL
	 * template's own {@code printQuantity}) preserves that identity through decoding - so the common
	 * batch doesn't pay for pixel comparison at all; see {@link #imagesEqualByPixels} for the
	 * (currently unused by this method, kept for a future distinct-object-but-same-content case) slower
	 * fallback. Each family's own {@link #print}/copies handling then decides how to realize the
	 * collapsed call - some (Niimbot) transmit the image once and let the printer replicate it
	 * natively; others (Phomemo d-series, no native multi-copy concept) still resend the raster
	 * internally per copy - either way this is never worse than the naive per-image loop, and avoids
	 * redundant re-encoding/re-transmission, especially costly over a slow transport like Bluetooth.
	 * {@code job}'s own copies count is used as a per-image multiplier (normally 1) rather than
	 * assumed, and is restored to that original value before this method returns.
	 */
	default void printBatch(List<BufferedImage> images, PrintJob job) throws IOException, TimeoutException {
		int baseCopies = job.getCopies();
		int i = 0;
		while (i < images.size()) {
			BufferedImage current = images.get(i);
			int runLength = 1;
			while (i + runLength < images.size() && images.get(i + runLength) == current)
				runLength++;
			job.setCopies(baseCopies * runLength);
			print(current, job);
			i += runLength;
		}
		job.setCopies(baseCopies);
	}

	/**
	 * Slower, currently-unused-by-{@link #printBatch} fallback for detecting equal-content-but-distinct
	 * image instances (e.g. a template emitting several separately-authored but identical labels) -
	 * pixel-by-pixel, since {@link BufferedImage} has no useful {@code equals()}. Kept available for
	 * when that case is worth optimizing too, without needing to touch {@link #printBatch}'s hot path.
	 */
	static boolean imagesEqualByPixels(BufferedImage a, BufferedImage b) {
		if (a == b)
			return true;
		int w = a.getWidth(), h = a.getHeight();
		if (w != b.getWidth() || h != b.getHeight())
			return false;
		return Arrays.equals(a.getRGB(0, 0, w, h, null, 0, w), b.getRGB(0, 0, w, h, null, 0, w));
	}

	/**
	 * The connected printer's DPI, physical printhead pixel width, and density range.
	 *
	 * @throws IllegalStateException if called before {@link #connect()}, or - Niimbot only - if the
	 *                                connected model isn't one this project's model catalog recognizes
	 */
	PrinterCapabilities getCapabilities();
}
