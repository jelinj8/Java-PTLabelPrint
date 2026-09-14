package cz.bliksoft.ptlabelprint;

import java.util.List;
import java.util.concurrent.Callable;

import cz.bliksoft.javautils.ble.BleAdapter;
import cz.bliksoft.javautils.ble.BleCharacteristic;
import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.javautils.ble.BleService;
import cz.bliksoft.javautils.ble.ScanFilter;
import cz.bliksoft.javautils.ble.utils.BleUtils;
import cz.bliksoft.ptlabelprint.protocol.BleTransport;
import cz.bliksoft.ptlabelprint.protocol.niimbot.D110V4PrintTask;
import cz.bliksoft.ptlabelprint.protocol.niimbot.EncodedImage;
import cz.bliksoft.ptlabelprint.protocol.niimbot.LabelType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotImageEncoder;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PageColorType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PixelSource;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrintOptions;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterInfo;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command-line front end for PtLabelPrint. Only the Niimbot protocol family ({@link NiimbotDevice})
 * is wired up so far - {@code scan}/{@code info} exercise connect and printer-info retrieval
 * against Niimbot-branded printers specifically (D11_H, M2), not Phomemo's (confirmed via real Q30
 * hardware to speak an unrelated protocol - see the project CLAUDE.md's "Status" section).
 * {@code gatt}/{@code raw} are protocol-agnostic diagnostics for bringing up a new/unfamiliar
 * device's BLE channel by hand. Phomemo support and configure/print commands land here once
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} exists.
 */
@Command(name = "ptlabelprint-cli", mixinStandardHelpOptions = true, version = "ptlabelprint 0.1.0-SNAPSHOT",
		description = "CLI for Niimbot-protocol label printers (BLE, optionally Serial/USB) plus protocol-agnostic BLE diagnostics.",
		subcommands = {Cli.ScanCommand.class, Cli.InfoCommand.class, Cli.RawCommand.class, Cli.GattCommand.class,
				Cli.PhomemoPrintTestCommand.class, Cli.NiimbotPrintTestCommand.class})
public class Cli implements Runnable {

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Cli()).execute(args);
		System.exit(exitCode);
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
	 * protocol - D30/D35/D50/D110/Q30/Q30S). Uses real consumables - deliberately the smallest of
	 * phomymo's own confirmed-working D-series label presets (12mm x 12mm, its {@code D_SERIES_LABEL_SIZES['12x12']})
	 * rather than an arbitrary size: the pre-rotation <em>height</em> becomes the printhead's fixed
	 * physical dot-width after {@link RasterImage#rotate90Clockwise()}, so it must match real
	 * hardware capacity, not be picked freely - an earlier arbitrary 60px value produced a
	 * misaligned/doubled print on real hardware, presumably from the printer reinterpreting the
	 * byte stream against its own fixed row width instead of the (too narrow) one this code
	 * declared. This is a minimal validation pattern, not real label creation (no image pipeline
	 * exists yet - see CLAUDE.md).
	 */
	@Command(name = "phomemo-print-test",
			description = "Print a small test pattern to a Phomemo d-series printer (D30/D35/D50/D110/Q30/Q30S). Uses real consumables.")
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

		@Override
		public Integer call() throws Exception {
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
					RasterImage image = buildTestPattern();
					System.out.println("Printing " + (image.getWidthBytes() * 8) + "x" + image.getHeightLines()
							+ "px test pattern (density=" + density + ", continuous=" + continuous + ")...");

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

		/**
		 * phomymo's {@code D_SERIES_LABEL_SIZES['12x12']}: 12mm x 12mm at 203 DPI (8px/mm per
		 * phomymo's own {@code PX_PER_MM}) = 96x96px pre-rotation - a solid black square with an
		 * 8px white margin, so a working print shows a clearly visible, correctly-square black
		 * block, not just "something happened".
		 */
		private static RasterImage buildTestPattern() {
			int widthBytes = 12; // 96px: the label's length axis (pre-rotation width)
			int heightLines = 96; // 96px: the label's printhead-width axis (pre-rotation height) - becomes the
									// physical dot-width after rotation, so must match real hardware capacity
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
	 * Prints a small validation test pattern via {@link D110V4PrintTask} - the print task
	 * niimbluelib's own model dispatch table assigns to Niimbot's D11_H (also D110_M protocol v4,
	 * B21_PRO, B1_PRO, C1, EP1C - untested here). Uses real consumables. Unlike Phomemo's
	 * {@code d-series}, the Niimbot protocol doesn't require the image width to exactly match the
	 * printhead's physical capacity (the printer accepts whatever {@code cols} the caller
	 * declares) - this test still uses the connected printer's own confirmed printhead width for a
	 * full-width, unambiguous test print.
	 */
	@Command(name = "niimbot-print-test",
			description = "Print a small test pattern to a Niimbot-branded printer (D11_H tested; likely also D110_M v4/B21_PRO/B1_PRO/C1/EP1C). Uses real consumables.")
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

					D110V4PrintTask task = new D110V4PrintTask(device, options);
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
}
