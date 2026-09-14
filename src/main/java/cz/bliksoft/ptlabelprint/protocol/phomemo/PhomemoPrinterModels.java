package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link PhomemoPrinterModelMeta} entries, ported verbatim from phomymo's
 * {@code src/web/printers.json} - the 12 models across the 6 protocol tags this project has
 * <b>not</b> yet written print-flow code for ({@code p12}, {@code m02}, generic {@code m-series},
 * {@code m04}, {@code m110}, {@code tspl}). "Cataloged, not implemented" means: real specs (width,
 * dpi, alignment, rotation, tape) are captured here so a device can be correctly identified and
 * described, but there is no command-builder/print-flow code anywhere in
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} for any of these 6 tags - see
 * {@link cz.bliksoft.ptlabelprint.printer.PrinterFamily} and
 * {@link cz.bliksoft.ptlabelprint.printer.UnimplementedPrinterFamilyException} for how that's
 * surfaced through the printer-abstraction layer, and the project's CLAUDE.md "Protocol families"
 * section for the actual command bytes each tag would need if/when one gets ported.
 *
 * <p>
 * <b>3 of these 17 models have no reachable {@link cz.bliksoft.ptlabelprint.printer.PrinterCatalog}
 * entry</b> ({@code M04S_80}, {@code M04S_110}, {@code M110S}): phomymo itself ships them with an
 * empty {@code namePatterns} list - manual-selection-only, same physical hardware as
 * {@code M04S_53}/{@code M110} respectively (a caller who already knows they have one of these
 * constructs a {@code PrinterDefinition} directly, e.g.
 * {@code new PrinterDefinition("phomemo-m110s", PrinterFamily.PHOMEMO_M110, Collections.emptyList(), false)}
 * - there is nothing for {@code PrinterCatalog}'s name-based heuristic to match against). They are
 * still listed here in full, for documentation/manual construction.
 */
public final class PhomemoPrinterModels {

	private static final List<PhomemoPrinterModelMeta> TABLE = Arrays.asList(
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.P12, "P12 / P12 Pro", "p12", 12, 203,
					PhomemoAlignment.CENTER, true, true, new int[] {12}, 12, "tape"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.A30, "A30", "p12", 15, 203, PhomemoAlignment.CENTER, true,
					true, new int[] {12, 14, 15}, 15, "tape"),

			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M02, "M02 / M02S / M02X", "m02", 48, 203,
					PhomemoAlignment.CENTER, false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M02_PRO, "M02 Pro", "m02", 78, 300, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),

			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M03, "M03", "m-series", 54, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.T02, "T02", "m-series", 48, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M200, "M200", "m-series", 76, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M250, "M250", "m-series", 72, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M220, "M220", "m-series", 72, 203, PhomemoAlignment.RIGHT,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M221, "M221", "m-series", 72, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M260, "M260", "m-series", 72, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "m-series"),

			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M04S_53, "M04S - 53mm paper", "m04", 75, 300,
					PhomemoAlignment.CENTER, false, false, null, null, "m-series"),
			// no PrinterCatalog entry - see class javadoc
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M04S_80, "M04S - 80mm paper", "m04", 112, 300,
					PhomemoAlignment.CENTER, false, false, null, null, "m-series"),
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M04S_110, "M04S - 110mm paper", "m04", 154, 300,
					PhomemoAlignment.CENTER, false, false, null, null, "m-series"),

			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M110, "M110 / M120", "m110", 48, 203,
					PhomemoAlignment.CENTER, false, false, null, null, "m-series"),
			// no PrinterCatalog entry - see class javadoc (reportedly advertises as "Q..."-prefixed)
			new PhomemoPrinterModelMeta(PhomemoPrinterModel.M110S, "M110S", "m110", 48, 203, PhomemoAlignment.RIGHT,
					false, false, null, null, "m-series"),

			new PhomemoPrinterModelMeta(PhomemoPrinterModel.PM241, "PM-241-BT", "tspl", 102, 203, PhomemoAlignment.CENTER,
					false, false, null, null, "pm241"));

	private PhomemoPrinterModels() {
	}

	public static List<PhomemoPrinterModelMeta> all() {
		return TABLE;
	}

	public static Optional<PhomemoPrinterModelMeta> findByModel(PhomemoPrinterModel model) {
		List<PhomemoPrinterModelMeta> matches = new ArrayList<>();
		for (PhomemoPrinterModelMeta meta : TABLE) {
			if (meta.getModel() == model) {
				matches.add(meta);
			}
		}
		return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
	}
}
