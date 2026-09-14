package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code B21_V1} print task - ported from niimbluelib's {@code B21V1PrintTask}
 * (src/print_tasks/B21V1PrintTask.ts). Per {@link NiimbotPrintTasks}' dispatch table, this is the
 * print task the B21 uses. Unlike every other task in this package, {@code quantity} copies aren't
 * requested via a single page-size field - each copy is sent as its own complete page in a loop
 * (niimbluelib's own comment leaves {@code PacketGenerator.printClear()} commented out here, so
 * this port omits it too, unlike {@link OldD11PrintTask}/{@link D110PrintTask}). Completion is
 * tracked via {@link NiimbotDevice#waitUntilPrintFinishedByPrintEndPoll} - repeatedly retrying
 * {@link NiimbotDevice#printEnd()} until it succeeds, not a {@code PrintStatus} poll.
 */
public class B21V1PrintTask extends AbstractNiimbotPrintTask {

	public B21V1PrintTask(NiimbotDevice device, PrintOptions options) {
		super(device, options);
	}

	@Override
	public void printInit() throws IOException, TimeoutException {
		device.sendAllRaw(Arrays.asList(PacketGenerator.setDensity(options.getDensity()),
				PacketGenerator.setLabelType(options.getLabelType().getCode()), PacketGenerator.printStart1b()));
	}

	@Override
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		for (int i = 0; i < quantity; i++) {
			List<NiimbotPacket> pkts = new ArrayList<>();
			pkts.add(PacketGenerator.pageStart());
			pkts.add(PacketGenerator.setPageSize4b(image.getRows(), image.getCols()));
			pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels(), "total"));
			pkts.add(PacketGenerator.pageEnd());

			device.sendAllRaw(pkts, options.getPageTimeoutMs());
		}
	}

	@Override
	public void waitForFinished() throws IOException, TimeoutException {
		device.setPacketTimeout(options.getStatusTimeoutMs());
		try {
			device.waitUntilPrintFinishedByPrintEndPoll(options.getStatusPollIntervalMs(),
					options.getStatusTimeoutMs() * options.getTotalPages() + 10_000);
		} finally {
			device.setPacketTimeout(1000);
		}
	}
}
