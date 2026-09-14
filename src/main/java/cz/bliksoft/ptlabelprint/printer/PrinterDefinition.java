package cz.bliksoft.ptlabelprint.printer;

import java.util.Collections;
import java.util.List;

/**
 * A declarative per-model entry: which {@link PrinterFamily} a model speaks, and the BLE
 * advertised-name prefixes that identify it. Mirrors phomymo's {@code printers.json} entries (for
 * the {@code PHOMEMO_D_SERIES} rows, the prefixes are copied directly from its
 * {@code D_CMD}/{@code d-series} entry) and niimbluelib's own per-model naming convention (for the
 * {@code NIIMBOT} rows, each model's advertised name is that model's niimbluelib
 * {@code PrinterModel} enum name as a prefix - confirmed for {@code D11_H} against real hardware:
 * {@code "D11_H-G412010570"}).
 */
public class PrinterDefinition {

	private final String id;
	private final PrinterFamily family;
	private final List<String> namePrefixes;
	private final boolean confirmedOnHardware;

	public PrinterDefinition(String id, PrinterFamily family, List<String> namePrefixes, boolean confirmedOnHardware) {
		this.id = id;
		this.family = family;
		this.namePrefixes = Collections.unmodifiableList(namePrefixes);
		this.confirmedOnHardware = confirmedOnHardware;
	}

	/** A short human identifier, e.g. {@code "D11_H"} or {@code "phomemo-q30"} - not necessarily the exact advertised name. */
	public String getId() {
		return id;
	}

	public PrinterFamily getFamily() {
		return family;
	}

	public List<String> getNamePrefixes() {
		return namePrefixes;
	}

	/** True if this exact entry has been connected to and exercised (info and/or print) on real hardware in this project - see CLAUDE.md's "Status". */
	public boolean isConfirmedOnHardware() {
		return confirmedOnHardware;
	}

	@Override
	public String toString() {
		return id + "(" + family + ")";
	}
}
