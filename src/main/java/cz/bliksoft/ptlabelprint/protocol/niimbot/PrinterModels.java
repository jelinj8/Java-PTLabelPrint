package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link PrinterModelMeta} entries, looked up by the ID reported via
 * {@link PrinterInfoType#PRINTER_MODEL_ID}. Ported (partially - see {@link PrinterModel}'s
 * javadoc) from niimbluelib's {@code modelsLibrary} / {@code getPrinterMetaById}
 * (src/printer_models.ts).
 *
 * <p>
 * A Phomemo Q30 is <b>not</b> in this table, and never will need to be: confirmed against real
 * hardware (see the project's CLAUDE.md "Status" section) that the Q30 doesn't speak this protocol
 * at all - it and the rest of Phomemo's D-series speak phomymo's own ESC/POS-derived
 * {@code d-series} protocol instead, which belongs in a separate
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} family, not here. (An earlier version of this
 * project's docs incorrectly assumed "similar to D30" in phomymo's README meant Niimbot-protocol
 * compatibility - it didn't; that assumption has been corrected.) {@link #findById(int)} returns
 * empty for any unrecognized ID - callers must handle that gracefully rather than assuming every
 * connected printer resolves to a known entry.
 */
public final class PrinterModels {

	private static final List<PrinterModelMeta> TABLE = Arrays.asList(
			new PrinterModelMeta(PrinterModel.D101, new int[] {2560}, 203, PrintDirection.LEFT, 192,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D11, new int[] {512}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D11_H, new int[] {528}, 300, PrintDirection.LEFT, 142,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D11_PRO, new int[] {531}, 300, PrintDirection.LEFT, 142,
					Arrays.asList(LabelType.TRANSPARENT, LabelType.WITH_GAPS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D110, new int[] {2304, 2305}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D110_M, new int[] {2320}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D11S, new int[] {514}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.HI_D110, new int[] {2305}, 203, PrintDirection.LEFT, 120,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 3),
			new PrinterModelMeta(PrinterModel.HI_NB_D11, new int[] {512}, 203, PrintDirection.LEFT, 120,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.M2_H, new int[] {4608}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK, LabelType.BLACK_MARK_GAP),
					1, 5, 3));

	private PrinterModels() {
	}

	public static List<PrinterModelMeta> all() {
		return TABLE;
	}

	public static Optional<PrinterModelMeta> findById(int modelId) {
		List<PrinterModelMeta> matches = new ArrayList<>();

		for (PrinterModelMeta meta : TABLE) {
			for (int id : meta.getIds()) {
				if (id == modelId) {
					matches.add(meta);
				}
			}
		}

		return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
	}
}
