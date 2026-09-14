package cz.bliksoft.ptlabelprint.printer;

import cz.bliksoft.javautils.ble.BlePeripheral;

/** Constructs the right concrete {@link LabelPrinter} for a {@link PrinterDefinition}. */
public final class PrinterFactory {

	private PrinterFactory() {
	}

	/**
	 * @throws UnimplementedPrinterFamilyException if {@code definition}'s family is a real,
	 *         cataloged family with no {@link LabelPrinter} implementation yet (currently the 6
	 *         {@code PHOMEMO_*} families other than {@link PrinterFamily#PHOMEMO_D_SERIES} - see
	 *         {@link PrinterFamily}'s own javadoc)
	 */
	public static LabelPrinter create(PrinterDefinition definition, BlePeripheral peripheral) {
		switch (definition.getFamily()) {
		case NIIMBOT:
			return new NiimbotLabelPrinter(definition, peripheral);
		case PHOMEMO_D_SERIES:
			return new PhomemoDSeriesLabelPrinter(definition, peripheral);
		case PHOMEMO_M02:
		case PHOMEMO_M04:
		case PHOMEMO_M110:
		case PHOMEMO_M_SERIES:
		case PHOMEMO_P12:
		case PHOMEMO_TSPL:
			throw new UnimplementedPrinterFamilyException(definition);
		default:
			// Unreachable today - every PrinterFamily constant is handled above. Kept as a
			// defensive fallback for a future enum constant added without a matching case here.
			throw new IllegalArgumentException("No LabelPrinter implementation for " + definition.getFamily());
		}
	}
}
