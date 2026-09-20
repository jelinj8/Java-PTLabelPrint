package cz.bliksoft.ptlabelprint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import cz.bliksoft.javautils.ble.BleAdapter;
import cz.bliksoft.javautils.ble.BleCharacteristic;
import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.javautils.ble.BleService;
import cz.bliksoft.javautils.ble.ScanFilter;
import cz.bliksoft.javautils.ble.utils.BleUtils;
import cz.bliksoft.ptlabelprint.image.PixelSource;
import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;
import cz.bliksoft.ptlabelprint.printer.PrintJob;
import cz.bliksoft.ptlabelprint.printer.Rotation;
import cz.bliksoft.ptlabelprint.protocol.BleTransport;
import cz.bliksoft.ptlabelprint.protocol.niimbot.AbstractNiimbotPrintTask;
import cz.bliksoft.ptlabelprint.protocol.niimbot.B1PrintTask;
import cz.bliksoft.ptlabelprint.protocol.niimbot.D110V4PrintTask;
import cz.bliksoft.ptlabelprint.protocol.niimbot.EncodedImage;
import cz.bliksoft.ptlabelprint.protocol.niimbot.HeartbeatData;
import cz.bliksoft.ptlabelprint.protocol.niimbot.LabelType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotImageEncoder;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotPrintTasks;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PageColorType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PaperInfo;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrintOptions;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterInfo;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterModel;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterModelMeta;
import cz.bliksoft.ptlabelprint.protocol.niimbot.RfidInfo;
import cz.bliksoft.ptlabelprint.protocol.niimbot.SoundSettingsItemType;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesLabelSizes;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;
import cz.bliksoft.ptlabelprint.printer.LabelPrinter;
import cz.bliksoft.ptlabelprint.printer.NiimbotLabelPrinter;
import cz.bliksoft.ptlabelprint.printer.PrinterCatalog;
import cz.bliksoft.ptlabelprint.printer.PrinterDefinition;
import cz.bliksoft.ptlabelprint.printer.PrinterFactory;
import cz.bliksoft.ptlabelprint.printer.UnimplementedPrinterFamilyException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command-line front end for PtLabelPrint. {@code scan}/{@code info} exercise
 * {@code protocol.niimbot} specifically; {@code phomemo-print-test} exercises
 * {@code protocol.phomemo}'s {@code d-series}; {@code gatt}/{@code raw} are protocol-agnostic
 * diagnostics for bringing up a new/unfamiliar device's BLE channel by hand. {@code discover} and
 * {@code connect} instead go through the {@code cz.bliksoft.ptlabelprint.printer} abstraction
 * layer - manufacturer-agnostic BLE-name-based family detection plus a unified connect lifecycle
 * (see {@link LabelPrinter}'s own javadoc for why printing itself still isn't unified).
 */
@Command(name = "ptlabelprint-cli", mixinStandardHelpOptions = true, versionProvider = Cli.VersionProvider.class,
		description = "CLI for Niimbot/Phomemo label printers (BLE, optionally Serial/USB) plus protocol-agnostic BLE diagnostics.",
		subcommands = {Cli.ScanCommand.class, Cli.InfoCommand.class, Cli.MediaCommand.class,
				Cli.NiimbotCalibrateCommand.class, Cli.NiimbotSetTimeCommand.class, Cli.NiimbotFirmwareUpgradeCommand.class,
				Cli.RawCommand.class, Cli.GattCommand.class, Cli.PhomemoPrintTestCommand.class,
				Cli.NiimbotPrintTestCommand.class, Cli.DiscoverCommand.class, Cli.ConnectCommand.class,
				Cli.PrintTestCommand.class})
public class Cli implements Runnable {

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Cli()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * Reads the real released version from the jar manifest's {@code Implementation-Version}
	 * entry (populated by maven-jar-plugin's {@code addDefaultImplementationEntries}, see pom.xml)
	 * instead of a hardcoded string that would silently go stale on every release - confirmed to
	 * do exactly that (still said "0.1.0-SNAPSHOT" several releases later). Falls back to a plain
	 * label when run from raw classes (IDE/test execution) rather than a built jar, since there's
	 * no manifest to read in that case.
	 */
	static final class VersionProvider implements CommandLine.IVersionProvider {
		@Override
		public String[] getVersion() {
			String version = Cli.class.getPackage().getImplementationVersion();
			return new String[] { "ptlabelprint " + (version != null ? version : "(development build)") };
		}
	}

	@Override
	public void run() {
		System.out.println("ptlabelprint-cli: pass a subcommand (scan, info) or --help.");
	}

	@Command(name = "scan", description = "Scan for BLE label printers speaking the Niimbot-compatible protocol.")
	static class ScanCommand implements Callable<Integer> {

		@Option(names = {"-t", "--timeout"}, description = "Scan duration in milliseconds (default: ${DEFAULT-VALUE}).")
		long timeoutMs = 5000;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> results = BleUtils.scan(adapter, BleTransport.scanFilter(), timeoutMs);

				if (results.isEmpty()) {
					System.out.println("No devices found.");
					return 0;
				}

				for (BleUtils.BleDeviceResult r : results) {
					System.out.printf("%s\t%s\trssi=%s%n", r.getAddress(), r.getName() != null ? r.getName() : "-",
							r.getRssi() != null ? r.getRssi() : "-");
				}
			}
			return 0;
		}
	}

	@Command(name = "info", description = "Connect to a printer and print its properties.")
	static class InfoCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				// BSToolbox-BLE requires an address to have been seen by scan() on this same
				// adapter before connect() will work on it - see BlePeripheral's javadoc.
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					PrinterInfo info = device.connect();
					System.out.println(info);
					device.getModelMetadata().ifPresent(meta -> System.out.println("Model metadata: " + meta.getModel()));
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}
	}

	@Command(name = "media",
			description = "Connect to a printer and query its live state (lid/paper/RFID-lock) and loaded-roll RFID info.")
	static class MediaCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					device.connect();

					try {
						HeartbeatData heartbeat = device.heartbeat();
						System.out.println("State: lidClosed=" + heartbeat.getLidClosed() + ", paperInserted="
								+ heartbeat.getPaperInserted() + ", paperRfidSuccess=" + heartbeat.getPaperRfidSuccess()
								+ ", ribbonInserted=" + heartbeat.getRibbonInserted() + ", ribbonRfidSuccess="
								+ heartbeat.getRibbonRfidSuccess() + ", batteryPercents=" + heartbeat.getBatteryPercents()
								+ ", temp=" + heartbeat.getTemp());
					} catch (Exception e) {
						System.out.println("State: heartbeat query failed: " + e);
					}

					try {
						RfidInfo paper = device.rfidInfo();
						System.out.println("Loaded media (paper RFID): " + paper);
					} catch (Exception e) {
						System.out.println("Loaded media (paper RFID): query failed: " + e);
					}

					try {
						RfidInfo ribbon = device.rfidInfo2();
						System.out.println("Loaded media (ribbon RFID): " + ribbon);
					} catch (Exception e) {
						System.out.println("Loaded media (ribbon RFID): query failed: " + e);
					}

					try {
						PaperInfo paper = device.getPaperInfo();
						System.out.println("Loaded media (paper geometry): " + paper);
					} catch (Exception e) {
						System.out.println("Loaded media (paper geometry): query failed: " + e);
					}

					try {
						boolean bluetoothSound = device.isSoundEnabled(SoundSettingsItemType.BLUETOOTH_CONNECTION_SOUND);
						boolean powerSound = device.isSoundEnabled(SoundSettingsItemType.POWER_SOUND);
						System.out.println("Sound: bluetoothConnectionSound=" + bluetoothSound + ", powerSound=" + powerSound);
					} catch (Exception e) {
						System.out.println("Sound: query failed: " + e);
					}
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}
	}

	@Command(name = "niimbot-calibrate",
			description = "Run the printer's own label positioning calibration. USES REAL CONSUMABLES (niimbluelib's own note: ejects ~15cm of paper).")
	static class NiimbotCalibrateCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = "--value", description = "Calibration value to send (default: ${DEFAULT-VALUE}).")
		int value = 1;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					device.connect();
					boolean ok = device.labelPositioningCalibration(value);
					System.out.println("Calibration " + (ok ? "accepted" : "refused") + " (value=" + value + ").");
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}
	}

	@Command(name = "niimbot-set-time", description = "Set the printer's real-time clock.")
	static class NiimbotSetTimeCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					device.connect();
					java.time.LocalDateTime now = java.time.LocalDateTime.now();
					device.setPrinterTime(now);
					System.out.println("Printer time set to " + now + ".");
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}
	}

	/**
	 * <b>HIGH RISK - see {@link NiimbotDevice#firmwareUpgrade}'s javadoc.</b> An interrupted or
	 * corrupt upload, or firmware for the wrong model, can permanently brick the printer - unlike
	 * wasted consumables, this is not a reversible mistake. This port has never been exercised
	 * against real hardware; there is no validated firmware file in this project to test with.
	 * Requires {@code --confirm-firmware-risk} so it can't be triggered by an accidental invocation.
	 */
	@Command(name = "niimbot-firmware-upgrade",
			description = "Upload new firmware to a Niimbot-branded printer. HIGH RISK: can permanently brick the "
					+ "printer if interrupted, given a corrupt image, or given firmware for the wrong model. This "
					+ "port has never been tested against real hardware - use entirely at your own risk.")
	static class NiimbotFirmwareUpgradeCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Parameters(index = "1", description = "Path to the firmware image file.")
		String firmwareFile;

		@Parameters(index = "2", description = "Firmware version string in \"x.x\" form, sent with the upgrade request.")
		String version;

		@Option(names = "--confirm-firmware-risk", required = true,
				description = "Required. Confirms you understand this can permanently brick the printer and that this "
						+ "code has never been tested against real hardware.")
		boolean confirmed;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			byte[] firmwareData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(firmwareFile));
			System.out.println("Loaded " + firmwareData.length + " bytes from " + firmwareFile + ".");

			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					device.connect();
					System.out.println("Starting firmware upgrade - do not disconnect or power off the printer.");
					device.firmwareUpgrade(firmwareData, version,
							chunkIndex -> System.out.println("Sent chunk " + chunkIndex));
					System.out.println("Firmware upgrade completed.");
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}
	}

	/**
	 * Diagnostic command for protocol bring-up: connects directly via {@link BlePeripheral} (no
	 * {@link NiimbotPacket} framing, no {@link BleTransport} channel-discovery assumptions unless
	 * defaults are used), writes exactly the given hex bytes to an explicit or auto-discovered
	 * characteristic, and prints whatever arrives on an explicit or auto-discovered notify
	 * characteristic. For testing protocol hypotheses by hand - e.g. whether a clone needs the
	 * {@code 0x03} connect-prefix quirk niimbluelib reverse-engineered from the official Niimbot
	 * app, or whether it needs a WRITE (with response) instead of WRITE_WITHOUT_RESPONSE despite
	 * advertising both.
	 */
	@Command(name = "raw", description = "Write raw hex bytes to a BLE characteristic and print whatever comes back.")
	static class RawCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Parameters(index = "1", description = "Hex bytes to write, e.g. 5555c10101c1aaaa (whitespace ignored).")
		String hex;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-l", "--listen"}, description = "How long to listen for a response, in ms (default: ${DEFAULT-VALUE}).")
		long listenMs = 3000;

		@Option(names = "--service", description = "GATT service UUID (default: auto-discover the same way BleTransport does).")
		String serviceUuid;

		@Option(names = "--write-char", description = "Characteristic UUID to write to (default: auto-discovered).")
		String writeCharUuid;

		@Option(names = "--notify-char", description = "Characteristic UUID to subscribe to (default: auto-discovered).")
		String notifyCharUuid;

		@Option(names = "--with-response", description = "Use WRITE (with response) instead of WRITE_WITHOUT_RESPONSE.")
		boolean withResponse;

		@Option(names = "--read", description = "After writing and listening, READ this characteristic UUID and print its value "
				+ "(tests whether the response is synchronous-read rather than pushed via NOTIFY).")
		String readCharUuid;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				peripheral.connect();

				try {
					String svc = serviceUuid;
					String writeChar = writeCharUuid;
					String notifyChar = notifyCharUuid;

					if (svc == null || writeChar == null || notifyChar == null) {
						outer:
						for (BleService service : peripheral.discoverServices()) {
							BleCharacteristic w = null;
							BleCharacteristic n = null;
							for (BleCharacteristic c : service.getCharacteristics()) {
								if (n == null && c.getProperties().contains("NOTIFY")) {
									n = c;
								}
								if (w == null && (c.getProperties().contains("WRITE_WITHOUT_RESPONSE") || c.getProperties().contains("WRITE"))) {
									w = c;
								}
							}
							if (w != null && n != null) {
								svc = service.getUuid();
								writeChar = w.getUuid();
								notifyChar = n.getUuid();
								break outer;
							}
						}
					}

					if (svc == null || writeChar == null || notifyChar == null) {
						System.err.println("Could not auto-discover a write+notify characteristic pair; pass --service/--write-char/--notify-char explicitly (see 'gatt').");
						return 1;
					}

					System.out.println("Using service=" + svc + " write=" + writeChar + " notify=" + notifyChar
							+ " withResponse=" + withResponse);

					peripheral.subscribe(svc, notifyChar, (charUuid, value) -> {
						StringBuilder sb = new StringBuilder();
						for (byte b : value) {
							sb.append(String.format("%02x ", b & 0xff));
						}
						System.out.println("RX " + sb.toString().trim());
					});

					byte[] bytes = hexToBytes(hex);
					System.out.println("TX " + hex.replaceAll("\\s", ""));
					peripheral.writeCharacteristic(svc, writeChar, bytes, withResponse);
					Thread.sleep(listenMs);

					if (readCharUuid != null) {
						byte[] value = peripheral.readCharacteristic(svc, readCharUuid);
						StringBuilder sb = new StringBuilder();
						for (byte b : value) {
							sb.append(String.format("%02x ", b & 0xff));
						}
						System.out.println("READ " + readCharUuid + " = " + sb.toString().trim());
					}
				} finally {
					peripheral.disconnect();
				}
			}
			return 0;
		}

		private static byte[] hexToBytes(String hex) {
			String clean = hex.replaceAll("\\s", "");
			byte[] out = new byte[clean.length() / 2];
			for (int i = 0; i < out.length; i++) {
				out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
			}
			return out;
		}
	}

	/**
	 * Diagnostic command for bringing up a new device: dumps every GATT service/characteristic and
	 * its properties, independent of any protocol assumptions. Useful when
	 * {@link BleTransport}'s "single NOTIFY+WRITE_WITHOUT_RESPONSE characteristic" search
	 * (which matches Niimbot-branded printers) doesn't find anything on a given device - e.g. a
	 * clone that splits TX/RX across two characteristics.
	 */
	@Command(name = "gatt", description = "Connect to a BLE device and dump its GATT services/characteristics (protocol-agnostic).")
	static class GattCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the device (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				peripheral.connect();

				try {
					for (BleService service : peripheral.discoverServices()) {
						System.out.println("Service " + service.getUuid());
						for (BleCharacteristic c : service.getCharacteristics()) {
							System.out.println("  Characteristic " + c.getUuid() + " " + c.getProperties());
						}
					}
				} finally {
					peripheral.disconnect();
				}
			}
			return 0;
		}
	}

	/**
	 * Prints a small validation test pattern via {@link DSeriesPrinter} (Phomemo's {@code d-series}
	 * protocol - D30/D35/D50/Q30/Q30S, all confirmed by phomymo's own current {@code printers.json}
	 * to share this exact protocol and BLE channel; only the Q30 itself is hardware-confirmed - see
	 * CLAUDE.md). Uses real consumables - defaults to the smallest of phomymo's own
	 * {@link DSeriesLabelSizes} presets (12mm x 12mm) rather than an arbitrary size: the
	 * pre-rotation <em>height</em> becomes the printhead's fixed physical dot-width after
	 * {@link RasterImage#rotate90Clockwise()}, so it must match real hardware capacity, not be
	 * picked freely - an earlier arbitrary 60px value produced a misaligned/doubled print on real
	 * hardware, presumably from the printer reinterpreting the byte stream against its own fixed
	 * row width instead of the (too narrow) one this code declared. {@code --label} selects any of
	 * {@link DSeriesLabelSizes}' other presets instead. This is a minimal validation pattern, not
	 * real label creation (no image pipeline exists yet - see CLAUDE.md).
	 */
	@Command(name = "phomemo-print-test",
			description = "Print a small test pattern to a Phomemo d-series printer (D30/D35/D50/Q30/Q30S). Uses real consumables.")
	static class PhomemoPrintTestCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-d", "--density"}, description = "Print density 1-8 (default: ${DEFAULT-VALUE}).")
		int density = 6;

		@Option(names = "--continuous", description = "Continuous tape instead of die-cut/gap labels.")
		boolean continuous;

		@Option(names = {"-l", "--label"},
				description = "Label size preset in mm, WIDTHxHEIGHT (default: ${DEFAULT-VALUE}). Valid: 40x12, 30x12, "
						+ "22x12, 12x12, 30x14, 22x14, 40x15, 30x15 (from phomymo's own D_SERIES_LABEL_SIZES).")
		String label = "12x12";

		@Override
		public Integer call() throws Exception {
			Optional<DSeriesLabelSizes.Preset> preset = DSeriesLabelSizes.find(label);
			if (!preset.isPresent()) {
				System.err.println("Unknown --label \"" + label + "\" - valid presets: "
						+ DSeriesLabelSizes.all().stream().map(DSeriesLabelSizes.Preset::getKey).collect(Collectors.joining(", ")));
				return 1;
			}

			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				BleTransport transport = new BleTransport(peripheral);
				transport.connect();

				try {
					RasterImage image = buildTestPattern(preset.get());
					System.out.println("Printing " + (image.getWidthBytes() * 8) + "x" + image.getHeightLines()
							+ "px test pattern (label=" + label + ", density=" + density + ", continuous=" + continuous + ")...");

					DSeriesPrinter.print(transport, image, density, continuous, 0,
							pct -> System.out.print("\rSending: " + pct + "%  "));

					System.out.println();
					System.out.println("Done.");
				} finally {
					transport.disconnect();
				}
			}
			return 0;
		}

		/** A solid black square/rectangle with an 8px white margin, so a working print shows a clearly visible block, not just "something happened". */
		private static RasterImage buildTestPattern(DSeriesLabelSizes.Preset preset) {
			int widthBytes = preset.getWidthBytes(); // the label's length axis (pre-rotation width)
			int heightLines = preset.getHeightLines(); // the label's printhead-width axis (pre-rotation height) -
														// becomes the physical dot-width after rotation
			byte[] data = new byte[widthBytes * heightLines];

			for (int row = 8; row < heightLines - 8; row++) {
				for (int b = 1; b < widthBytes - 1; b++) {
					data[row * widthBytes + b] = (byte) 0xff;
				}
			}

			return new RasterImage(data, widthBytes, heightLines);
		}
	}

	/**
	 * Prints a small validation test pattern, dispatching to whichever print task
	 * {@link NiimbotPrintTasks} assigns to the connected model/protocol-version. Uses real
	 * consumables. Only D11_H ({@link D110V4PrintTask}) and M2_H ({@link B1PrintTask}) are actually
	 * confirmed against real hardware - every other model {@link NiimbotPrintTasks} maps is ported
	 * from niimbluelib but untested here, and any model it doesn't map at all has no print task
	 * ported yet (surfaced as a clear error below, not a silent guess). Unlike Phomemo's
	 * {@code d-series}, the Niimbot protocol doesn't require the image width to exactly match the
	 * printhead's physical capacity (the printer accepts whatever {@code cols} the caller
	 * declares) - this test still uses the connected printer's own confirmed printhead width for a
	 * full-width, unambiguous test print.
	 */
	@Command(name = "niimbot-print-test",
			description = "Print a small test pattern to a Niimbot-branded printer (D11_H/M2_H hardware-confirmed; every other model NiimbotPrintTasks maps is ported but untested). Uses real consumables.")
	static class NiimbotPrintTestCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'scan').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-d", "--density"}, description = "Print density, model-dependent range (default: ${DEFAULT-VALUE}).")
		int density = 3;

		@Option(names = {"-r", "--rows"}, description = "Test pattern length in dots (default: ${DEFAULT-VALUE}, ~10mm at 300 DPI).")
		int rows = 120;

		@Option(names = {"-v", "--debug"}, description = "Log raw TX/RX packet bytes to stderr.")
		boolean debug;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'scan' first to confirm the address).");
					return 1;
				}

				BlePeripheral peripheral = found.get(0).getPeripheral(adapter);
				NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
				device.setDebug(debug);

				try {
					PrinterInfo info = device.connect();
					int cols = info.getPrintheadWidth() != null ? info.getPrintheadWidth() : 96;
					System.out.println("Connected: " + info);
					System.out.println("Printing " + cols + "x" + rows + "px test pattern (density=" + density + ")...");

					EncodedImage image = NiimbotImageEncoder.encode(buildTestPattern(cols, rows), PageColorType.SINGLE_COLOR);

					PrintOptions options = new PrintOptions()
							.setLabelType(info.getLabelType() != null ? info.getLabelType() : LabelType.WITH_GAPS)
							.setDensity(density)
							.setTotalPages(1);

					PrinterModel model = device.getModelMetadata().map(PrinterModelMeta::getModel).orElse(null);
					AbstractNiimbotPrintTask task = NiimbotPrintTasks.findPrintTask(model, info.getProtocolVersion())
							.map(factory -> factory.create(device, options))
							.orElseThrow(() -> new IllegalStateException("No print task implemented for model " + model + " yet"));

					task.printInit();
					task.printPage(image, 1);
					task.waitForFinished();
					task.printEnd();

					System.out.println("Done.");
				} finally {
					device.disconnect();
				}
			}
			return 0;
		}

		/** A solid black rectangle with an 8px white margin, full printhead width. */
		private static PixelSource buildTestPattern(int cols, int rows) {
			return new PixelSource() {
				@Override
				public int getWidth() {
					return cols;
				}

				@Override
				public int getHeight() {
					return rows;
				}

				@Override
				public boolean isBlack(int x, int y) {
					int margin = 8;
					return x >= margin && x < cols - margin && y >= margin && y < rows - margin;
				}
			};
		}
	}

	/**
	 * Unfiltered scan cross-referenced against {@link PrinterCatalog} - the manufacturer-agnostic
	 * replacement for {@code scan}'s Niimbot-service-UUID filter, which (per this project's own
	 * real-hardware testing) neither a genuine Niimbot D11_H nor a Phomemo Q30 actually advertises.
	 */
	@Command(name = "discover", description = "Unfiltered BLE scan, showing the guessed printer family (if any) for each device found.")
	static class DiscoverCommand implements Callable<Integer> {

		@Option(names = {"-t", "--timeout"}, description = "Scan duration in milliseconds (default: ${DEFAULT-VALUE}).")
		long timeoutMs = 5000;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> results = BleUtils.scan(adapter, timeoutMs);

				if (results.isEmpty()) {
					System.out.println("No devices found.");
					return 0;
				}

				for (BleUtils.BleDeviceResult r : results) {
					List<PrinterDefinition> matches = PrinterCatalog.detect(r.getName());
					String guess;
					if (matches.isEmpty()) {
						guess = "-";
					} else if (matches.size() == 1) {
						guess = matches.get(0).toString();
					} else {
						guess = "AMBIGUOUS " + matches;
					}

					System.out.printf("%s\t%s\trssi=%s\t%s%n", r.getAddress(), r.getName() != null ? r.getName() : "-",
							r.getRssi() != null ? r.getRssi() : "-", guess);
				}
			}
			return 0;
		}
	}

	/**
	 * Connects through the {@link cz.bliksoft.ptlabelprint.printer} abstraction layer: detects the
	 * family from the device's advertised name (see {@link PrinterCatalog}), then dispatches to the
	 * matching {@link LabelPrinter}. For Niimbot, prints the same {@link PrinterInfo} {@code info}
	 * does; Phomemo's {@code d-series} has no info-query capability at all (see
	 * {@link cz.bliksoft.ptlabelprint.printer.PhomemoDSeriesLabelPrinter}'s javadoc), so a
	 * successful connect is all this can report for it.
	 */
	@Command(name = "connect", description = "Auto-detect a printer's family by name and connect through the printer abstraction layer.")
	static class ConnectCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'discover').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Override
		public Integer call() throws Exception {
			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'discover' first to confirm the address).");
					return 1;
				}

				BleUtils.BleDeviceResult result = found.get(0);
				List<PrinterDefinition> matches = PrinterCatalog.detect(result.getName());

				if (matches.isEmpty()) {
					System.err.println("Could not detect a known printer family from name \"" + result.getName()
							+ "\" - see 'gatt'/'raw' for protocol-agnostic bring-up instead.");
					return 1;
				}
				if (matches.size() > 1) {
					System.err.println("Ambiguous match for name \"" + result.getName() + "\": " + matches);
					return 1;
				}

				PrinterDefinition definition = matches.get(0);
				System.out.println("Detected: " + definition);

				LabelPrinter printer;
				try {
					printer = PrinterFactory.create(definition, new BleTransport(result.getPeripheral(adapter)));
				} catch (UnimplementedPrinterFamilyException e) {
					System.err.println("Detected as " + definition + ", but that family isn't implemented yet"
							+ " (cataloged, not printable) - see CLAUDE.md's \"Protocol families\" section.");
					return 1;
				}

				try (LabelPrinter p = printer) {
					p.connect();
					System.out.println("Connected.");

					if (p instanceof NiimbotLabelPrinter) {
						System.out.println(((NiimbotLabelPrinter) p).getPrinterInfo());
					} else {
						System.out.println("(no info-query capability for this protocol family)");
					}
				}
			}
			return 0;
		}
	}

	/**
	 * Exercises the manufacturer-agnostic {@link LabelPrinter#print(BufferedImage, PrintJob)}
	 * surface end to end, regardless of which family {@code --address} turns out to be: detect via
	 * {@link PrinterCatalog}, connect via {@link PrinterFactory}, print {@link PrinterCapabilities},
	 * then one {@code print(BufferedImage, PrintJob)} call. {@code niimbot-print-test}/
	 * {@code phomemo-print-test} stay as they are for family-specific options this common surface
	 * doesn't expose - see this class's own javadoc. Uses real consumables.
	 */
	@Command(name = "print-test",
			description = "Print a BufferedImage through the unified LabelPrinter.print(BufferedImage, PrintJob) abstraction, regardless of printer family. Uses real consumables.")
	static class PrintTestCommand implements Callable<Integer> {

		@Parameters(index = "0", description = "BLE address of the printer (see 'discover').")
		String address;

		@Option(names = {"-t", "--scan-timeout"},
				description = "How long to scan for the address before connecting, in ms (default: ${DEFAULT-VALUE}).")
		long scanTimeoutMs = 5000;

		@Option(names = {"-c", "--copies"}, description = "Number of copies (default: ${DEFAULT-VALUE}).")
		int copies = 1;

		@Option(names = "--continuous", description = "Continuous tape instead of gapped/die-cut labels.")
		boolean continuous;

		@Option(names = {"-d", "--density"},
				description = "Print density, family-native scale and range (default: family's own default).")
		Integer density;

		@Option(names = {"-r", "--rotation"},
				description = "auto (default), none, 90, 180, 270 - see LabelPrinter's own javadoc for the algorithm.")
		String rotation = "auto";

		@Option(names = {"-i", "--image"},
				description = "Image file to print (any format ImageIO can read). Default: a synthetic solid-square test pattern.")
		File image;

		@Override
		public Integer call() throws Exception {
			Rotation parsedRotation = parseRotation(rotation);
			if (parsedRotation == null) {
				System.err.println("Unknown --rotation \"" + rotation + "\" - valid: auto, none, 90, 180, 270.");
				return 1;
			}

			try (BleAdapter adapter = new BleAdapter()) {
				List<BleUtils.BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress(address), scanTimeoutMs);

				if (found.isEmpty()) {
					System.err.println("Device " + address + " not found during scan (run 'discover' first to confirm the address).");
					return 1;
				}

				BleUtils.BleDeviceResult result = found.get(0);
				List<PrinterDefinition> matches = PrinterCatalog.detect(result.getName());

				if (matches.isEmpty()) {
					System.err.println("Could not detect a known printer family from name \"" + result.getName() + "\".");
					return 1;
				}
				if (matches.size() > 1) {
					System.err.println("Ambiguous match for name \"" + result.getName() + "\": " + matches);
					return 1;
				}

				PrinterDefinition definition = matches.get(0);
				System.out.println("Detected: " + definition);

				LabelPrinter printer;
				try {
					printer = PrinterFactory.create(definition, new BleTransport(result.getPeripheral(adapter)));
				} catch (UnimplementedPrinterFamilyException e) {
					System.err.println("Detected as " + definition + ", but that family isn't implemented yet.");
					return 1;
				}

				try (LabelPrinter p = printer) {
					p.connect();
					System.out.println("Connected.");

					PrinterCapabilities capabilities = p.getCapabilities();
					System.out.println("Capabilities: " + capabilities);

					BufferedImage img = image != null ? ImageIO.read(image) : buildTestPattern(capabilities.getPrintheadPixels());
					if (img == null) {
						System.err.println("Could not read image file: " + image);
						return 1;
					}

					PrintJob job = new PrintJob()
							.setCopies(copies)
							.setContinuousMedia(continuous)
							.setDensity(density)
							.setRotation(parsedRotation);

					System.out.println("Printing " + img.getWidth() + "x" + img.getHeight() + "px (copies=" + copies
							+ ", continuous=" + continuous + ", density=" + (density != null ? density : "default")
							+ ", rotation=" + parsedRotation + ")...");

					p.print(img, job);

					System.out.println("Done.");
				}
			}
			return 0;
		}

		private static Rotation parseRotation(String value) {
			switch (value.toLowerCase()) {
			case "auto":
				return Rotation.AUTO;
			case "none":
				return Rotation.NONE;
			case "90":
				return Rotation.CW_90;
			case "180":
				return Rotation.CW_180;
			case "270":
				return Rotation.CW_270;
			default:
				return null;
			}
		}

		/** A solid black square with a white margin, sized to the printhead width - a safe default when no --image is given. */
		private static BufferedImage buildTestPattern(int printheadPixels) {
			int size = Math.max(printheadPixels, 1);
			BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, size, size);
			g.setColor(Color.BLACK);
			int margin = Math.max(size / 8, 1);
			g.fillRect(margin, margin, size - 2 * margin, size - 2 * margin);
			g.dispose();
			return img;
		}
	}
}
