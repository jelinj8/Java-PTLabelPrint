/**
 * The printer abstraction layer: declarative per-model definitions ({@link
 * cz.bliksoft.ptlabelprint.printer.PrinterDefinition}, registered in {@link
 * cz.bliksoft.ptlabelprint.printer.PrinterCatalog}) identifying which {@code
 * cz.bliksoft.ptlabelprint.protocol.*} family a model speaks, auto-detected from its BLE
 * advertised name - mirrors <a href="https://github.com/transcriptionstream/phomymo">phomymo</a>'s
 * {@code printers.json} + auto-detect approach. {@link cz.bliksoft.ptlabelprint.printer.LabelPrinter}
 * unifies only the connection lifecycle (connect/close/isConnected) across families - deliberately
 * <b>not</b> a unified print API, since the two implemented families' real print capabilities
 * genuinely differ (see {@link cz.bliksoft.ptlabelprint.printer.LabelPrinter}'s own javadoc for
 * why); {@link cz.bliksoft.ptlabelprint.printer.NiimbotLabelPrinter}/{@link
 * cz.bliksoft.ptlabelprint.printer.PhomemoDSeriesLabelPrinter} expose each family's own typed API
 * after connecting.
 *
 * <p>
 * Name-based auto-detection is a heuristic, not authoritative - see {@link
 * cz.bliksoft.ptlabelprint.printer.PrinterCatalog}'s own javadoc, and this project's own history
 * of a Phomemo Q30 initially being mistaken for a Niimbot-protocol device from a reference
 * project's naming/grouping ({@code cz.bliksoft.ptlabelprint.protocol.niimbot}'s package-info has
 * the full account). Only entries for the two device families/models actually implemented and
 * tested against real hardware so far (Niimbot D11_H, Phomemo Q30) - plus the other models each
 * family's protocol code already supports in principle - are in the catalog; see {@link
 * cz.bliksoft.ptlabelprint.printer.PrinterDefinition#isConfirmedOnHardware()}.
 */
package cz.bliksoft.ptlabelprint.printer;
