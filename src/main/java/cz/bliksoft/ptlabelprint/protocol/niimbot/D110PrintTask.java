package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code D110} print task - ported from niimbluelib's {@code D110PrintTask}
 * (src/print_tasks/D110PrintTask.ts). Per {@link NiimbotPrintTasks}' dispatch table, this is the
 * print task B21S/B21S_C2B/D110 use (also plain D11 at protocol v1/v2 - <b>not</b> D11/D11S at
 * their default protocol version, which use {@link OldD11PrintTask}). Same
 * {@link PacketGenerator#printClear()}/{@link PacketGenerator#setPrintQuantity(int)} shape as
 * {@link OldD11PrintTask}, but a 4-byte page size ({@link PacketGenerator#setPageSize4b}, rows
 * <i>and</i> cols) and completion tracked by status-polling against {@link #getPagesPrinted()}
 * (how many pages have actually been sent so far) rather than the configured
 * {@link PrintOptions#getTotalPages()} - ported verbatim from niimbluelib's own
 * {@code waitForPageFinished}/{@code waitForFinished} split, kept "for compatibility with previous
 * versions" per niimbluelib's own comment even though it sends one redundant packet.
 */
public class D110PrintTask extends AbstractNiimbotPrintTask {

	public D110PrintTask(NiimbotDevice device, PrintOptions options) {
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

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.printClear());
		pkts.add(PacketGenerator.pageStart());
		pkts.add(PacketGenerator.setPageSize4b(image.getRows(), image.getCols()));
		pkts.add(PacketGenerator.setPrintQuantity(quantity));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels()));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}

	@Override
	public void waitForPageFinished() throws IOException, TimeoutException {
		device.setPacketTimeout(options.getStatusTimeoutMs());
		try {
			device.waitUntilPrintFinishedByStatusPoll(getPagesPrinted(), options.getStatusPollIntervalMs(),
					options.getStatusTimeoutMs() * getPagesPrinted() + 10_000);
		} finally {
			device.setPacketTimeout(1000);
		}
	}

	@Override
	public void waitForFinished() throws IOException, TimeoutException {
		waitForPageFinished();
	}
}
