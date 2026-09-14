package cz.bliksoft.ptlabelprint.protocol;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import cz.bliksoft.javautils.ble.BleCharacteristic;
import cz.bliksoft.javautils.ble.BleException;
import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.javautils.ble.BleService;
import cz.bliksoft.javautils.ble.ScanFilter;

/**
 * BLE transport built on BSToolbox-BLE, shared across protocol families (moved here from
 * {@code protocol.niimbot} once {@code protocol.phomemo} needed it too - the channel-discovery
 * logic below isn't brand- or protocol-specific). Ported from niimbluelib's
 * {@code NiimbotBluetoothClient} (src/client/bluetooth_impl.ts): discover services and use
 * whichever characteristic supports both {@code NOTIFY} and {@code WRITE_WITHOUT_RESPONSE} for
 * both directions - this is how Niimbot-branded printers expose their channel.
 *
 * <p>
 * <b>Confirmed on real hardware this doesn't cover every printer</b>: a Phomemo Q30 instead splits
 * the channel into two separate characteristics under one service (observed: service
 * {@code 0000ff00-...}, write+write-without-response on {@code 0000ff02-...}, notify on
 * {@code 0000ff03-...} - a common vendor-specific BLE UART pattern, unrelated to Niimbot's own
 * service UUID below, which the Q30 doesn't advertise or expose at all, and confirmed to match
 * phomymo's own {@code BLE.SERVICE_UUID}/{@code WRITE_CHAR_UUID}/{@code NOTIFY_CHAR_UUID}
 * constants for its Phomemo `d-series` protocol). This class therefore falls back to pairing the
 * first NOTIFY-capable and first WRITE(_WITHOUT_RESPONSE)-capable characteristics within one
 * service when no single characteristic offers both - use the CLI's {@code gatt} command to
 * inspect an unfamiliar device's actual GATT layout before assuming either shape.
 *
 * <p>
 * The caller is responsible for discovering the {@link BlePeripheral} via a prior
 * {@code BleAdapter.scan()} - {@link BlePeripheral} itself only supports {@code connect()} on an
 * already-discovered address, per BSToolbox-BLE's own contract. {@link #scanFilter()} matches
 * Niimbot's own advertised service UUID specifically (useful for discovering Niimbot-branded
 * printers, not for Phomemo or other families - per the Q30 finding above, use the CLI's
 * unfiltered {@code scan} or a name-based filter for those instead).
 */
public class BleTransport implements Transport {

	/** Niimbot's own advertised GATT service UUID - narrows discovery for genuine Niimbot units. Not useful for other protocol families (see class javadoc). */
	public static final String SERVICE_UUID = "e7810a71-73ae-499d-8c15-faa9aef0c3f2";

	private final BlePeripheral peripheral;
	private String channelServiceUuid;
	private String writeCharUuid;
	private String notifyCharUuid;
	private volatile Consumer<byte[]> rawDataListener;
	private volatile boolean connected;

	public BleTransport(BlePeripheral peripheral) {
		this.peripheral = peripheral;
	}

	/** A scan filter matching Niimbot's own advertised GATT service, for use with {@code BleAdapter.scan()}/{@code BleUtils}. */
	public static ScanFilter scanFilter() {
		return new ScanFilter().withServiceUuid(SERVICE_UUID);
	}

	@Override
	public void connect() throws IOException {
		try {
			peripheral.connect();

			List<BleService> services = peripheral.discoverServices();
			findChannel(services);

			if (writeCharUuid == null || notifyCharUuid == null) {
				peripheral.disconnect();
				throw new IOException(
						"Suitable device characteristic(s) not found (need NOTIFY and WRITE/WRITE_WITHOUT_RESPONSE, "
								+ "either combined on one characteristic or split across two in the same service - "
								+ "use the CLI's 'gatt' command to inspect this device)");
			}

			peripheral.setDisconnectListener(reason -> connected = false);
			peripheral.subscribe(channelServiceUuid, notifyCharUuid, (charUuid, value) -> {
				Consumer<byte[]> listener = rawDataListener;
				if (listener != null) {
					listener.accept(value);
				}
			});

			connected = true;
		} catch (BleException e) {
			throw new IOException("BLE connect failed: " + e.getMessage(), e);
		}
	}

	/**
	 * First pass: a genuine Niimbot-style single characteristic with both NOTIFY and
	 * WRITE_WITHOUT_RESPONSE. Second pass (only if the first finds nothing): the first
	 * NOTIFY-capable and first WRITE(_WITHOUT_RESPONSE)-capable characteristics within one
	 * service, for split-channel devices like the Phomemo Q30 (see class javadoc).
	 */
	private void findChannel(List<BleService> services) {
		for (BleService service : services) {
			for (BleCharacteristic c : service.getCharacteristics()) {
				if (c.getProperties().contains("NOTIFY") && c.getProperties().contains("WRITE_WITHOUT_RESPONSE")) {
					channelServiceUuid = service.getUuid();
					writeCharUuid = c.getUuid();
					notifyCharUuid = c.getUuid();
					return;
				}
			}
		}

		for (BleService service : services) {
			BleCharacteristic write = null;
			BleCharacteristic notify = null;

			for (BleCharacteristic c : service.getCharacteristics()) {
				List<String> props = c.getProperties();
				if (notify == null && props.contains("NOTIFY")) {
					notify = c;
				}
				if (write == null && (props.contains("WRITE_WITHOUT_RESPONSE") || props.contains("WRITE"))) {
					write = c;
				}
			}

			if (write != null && notify != null) {
				channelServiceUuid = service.getUuid();
				writeCharUuid = write.getUuid();
				notifyCharUuid = notify.getUuid();
				return;
			}
		}
	}

	@Override
	public void disconnect() throws IOException {
		try {
			if (peripheral.isConnected()) {
				peripheral.disconnect();
			}
		} catch (BleException e) {
			throw new IOException("BLE disconnect failed: " + e.getMessage(), e);
		} finally {
			connected = false;
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void write(byte[] data) throws IOException {
		try {
			peripheral.writeCharacteristic(channelServiceUuid, writeCharUuid, data, false);
		} catch (BleException e) {
			throw new IOException("BLE write failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void setRawDataListener(Consumer<byte[]> listener) {
		this.rawDataListener = listener;
	}
}
