package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link M110PrinterModelMeta} entries - what {@code printer.LabelPrinter#getCapabilities()}
 * and {@code PhomemoM110LabelPrinter#print} need to know per model, keyed by
 * {@code printer.PrinterCatalog}'s own definition id (same idea as {@link DSeriesPrinterModels}).
 *
 * <p>
 * Like {@code d-series}, the {@code m110} print flow can't learn the printhead width from the
 * device, so it's tabulated here - and only as an upper bound: rows are sent at the image's own
 * width, not padded out to the printhead (see {@link M110Printer}).
 * <ul>
 * <li>{@code phomemo-m421} - <b>hardware-confirmed</b> printing a 40x20mm gap label (320px rows).
 * The 912px printhead figure is what Phomemo's own app sends: a captured print job from it used
 * 114-byte rows ({@code GS v 0} width 114) for a 100mm-wide label.</li>
 * <li>{@code phomemo-m110} (M110/M120) - untested here; 384px is phomymo's own {@code printers.json}
 * {@code widthBytes} (48) for that row, see {@link PhomemoPrinterModels}.</li>
 * </ul>
 * Density is this library's common 1-8 scale ({@link M110Commands#densityToM110}).
 *
 * <p>
 * The last column is {@link M110PrinterModelMeta#getLeftOffsetMm()}, 0 for both - for the M421
 * deliberately: it aligns media to the left and physically starts printing about 1mm in from that
 * edge (measured), which is where a label with the usual 1mm side gap begins, so unshifted output is
 * already right for ordinary stock. Treating that as the zero point keeps
 * {@code PrintJob#getMediaSideGapMm()} a plain "move the print this much further right" for stock
 * with a wider gap, rather than making every caller know the printer's 1mm. The M110's 0 is simply
 * unknown.
 */
public final class M110PrinterModels {

	private static final List<M110PrinterModelMeta> TABLE = Arrays.asList(
			new M110PrinterModelMeta("phomemo-m110", 203, 384, 1, 8, 0),
			new M110PrinterModelMeta("phomemo-m421", 203, 912, 1, 8, 0));

	private M110PrinterModels() {
	}

	public static List<M110PrinterModelMeta> all() {
		return TABLE;
	}

	public static Optional<M110PrinterModelMeta> findById(String catalogId) {
		return TABLE.stream().filter(m -> m.getCatalogId().equals(catalogId)).findFirst();
	}
}
