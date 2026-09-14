/**
 * Phomemo's own protocol families - ported from
 * <a href="https://github.com/transcriptionstream/phomymo">phomymo</a> (MIT), which is treated as
 * a direct porting source here (not just design inspiration) since it's the confirmed, working
 * reference implementation. Only the {@code d-series} sub-protocol (D30/D35/D50/D110/Q30/Q30S) is
 * implemented so far - see {@link cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter}.
 * {@code m02}/{@code m04}/{@code m110}/generic {@code m-series}/{@code p12}/{@code tspl} are not
 * ported yet; port on demand from phomymo's {@code src/web/printer.js}
 * (`M02_CMD`/`M04_CMD`/`M110_CMD`/`CMD`/`P12_CMD`/`TSPL`) when a model in one of those families
 * needs support.
 *
 * <p>
 * <b>Not the same as {@link cz.bliksoft.ptlabelprint.protocol.niimbot}</b>: an earlier version of
 * this project incorrectly assumed Phomemo's D/Q-series spoke the Niimbot protocol. Confirmed
 * against real hardware that they don't - see {@code cz.bliksoft.ptlabelprint.protocol.niimbot}'s
 * package-info for the corrected finding.
 */
package cz.bliksoft.ptlabelprint.protocol.phomemo;
