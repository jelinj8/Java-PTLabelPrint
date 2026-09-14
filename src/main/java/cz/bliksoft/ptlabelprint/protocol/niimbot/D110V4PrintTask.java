package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code D110M_V4} print task - ported from niimbluelib's
 * {@code D110MV4PrintTask} (src/print_tasks/D110MV4PrintTask.ts). Per niimbluelib's own
 * {@code modelPrintTasks} dispatch table (see {@link NiimbotPrintTasks}), this is the print task
 * Niimbot's D11_H uses (also D110_M protocol v4, B21_PRO, B1_PRO, C1, EP1C) - <b>not</b> the same
 * as the plain D11's task ({@code OldD11PrintTask}) or D110's ({@code D110PrintTask}).
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
public class D110V4PrintTask extends AbstractNiimbotPrintTask {

	public D110V4PrintTask(NiimbotDevice device, PrintOptions options) {
		super(device, options);
	}

	@Override
	public boolean isSupportColor(PageColorType pageColor) {
		return pageColor == PageColorType.SINGLE_COLOR || pageColor == PageColorType.DOUBLE_COLOR;
	}

	/** Sets label type, density, and sends the print-start command. */
	@Override
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
	@Override
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		NiimbotPacket statusPacket = PacketGenerator.printStatus();
		statusPacket.setOneWay(true);
		device.sendRaw(statusPacket);

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.setPageSize13b(image.getRows(), image.getCols(), quantity, options.getCutHeight(),
				options.getCutType(), 0, 0, null));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels()));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}

	/**
	 * Ends the print job. Ported from niimbluelib's own override, including its B21_PRO-specific
	 * quirk of sending a one-way {@code Heartbeat} right after {@code PrintEnd} - per niimbluelib's
	 * own comment: "B21_PRO drops the first packet after PrintEnd."
	 */
	@Override
	public boolean printEnd() throws IOException, TimeoutException {
		NiimbotPacket heartbeatPkt = PacketGenerator.heartbeat(HeartbeatType.ADVANCED_1);
		heartbeatPkt.setOneWay(true);

		boolean result = device.printEnd();
		device.sendRaw(heartbeatPkt);
		return result;
	}
}
