/**
 * The Niimbot protocol family: packet framing/CRC, the command catalog, and per-model device
 * configmaps - ported from <a href="https://github.com/MultiMote/niimbluelib">niimbluelib</a>
 * (MIT). Covers Niimbot-branded printers (D11_H, M2) only.
 *
 * <p>
 * <b>Not Phomemo's D/Q-series</b>: an earlier version of this project assumed Phomemo's
 * D30/D35/D50/D110/Q30/Q30S spoke this same protocol, reading too much into
 * <a href="https://github.com/transcriptionstream/phomymo">phomymo</a>'s README grouping them as
 * "similar to D30." Confirmed against real Q30 hardware (see the project CLAUDE.md's "Status"
 * section) that this is wrong: those printers speak phomymo's own ESC/POS-derived
 * {@code d-series} protocol instead - ported in a separate
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} family, not here.
 *
 * <p>
 * Other protocol families are planned or noted as sibling packages here (see the project
 * CLAUDE.md's "Protocol families" section for details, references, and - for {@code phomemo} -
 * the concrete command bytes already confirmed from phomymo's source):
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} (Phomemo's own families: {@code d-series}
 * confirmed against real Q30 hardware to be needed; {@code m02}/{@code m04}/{@code m110}/generic
 * {@code m-series}/{@code p12}/{@code tspl} not yet tested against hardware),
 * {@code cz.bliksoft.ptlabelprint.protocol.zebra} (Zebra/ZPL, planned - raw ZPL over a
 * serial-like BLE characteristic, no framing to port; generate the ZPL itself with the sibling
 * {@code cz.bliksoft.java:zpl} library rather than reimplementing it here), and
 * {@code cz.bliksoft.ptlabelprint.protocol.brother} (Brother P-touch, planned). Several of the
 * reference sources for the not-yet-implemented families are GPL-3.0 or unlicensed
 * (vivier/phomemo-tools, yaddran/thermal-print, pklaus/brother_ql, furrtek/PTouchHH,
 * cbdevnet/pt1230): usable as references for wire-format facts, but no code may be ported from
 * them into this MIT-licensed project - reimplement from the facts, clean-room, the same way this
 * package treats the (MIT) niimbluelib, and {@code .protocol.phomemo} will treat the (MIT)
 * phomymo, as porting sources rather than a licensing constraint.
 */
package cz.bliksoft.ptlabelprint.protocol.niimbot;
