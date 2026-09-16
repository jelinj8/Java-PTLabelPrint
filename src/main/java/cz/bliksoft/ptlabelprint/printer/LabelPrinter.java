package cz.bliksoft.ptlabelprint.printer;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
	 * The connected printer's DPI, physical printhead pixel width, and density range.
	 *
	 * @throws IllegalStateException if called before {@link #connect()}, or - Niimbot only - if the
	 *                                connected model isn't one this project's model catalog recognizes
	 */
	PrinterCapabilities getCapabilities();
}
