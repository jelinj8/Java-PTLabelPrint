package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code D110M_V4} print task - ported from niimbluelib's
 * {@code D110MV4PrintTask} (src/print_tasks/D110MV4PrintTask.ts). Per niimbluelib's own
 * {@code modelPrintTasks} dispatch table, this is the print task Niimbot's D11_H uses (also
 * D110_M protocol v4, B21_PRO, B1_PRO, C1, EP1C) - <b>not</b> the same as the plain D11's task
 * ({@code OldD11PrintTask}, not ported) or D110's ({@code D110PrintTask}, not ported). This is the
 * only print task this project has ported so far; add another concrete class (not a shared
 * abstraction) if/when a model needing a different one is tested.
 *
 * <p>
 * Usage mirrors niimbluelib's own {@code AbstractPrintTask} example:
 *
 * <pre>{@code
 * D110V4PrintTask task = new D110V4PrintTask(device, options);
 * task.printInit();
 * task.printPage(encodedImage, quantity);
 * task.waitForFinished();
 * task.printEnd();
 * }</pre>
 */
public class D110V4PrintTask {

	private final NiimbotDevice device;
	private final PrintOptions options;
	private int pagesPrinted;

	public D110V4PrintTask(NiimbotDevice device, PrintOptions options) {
		this.device = device;
		this.options = options;
	}

	public boolean isSupportColor(PageColorType pageColor) {
		return pageColor == PageColorType.SINGLE_COLOR || pageColor == PageColorType.DOUBLE_COLOR;
	}

	/** Sets label type, density, and sends the print-start command. */
	public void printInit() throws IOException, TimeoutException {
		device.sendAllRaw(Arrays.asList(
				PacketGenerator.setLabelType(options.getLabelType().getCode()),
				PacketGenerator.setDensity(options.getDensity()),
				PacketGenerator.printStart9b(options.getTotalPages(), options.getPageColor(), options.getSpeed(), false)));
	}

	/**
	 * Prints one page. Ported from niimbluelib's own {@code printPage}, including its
	 * B21_PRO-specific quirk of sending {@code PrintStatus} one-way (no response waited)
	 * immediately after {@code PrintStart} - kept even though it's undocumented why real hardware
	 * needs it, per niimbluelib's own comment: "does not respond on first packet after PrintStart
	 * if using Bluetooth connection."
	 */
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		NiimbotPacket statusPacket = PacketGenerator.printStatus();
		statusPacket.setOneWay(true);
		device.sendRaw(statusPacket);

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.setPageSize13b(image.getRows(), image.getCols(), quantity, options.getCutHeight(),
				options.getCutType(), 0, 0, null));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, device.getModelMetadata()
				.map(PrinterModelMeta::getPrintheadPixels).orElse(0)));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}

	private void validatePage(EncodedImage image, int quantity) {
		if (pagesPrinted + quantity > options.getTotalPages()) {
			throw new IllegalStateException("Trying to print too many pages (totalPages may not be set correctly)");
		}
		if (!isSupportColor(image.getPageColor())) {
			throw new IllegalArgumentException("Page color " + image.getPageColor() + " is not supported by this print task");
		}
		if (options.getPageColor() != image.getPageColor()) {
			throw new IllegalArgumentException(
					"Page color " + image.getPageColor() + " does not match print task color " + options.getPageColor());
		}
		pagesPrinted += quantity;
	}

	/** Polls print status until all pages report finished. */
	public void waitForFinished() throws IOException, TimeoutException {
		device.setPacketTimeout(options.getStatusTimeoutMs());
		try {
			device.waitUntilPrintFinishedByStatusPoll(options.getTotalPages(), options.getStatusPollIntervalMs(),
					options.getStatusTimeoutMs() * options.getTotalPages() + 10_000);
		} finally {
			device.setPacketTimeout(1000);
		}
	}

	/**
	 * Ends the print job. Ported from niimbluelib's own override, including its B21_PRO-specific
	 * quirk of sending a one-way {@code Heartbeat} right after {@code PrintEnd} - per niimbluelib's
	 * own comment: "B21_PRO drops the first packet after PrintEnd."
	 */
	public boolean printEnd() throws IOException, TimeoutException {
		NiimbotPacket heartbeatPkt = PacketGenerator.heartbeat(HeartbeatType.ADVANCED_1);
		heartbeatPkt.setOneWay(true);

		boolean result = device.printEnd();
		device.sendRaw(heartbeatPkt);
		return result;
	}
}
