package cz.bliksoft.ptlabelprint.printer;

import cz.bliksoft.javautils.ble.BlePeripheral;

/** Constructs the right concrete {@link LabelPrinter} for a {@link PrinterDefinition}. */
public final class PrinterFactory {

	private PrinterFactory() {
	}

	public static LabelPrinter create(PrinterDefinition definition, BlePeripheral peripheral) {
		switch (definition.getFamily()) {
		case NIIMBOT:
			return new NiimbotLabelPrinter(definition, peripheral);
		case PHOMEMO_D_SERIES:
			return new PhomemoDSeriesLabelPrinter(definition, peripheral);
		default:
			throw new IllegalArgumentException("No LabelPrinter implementation for " + definition.getFamily());
		}
	}
}
