package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Registry of {@link DSeriesPrinterModelMeta} entries - what {@code printer.LabelPrinter#getCapabilities()}
 * and the canvas-resize step in {@code PhomemoDSeriesLabelPrinter#print} need to know per model,
 * mirroring the shape of {@code protocol.niimbot}'s {@code PrinterModel}/{@code PrinterModelMeta}/
 * {@code PrinterModels}.
 *
 * <p>
 * Phomemo's `d-series` protocol has no info-query capability at all (see {@link DSeriesPrinter}'s
 * javadoc) - it can never learn its own printhead width from the device the way Niimbot's
 * {@code PrinterModelMeta} does by asking. The only confirmed data point anywhere in this project is
 * the Q30: 203 DPI, 96px (12mm) printhead, density range 1-8. phomymo's own {@code printers.json}
 * doesn't differentiate D30/D35/D50/Q30/Q30S at all (one generic row, no per-model dimensions) - so
 * every entry below is identical, and that uniformity is an <b>assumption</b> inherited from
 * phomymo's own undifferentiated data, not independently confirmed for anything but the Q30. Treat
 * it as the best available default, not a verified fact, for the other four.
 */
public final class DSeriesPrinterModels {

	private static final List<DSeriesPrinterModelMeta> TABLE = Arrays.asList(
			new DSeriesPrinterModelMeta(DSeriesPrinterModel.D30, 203, 96, 1, 8),
			new DSeriesPrinterModelMeta(DSeriesPrinterModel.D35, 203, 96, 1, 8),
			new DSeriesPrinterModelMeta(DSeriesPrinterModel.D50, 203, 96, 1, 8),
			new DSeriesPrinterModelMeta(DSeriesPrinterModel.Q30, 203, 96, 1, 8),
			new DSeriesPrinterModelMeta(DSeriesPrinterModel.Q30S, 203, 96, 1, 8));

	private DSeriesPrinterModels() {
	}

	public static List<DSeriesPrinterModelMeta> all() {
		return TABLE;
	}

	public static Optional<DSeriesPrinterModelMeta> findByModel(DSeriesPrinterModel model) {
		return TABLE.stream().filter(m -> m.getModel() == model).findFirst();
	}

	/**
	 * Looks up by {@code printer.PrinterCatalog}'s own definition id (e.g. {@code "phomemo-q30"}),
	 * so {@code PhomemoDSeriesLabelPrinter} can resolve its own metadata from
	 * {@code getDefinition().getId()} without needing a second, separately-maintained id scheme.
	 */
	public static Optional<DSeriesPrinterModelMeta> findById(String catalogId) {
		if (catalogId == null || !catalogId.startsWith("phomemo-")) {
			return Optional.empty();
		}
		String modelName = catalogId.substring("phomemo-".length()).toUpperCase(Locale.ROOT);
		try {
			return findByModel(DSeriesPrinterModel.valueOf(modelName));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
