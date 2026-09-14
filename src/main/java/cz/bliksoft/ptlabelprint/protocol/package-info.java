/**
 * Wire plumbing shared *across* protocol families: {@link cz.bliksoft.ptlabelprint.protocol.Transport}
 * (the generic byte-level connect/write/receive contract) and
 * {@link cz.bliksoft.ptlabelprint.protocol.BleTransport} (its BSToolbox-BLE implementation - GATT
 * channel discovery only, no protocol-specific framing). Moved here from {@code .protocol.niimbot}
 * once {@code .protocol.phomemo} needed the same transport - confirmed on real hardware that
 * BLE-channel discovery (find the NOTIFY/WRITE characteristic(s)) is not brand- or
 * protocol-specific, even though the bytes sent over it very much are.
 *
 * <p>
 * Framing/commands stay in each family's own subpackage
 * ({@link cz.bliksoft.ptlabelprint.protocol.niimbot}, and once created,
 * {@code .protocol.phomemo}/{@code .protocol.zebra}/{@code .protocol.brother}) - this package is
 * only for what's genuinely shared, not a dumping ground for anything protocol-adjacent.
 */
package cz.bliksoft.ptlabelprint.protocol;
