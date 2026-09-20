package cz.bliksoft.ptlabelprint.protocol;

import java.io.IOException;
import java.util.Arrays;
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
	 * Writes {@code data} as successive {@code chunkSize}-byte pieces, sleeping
	 * {@code chunkDelayMs} between writes - for a one-way, ack-free protocol family (e.g.
	 * Phomemo's {@code d-series}) that already sends many same-destination writes back-to-back
	 * with a fixed inter-write delay, exactly like this. The default implementation is a plain
	 * loop over {@link #write}, correct for any transport but paying one round trip per chunk;
	 * {@link BleTransport} overrides this with a single-round-trip version, since that per-chunk
	 * round trip is cheap over a direct local connection but can make a long transfer's total
	 * wall-clock time exceed a downstream peripheral's own real-time expectations once it's
	 * carried over a higher-latency link (this library's own remote-adapter feature, especially
	 * an ESP32-class remote bridge) - confirmed on real hardware: long Phomemo prints silently
	 * failed through such a bridge specifically because of this, even though every individual
	 * write still succeeded.
	 */
	default void writeStream(byte[] data, int chunkSize, long chunkDelayMs) throws IOException {
		for (int offset = 0; offset < data.length; offset += chunkSize) {
			if (offset > 0 && chunkDelayMs > 0) {
				try {
					Thread.sleep(chunkDelayMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted during writeStream", e);
				}
			}
			int len = Math.min(chunkSize, data.length - offset);
			write(Arrays.copyOfRange(data, offset, offset + len));
		}
	}

	/**
	 * Register the callback invoked with raw incoming bytes as they arrive (not necessarily
	 * one full logical message per call - the caller handles reassembly if its protocol needs
	 * it). Replaces any previously set listener. A fire-and-forget protocol family may simply
	 * never set one.
	 */
	void setRawDataListener(Consumer<byte[]> listener);
}
