package cz.bliksoft.ptlabelprint.printer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.ptlabelprint.protocol.BleTransport;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterInfo;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#NIIMBOT}. Thin wrapper around
 * {@link NiimbotDevice} - {@link #getDevice()} is the real, full-capability API (print flow,
 * heartbeat, RFID, etc.); this class only adds the family/definition bookkeeping the abstraction
 * layer needs.
 */
public class NiimbotLabelPrinter implements LabelPrinter {

	private final PrinterDefinition definition;
	private final NiimbotDevice device;

	public NiimbotLabelPrinter(PrinterDefinition definition, BlePeripheral peripheral) {
		if (definition.getFamily() != PrinterFamily.NIIMBOT) {
			throw new IllegalArgumentException("Definition " + definition + " is not a NIIMBOT printer");
		}
		this.definition = definition;
		this.device = new NiimbotDevice(new BleTransport(peripheral));
	}

	@Override
	public PrinterDefinition getDefinition() {
		return definition;
	}

	/** The full Niimbot device API - connect handshake result, printer info, print flow, etc. */
	public NiimbotDevice getDevice() {
		return device;
	}

	@Override
	public void connect() throws IOException, TimeoutException {
		device.connect();
	}

	/** Convenience for {@code getDevice().getPrinterInfo()}, valid after {@link #connect()}. */
	public PrinterInfo getPrinterInfo() {
		return device.getPrinterInfo();
	}

	@Override
	public void close() throws IOException {
		device.disconnect();
	}

	@Override
	public boolean isConnected() {
		return device.isConnected();
	}
}
