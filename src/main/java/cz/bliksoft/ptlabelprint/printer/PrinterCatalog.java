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
			// for D11_H ("D11_H-G412010570") and M2_H ("M2_H-I814050044"); the others are ported into
			// PrinterModels.java but their exact advertised-name format hasn't been checked against
			// real units.
			def("D101", PrinterFamily.NIIMBOT, false, "D101"),
			def("D11", PrinterFamily.NIIMBOT, false, "D11"),
			def("D11_H", PrinterFamily.NIIMBOT, true, "D11_H"),
			def("D11_PRO", PrinterFamily.NIIMBOT, false, "D11_PRO"),
			def("D110", PrinterFamily.NIIMBOT, false, "D110"),
			def("D110_M", PrinterFamily.NIIMBOT, false, "D110_M"),
			def("D11S", PrinterFamily.NIIMBOT, false, "D11S"),
			def("HI_D110", PrinterFamily.NIIMBOT, false, "HI_D110"),
			def("HI_NB_D11", PrinterFamily.NIIMBOT, false, "HI_NB_D11"),
			def("M2_H", PrinterFamily.NIIMBOT, true, "M2_H"),

			// Phomemo d-series (cz.bliksoft.ptlabelprint.protocol.phomemo). Name prefixes copied
			// from phomymo's own D_SERIES entry in printers.json - deliberately excluding its bare
			// "D" wildcard, which would collide with every Niimbot D-series entry above (confirmed:
			// phomymo's own list is ["D30","D35","D50","Q30S","Q30","D"] - the "D" is Phomemo-app
			// specific fallback behavior, not a real per-model prefix, and not safe to reuse here).
			def("phomemo-d30", PrinterFamily.PHOMEMO_D_SERIES, false, "D30"),
			def("phomemo-d35", PrinterFamily.PHOMEMO_D_SERIES, false, "D35"),
			def("phomemo-d50", PrinterFamily.PHOMEMO_D_SERIES, false, "D50"),
			def("phomemo-q30", PrinterFamily.PHOMEMO_D_SERIES, true, "Q30"),
			def("phomemo-q30s", PrinterFamily.PHOMEMO_D_SERIES, false, "Q30S")));

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
