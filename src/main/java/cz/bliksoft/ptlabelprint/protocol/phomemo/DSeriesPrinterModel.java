package cz.bliksoft.ptlabelprint.protocol.phomemo;

/**
 * The 5 `d-series` models this project's {@code printer.PrinterCatalog} already detects by BLE
 * name - matches its 5 existing entries exactly. See {@link DSeriesPrinterModels} for why this
 * table exists and what it can (and can't) responsibly claim about them.
 */
public enum DSeriesPrinterModel {
	D30,
	D35,
	D50,
	Q30,
	Q30S
}
