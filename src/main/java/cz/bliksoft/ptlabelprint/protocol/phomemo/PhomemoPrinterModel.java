package cz.bliksoft.ptlabelprint.protocol.phomemo;

/**
 * Phomemo models cataloged from phomymo's {@code printers.json}, across the 6 protocol tags this
 * project doesn't yet have print-flow code for ({@code p12}, {@code m02}, generic {@code m-series},
 * {@code m04}, {@code m110}, {@code tspl}) - see {@link PhomemoPrinterModels}'s javadoc for exactly
 * what "cataloged, not implemented" means here. The {@code d-series} protocol's 5 models
 * (D30/D35/D50/Q30/Q30S) are <b>not</b> in this enum - they already have full metadata via
 * {@link cz.bliksoft.ptlabelprint.printer.PrinterCatalog}'s existing entries plus
 * {@link DSeriesPrinter}/{@link DSeriesCommands}/{@link RasterImage}'s real implementation; adding
 * a second, inert table for them here would just create two sources of truth for the same models.
 */
public enum PhomemoPrinterModel {
	P12,
	A30,
	M02,
	M02_PRO,
	M03,
	T02,
	M200,
	M250,
	M220,
	M221,
	M260,
	M04S_53,
	M04S_80,
	M04S_110,
	M110,
	M110S,
	PM241
}
