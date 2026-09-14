package cz.bliksoft.ptlabelprint.printer;

import java.io.IOException;
import java.util.function.IntConsumer;

import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.ptlabelprint.protocol.BleTransport;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#PHOMEMO_D_SERIES}. Thinner than
 * {@link NiimbotLabelPrinter} since this protocol family has no connect-handshake/info-query
 * concept at all (see {@link DSeriesPrinter}'s javadoc) - {@link #connect()} only opens the BLE
 * channel, and {@link #print} is the real capability, a direct pass-through to
 * {@link DSeriesPrinter#print}.
 */
public class PhomemoDSeriesLabelPrinter implements LabelPrinter {

	private final PrinterDefinition definition;
	private final BleTransport transport;

	public PhomemoDSeriesLabelPrinter(PrinterDefinition definition, BlePeripheral peripheral) {
		if (definition.getFamily() != PrinterFamily.PHOMEMO_D_SERIES) {
			throw new IllegalArgumentException("Definition " + definition + " is not a PHOMEMO_D_SERIES printer");
		}
		this.definition = definition;
		this.transport = new BleTransport(peripheral);
	}

	@Override
	public PrinterDefinition getDefinition() {
		return definition;
	}

	@Override
	public void connect() throws IOException {
		transport.connect();
	}

	/** See {@link DSeriesPrinter#print} - the image height constraint documented there still applies. */
	public void print(RasterImage image, int density, boolean continuous, int feedDots, IntConsumer onProgress) throws IOException {
		DSeriesPrinter.print(transport, image, density, continuous, feedDots, onProgress);
	}

	@Override
	public void close() throws IOException {
		transport.disconnect();
	}

	@Override
	public boolean isConnected() {
		return transport.isConnected();
	}
}
