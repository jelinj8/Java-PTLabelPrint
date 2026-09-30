package cz.bliksoft.ptlabelprint.printer;

import java.io.IOException;

import cz.bliksoft.ptlabelprint.protocol.Transport;

/**
 * Shared plumbing for {@link LabelPrinter}s across Phomemo's protocol tags. Every Phomemo
 * sub-protocol - implemented ({@code d-series}, {@link PhomemoDSeriesLabelPrinter}; {@code m110},
 * {@link PhomemoM110LabelPrinter}) or merely cataloged ({@code m02}/{@code m04}/generic
 * {@code m-series}/{@code p12}/{@code tspl}, see {@link PrinterFamily}) - shares the same BLE
 * transport shape confirmed for {@code d-series} and {@code m110}: no connect handshake and a print
 * flow that never waits on a reply, and the same UUID-fallback discovery
 * {@link cz.bliksoft.ptlabelprint.protocol.BleTransport} already does generically, not a
 * protocol-specific channel. So {@link #connect()}/{@link #close()}/{@link #isConnected()} are the
 * same concrete body for all of them, and only this class - not each sub-protocol individually -
 * needs to know that.
 *
 * <p>
 * Takes a {@link Transport}, not a BLE-specific type, at construction - built by the caller from
 * whichever {@code BlePeripheral} (local- or, once BSToolbox-BLE ships it, remote-backed) or future
 * transport (Serial, ...) they have; see {@link PrinterFactory}'s own javadoc.
 *
 * <p>
 * Has two concrete subclasses ({@link PhomemoDSeriesLabelPrinter}, {@link PhomemoM110LabelPrinter}).
 * The still-unimplemented families have no {@link LabelPrinter} subclass at all, per
 * {@link PrinterFactory}'s {@link UnimplementedPrinterFamilyException}.
 */
public abstract class PhomemoLabelPrinter extends AbstractLabelPrinter {

	protected final Transport transport;

	protected PhomemoLabelPrinter(PrinterDefinition definition, PrinterFamily expectedFamily, Transport transport) {
		super(definition, expectedFamily);
		this.transport = transport;
	}

	@Override
	public void connect() throws IOException {
		transport.connect();
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
