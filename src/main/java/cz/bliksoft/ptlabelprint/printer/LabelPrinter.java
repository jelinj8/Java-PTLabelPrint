package cz.bliksoft.ptlabelprint.printer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Common connection lifecycle across protocol families. This is deliberately <b>not</b> a unified
 * "print an image" API: {@code protocol.niimbot} and {@code protocol.phomemo} have genuinely
 * different image representations, options, and capabilities (e.g. Niimbot exposes rich printer
 * info - model/battery/serial/RFID - that Phomemo's fire-and-forget {@code d-series} protocol has
 * no equivalent for at all), so forcing one print signature across them would either lose real
 * capability or paper over real differences with a misleading lowest-common-denominator API. See
 * {@link NiimbotLabelPrinter}/{@link PhomemoDSeriesLabelPrinter} for each family's own, real,
 * typed API - use {@link #getDefinition()} (or an {@code instanceof} check) to get to it after
 * connecting.
 */
public interface LabelPrinter extends AutoCloseable {

	PrinterDefinition getDefinition();

	void connect() throws IOException, TimeoutException;

	@Override
	void close() throws IOException;

	boolean isConnected();
}
