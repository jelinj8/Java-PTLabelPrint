package cz.bliksoft.ptlabelprint.printer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Registry of {@link PrinterDefinition}s, matched against a BLE advertised name the same way
 * phomymo's own {@code detectPrinterConfig} does: case-insensitive prefix match, longest prefix
 * wins (so a device named {@code "Q30S-xxxx"} matches the more specific {@code "Q30S"} entry, not
 * the shorter {@code "Q30"} one it also technically starts with).
 *
 * <p>
 * <b>This is a best-effort heuristic, not authoritative</b> - two different manufacturers can and
 * do reuse similar model-name conventions (that's exactly how this project's Phomemo Q30 got
 * mistaken for a Niimbot-protocol device earlier - see {@code protocol.niimbot}'s package-info).
 * {@link #detect(String)} returns every entry tied for the longest matching prefix, which is
 * usually one entry but is more than one if the catalog ever contains a genuine ambiguous
 * collision - callers must handle that case explicitly (e.g. by asking the user which family to
 * use, matching phomymo's own manual override option) rather than silently picking one. Callers
 * that already know the family (e.g. a user override) can skip detection entirely and construct
 * {@link NiimbotLabelPrinter}/{@link PhomemoDSeriesLabelPrinter} directly.
 */
public final class PrinterCatalog {

	private static final List<PrinterDefinition> ENTRIES = Collections.unmodifiableList(Arrays.asList(
			// Niimbot-branded printers (cz.bliksoft.ptlabelprint.protocol.niimbot). Each name prefix
			// is that model's niimbluelib PrinterModel enum name - confirmed against real hardware
			// for D11_H ("D11_H-G412010570") and M2_H ("M2_H-I814050044") only. The other 75 entries
			// are ported into PrinterModels.java from niimbluelib's verbatim vendor id[] data, but
			// their name-prefix here is *guessed* from the enum-naming convention (prefix == bare
			// enum name), not an observed device advertisement - unlike PrinterModelMeta.getIds(),
			// which is real vendor data. Treat every prefix below other than D11_H/M2_H as unverified
			// until a real device confirms it; catalogHasNoAmbiguousEntriesAsShipped() in
			// PrinterCatalogTest is the mechanical check that they don't collide with each other or
			// with the Phomemo entries below - it doesn't and can't confirm they're the *right*
			// prefixes, only that they're mutually distinguishable.
			def("A1_PRO", PrinterFamily.NIIMBOT, false, "A1_PRO"),
			def("A20", PrinterFamily.NIIMBOT, false, "A20"),
			def("A203", PrinterFamily.NIIMBOT, false, "A203"),
			def("A63", PrinterFamily.NIIMBOT, false, "A63"),
			def("A8", PrinterFamily.NIIMBOT, false, "A8"),
			def("A8_P", PrinterFamily.NIIMBOT, false, "A8_P"),
			def("B1", PrinterFamily.NIIMBOT, false, "B1"),
			def("B1_PRO", PrinterFamily.NIIMBOT, false, "B1_PRO"),
			def("B1_SE", PrinterFamily.NIIMBOT, false, "B1_SE"),
			def("B11", PrinterFamily.NIIMBOT, false, "B11"),
			def("B16", PrinterFamily.NIIMBOT, false, "B16"),
			def("B18", PrinterFamily.NIIMBOT, false, "B18"),
			def("B18S", PrinterFamily.NIIMBOT, false, "B18S"),
			def("B2", PrinterFamily.NIIMBOT, false, "B2"),
			def("B2_PRO", PrinterFamily.NIIMBOT, false, "B2_PRO"),
			def("B203", PrinterFamily.NIIMBOT, false, "B203"),
			def("B21", PrinterFamily.NIIMBOT, false, "B21"),
			def("B21_PRO", PrinterFamily.NIIMBOT, false, "B21_PRO"),
			def("B21_C2B", PrinterFamily.NIIMBOT, false, "B21_C2B"),
			def("B21_L2B", PrinterFamily.NIIMBOT, false, "B21_L2B"),
			def("B21S", PrinterFamily.NIIMBOT, false, "B21S"),
			def("B21S_C2B", PrinterFamily.NIIMBOT, false, "B21S_C2B"),
			def("B3", PrinterFamily.NIIMBOT, false, "B3"),
			def("B31", PrinterFamily.NIIMBOT, false, "B31"),
			def("B32", PrinterFamily.NIIMBOT, false, "B32"),
			def("B32R", PrinterFamily.NIIMBOT, false, "B32R"),
			def("B3S", PrinterFamily.NIIMBOT, false, "B3S"),
			def("B3S_A", PrinterFamily.NIIMBOT, false, "B3S_A"),
			def("B3S_P", PrinterFamily.NIIMBOT, false, "B3S_P"),
			def("B4", PrinterFamily.NIIMBOT, false, "B4"),
			def("B4_PRO", PrinterFamily.NIIMBOT, false, "B4_PRO"),
			def("B50", PrinterFamily.NIIMBOT, false, "B50"),
			def("B50W", PrinterFamily.NIIMBOT, false, "B50W"),
			def("BETTY", PrinterFamily.NIIMBOT, false, "BETTY"),
			def("C1", PrinterFamily.NIIMBOT, false, "C1"),
			def("D101", PrinterFamily.NIIMBOT, false, "D101"),
			def("D11", PrinterFamily.NIIMBOT, false, "D11"),
			def("D11_H", PrinterFamily.NIIMBOT, true, "D11_H"),
			def("D11_PRO", PrinterFamily.NIIMBOT, false, "D11_PRO"),
			def("D110", PrinterFamily.NIIMBOT, false, "D110"),
			def("D110_M", PrinterFamily.NIIMBOT, false, "D110_M"),
			def("D11S", PrinterFamily.NIIMBOT, false, "D11S"),
			def("EP1C", PrinterFamily.NIIMBOT, false, "EP1C"),
			def("EP2M_H", PrinterFamily.NIIMBOT, false, "EP2M_H"),
			def("EP3M", PrinterFamily.NIIMBOT, false, "EP3M"),
			def("ET10", PrinterFamily.NIIMBOT, false, "ET10"),
			def("FUST", PrinterFamily.NIIMBOT, false, "FUST"),
			def("H1", PrinterFamily.NIIMBOT, false, "H1"),
			def("H1S", PrinterFamily.NIIMBOT, false, "H1S"),
			def("HI_D110", PrinterFamily.NIIMBOT, false, "HI_D110"),
			def("HI_NB_D11", PrinterFamily.NIIMBOT, false, "HI_NB_D11"),
			def("JC_M90", PrinterFamily.NIIMBOT, false, "JC_M90"),
			def("JCB3S", PrinterFamily.NIIMBOT, false, "JCB3S"),
			def("K2", PrinterFamily.NIIMBOT, false, "K2"),
			def("K3", PrinterFamily.NIIMBOT, false, "K3"),
			def("K3_ITD", PrinterFamily.NIIMBOT, false, "K3_ITD"),
			def("K3_W", PrinterFamily.NIIMBOT, false, "K3_W"),
			def("K4", PrinterFamily.NIIMBOT, false, "K4"),
			def("M2_H", PrinterFamily.NIIMBOT, true, "M2_H"),
			def("M3", PrinterFamily.NIIMBOT, false, "M3"),
			def("MP3K", PrinterFamily.NIIMBOT, false, "MP3K"),
			def("MP3K_W", PrinterFamily.NIIMBOT, false, "MP3K_W"),
			def("N1", PrinterFamily.NIIMBOT, false, "N1"),
			def("P1", PrinterFamily.NIIMBOT, false, "P1"),
			def("P18", PrinterFamily.NIIMBOT, false, "P18"),
			def("P1S", PrinterFamily.NIIMBOT, false, "P1S"),
			def("S1", PrinterFamily.NIIMBOT, false, "S1"),
			def("S3", PrinterFamily.NIIMBOT, false, "S3"),
			def("S6", PrinterFamily.NIIMBOT, false, "S6"),
			def("S6_P", PrinterFamily.NIIMBOT, false, "S6_P"),
			def("T2S", PrinterFamily.NIIMBOT, false, "T2S"),
			def("T6", PrinterFamily.NIIMBOT, false, "T6"),
			def("T7", PrinterFamily.NIIMBOT, false, "T7"),
			def("T8", PrinterFamily.NIIMBOT, false, "T8"),
			def("T8S", PrinterFamily.NIIMBOT, false, "T8S"),
			def("TP2M_H", PrinterFamily.NIIMBOT, false, "TP2M_H"),
			def("Z401", PrinterFamily.NIIMBOT, false, "Z401"),

			// Phomemo d-series (cz.bliksoft.ptlabelprint.protocol.phomemo). Name prefixes copied
			// from phomymo's own D_SERIES entry in printers.json - deliberately excluding its bare
			// "D" wildcard, which would collide with every Niimbot D-series entry above (confirmed:
			// phomymo's own list is ["D30","D35","D50","Q30S","Q30","D"] - the "D" is Phomemo-app
			// specific fallback behavior, not a real per-model prefix, and not safe to reuse here).
			def("phomemo-d30", PrinterFamily.PHOMEMO_D_SERIES, false, "D30"),
			def("phomemo-d35", PrinterFamily.PHOMEMO_D_SERIES, false, "D35"),
			def("phomemo-d50", PrinterFamily.PHOMEMO_D_SERIES, false, "D50"),
			def("phomemo-q30", PrinterFamily.PHOMEMO_D_SERIES, true, "Q30"),
			def("phomemo-q30s", PrinterFamily.PHOMEMO_D_SERIES, false, "Q30S"),

			// Phomemo's 6 other cataloged-but-not-yet-implemented sub-protocols (see PrinterFamily's
			// own javadoc for why these families exist despite having no LabelPrinter implementation,
			// and PhomemoPrinterModels for the full per-model specs). Name prefixes copied verbatim
			// from phomymo's own printers.json namePatterns. 3 of that file's 17 non-d-series models
			// (M04S_80, M04S_110, M110S) ship with an EMPTY namePatterns list there too - manual-
			// selection-only, same physical hardware as a sibling entry below - so they deliberately
			// have no entry here; see PhomemoPrinterModels' javadoc for how to construct one by hand.
			def("phomemo-p12", PrinterFamily.PHOMEMO_P12, false, "P12 PRO", "P12PRO", "P12"),
			def("phomemo-a30", PrinterFamily.PHOMEMO_P12, false, "A30"),
			def("phomemo-m02", PrinterFamily.PHOMEMO_M02, false, "M02X", "M02S", "M02"),
			def("phomemo-m02-pro", PrinterFamily.PHOMEMO_M02, false, "M02 PRO", "M02PRO"),
			def("phomemo-m03", PrinterFamily.PHOMEMO_M_SERIES, false, "M03"),
			def("phomemo-t02", PrinterFamily.PHOMEMO_M_SERIES, false, "T02"),
			def("phomemo-m200", PrinterFamily.PHOMEMO_M_SERIES, false, "M200"),
			def("phomemo-m250", PrinterFamily.PHOMEMO_M_SERIES, false, "M250"),
			def("phomemo-m220", PrinterFamily.PHOMEMO_M_SERIES, false, "M220"),
			def("phomemo-m221", PrinterFamily.PHOMEMO_M_SERIES, false, "M221"),
			def("phomemo-m260", PrinterFamily.PHOMEMO_M_SERIES, false, "M260"),
			def("phomemo-m04s-53", PrinterFamily.PHOMEMO_M04, false, "M04A", "M04"),
			def("phomemo-m110", PrinterFamily.PHOMEMO_M110, false, "M110", "M120"),
			def("phomemo-pm241", PrinterFamily.PHOMEMO_TSPL, false, "PM-241", "PM241", "PM 241")));

	private PrinterCatalog() {
	}

	private static PrinterDefinition def(String id, PrinterFamily family, boolean confirmed, String... prefixes) {
		return new PrinterDefinition(id, family, Arrays.asList(prefixes), confirmed);
	}

	public static List<PrinterDefinition> all() {
		return ENTRIES;
	}

	/**
	 * Every entry tied for the longest name prefix matching {@code bleAdvertisedName}
	 * (case-insensitive), or empty if none match. See class javadoc for why this can return more
	 * than one entry, and why a match here is a heuristic, not a guarantee.
	 */
	public static List<PrinterDefinition> detect(String bleAdvertisedName) {
		if (bleAdvertisedName == null || bleAdvertisedName.isEmpty()) {
			return Collections.emptyList();
		}

		String name = bleAdvertisedName.toUpperCase(Locale.ROOT);
		int bestLength = -1;
		List<PrinterDefinition> best = new ArrayList<>();

		for (PrinterDefinition entry : ENTRIES) {
			for (String prefix : entry.getNamePrefixes()) {
				if (name.startsWith(prefix.toUpperCase(Locale.ROOT))) {
					int len = prefix.length();
					if (len > bestLength) {
						bestLength = len;
						best.clear();
						best.add(entry);
					} else if (len == bestLength && !best.contains(entry)) {
						best.add(entry);
					}
				}
			}
		}

		return best;
	}

	/** {@link #detect(String)}, but only when it resolves to exactly one entry - use when an ambiguous or absent match should just mean "unknown" rather than needing separate handling. */
	public static Optional<PrinterDefinition> detectUnambiguous(String bleAdvertisedName) {
		List<PrinterDefinition> matches = detect(bleAdvertisedName);
		return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
	}
}
