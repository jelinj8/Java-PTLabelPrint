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
 *
 * <p>
 * Extends {@link AbstractLabelPrinter} directly, not through an intermediate
 * {@code NiimbotLabelPrinter}-family class the way {@link PhomemoDSeriesLabelPrinter} extends
 * {@link PhomemoLabelPrinter}: there is exactly one concrete Niimbot {@link LabelPrinter} subclass
 * and no foreseeable sibling today. If/when a second one is ever needed, extract an intermediate
 * class following {@link PhomemoLabelPrinter}'s exact pattern rather than duplicating its
 * connect/close/isConnected bodies here first.
 */
public class NiimbotLabelPrinter extends AbstractLabelPrinter {

	private final NiimbotDevice device;

	public NiimbotLabelPrinter(PrinterDefinition definition, BlePeripheral peripheral) {
		super(definition, PrinterFamily.NIIMBOT);
		this.device = new NiimbotDevice(new BleTransport(peripheral));
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
