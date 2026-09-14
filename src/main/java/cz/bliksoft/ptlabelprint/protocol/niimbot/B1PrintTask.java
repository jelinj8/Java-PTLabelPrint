package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code B1} print task - ported from niimbluelib's {@code B1PrintTask}
 * (src/print_tasks/B1PrintTask.ts). Per niimbluelib's own {@code modelPrintTasks} dispatch table,
 * this is the print task Niimbot's <b>M2_H</b> uses (also B1, D110_M protocol &lt; 4, B21_C2B, N1,
 * D101) - despite the similarly-named {@link D110V4PrintTask} (the {@code D110M_V4} task), which
 * the M2_H does <b>not</b> use. Simpler than {@link D110V4PrintTask}: no B21_PRO one-way-PrintStatus
 * quirk before the image rows, no one-way-Heartbeat quirk after {@code PrintEnd}, and an explicit
 * {@code PageStart} the D110M_V4 flow doesn't send.
 *
 * <p>
 * Usage mirrors {@link D110V4PrintTask}'s own javadoc example.
 */
public class B1PrintTask {

	private final NiimbotDevice device;
	private final PrintOptions options;
	private int pagesPrinted;

	public B1PrintTask(NiimbotDevice device, PrintOptions options) {
		this.device = device;
		this.options = options;
	}

	public boolean isSupportColor(PageColorType pageColor) {
		return pageColor == PageColorType.SINGLE_COLOR || pageColor == PageColorType.DOUBLE_COLOR;
	}

	/** Sets density, label type, and sends the print-start command. */
	public void printInit() throws IOException, TimeoutException {
		device.sendAllRaw(Arrays.asList(PacketGenerator.setDensity(options.getDensity()),
				PacketGenerator.setLabelType(options.getLabelType().getCode()),
				PacketGenerator.printStart7b(options.getTotalPages(), options.getPageColor())));
	}

	/** Prints one page. Ported from niimbluelib's own {@code printPage}. */
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.pageStart());
		pkts.add(PacketGenerator.setPageSize6b(image.getRows(), image.getCols(), quantity));
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

	/** Ends the print job. Unlike {@link D110V4PrintTask#printEnd()}, no B21_PRO-specific quirk here. */
	public boolean printEnd() throws IOException, TimeoutException {
		return device.printEnd();
	}
}
