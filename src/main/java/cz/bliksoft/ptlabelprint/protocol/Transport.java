package cz.bliksoft.ptlabelprint.protocol;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A byte-level transport a protocol family's device/print API can send/receive raw bytes over -
 * shared across families (unlike framing/commands, which are family-specific). Only
 * {@link BleTransport} exists so far; a Serial implementation (for serial-capable printers, per
 * the project brief) is not implemented yet but would sit alongside it here.
 *
 * <p>
 * Moved here from {@code protocol.niimbot} once a second family ({@code protocol.phomemo}) needed
 * it too - see {@code cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice} for a
 * request/response consumer of this interface, and {@code protocol.phomemo}'s d-series printer for
 * a fire-and-forget one (no response correlation at all).
 */
public interface Transport {

	/** Open the transport (e.g. BLE GATT connect + subscribe to notifications). */
	void connect() throws IOException;

	void disconnect() throws IOException;

	boolean isConnected();

	/** Write raw bytes to the printer (a full framed packet, or a plain command/data chunk, depending on the protocol family). */
	void write(byte[] data) throws IOException;

	/**
	 * Register the callback invoked with raw incoming bytes as they arrive (not necessarily
	 * one full logical message per call - the caller handles reassembly if its protocol needs
	 * it). Replaces any previously set listener. A fire-and-forget protocol family may simply
	 * never set one.
	 */
	void setRawDataListener(Consumer<byte[]> listener);
}
