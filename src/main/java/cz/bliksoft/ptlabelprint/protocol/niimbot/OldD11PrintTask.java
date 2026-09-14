package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code D11_V1} print task - ported from niimbluelib's {@code OldD11PrintTask}
 * (src/print_tasks/OldD11PrintTask.ts). Per {@link NiimbotPrintTasks}' dispatch table, this is the
 * print task the plain D11 and D11S use (<b>not</b> the D11_H, which uses {@link D110V4PrintTask}).
 * Distinguishing features versus the newer tasks: {@link PacketGenerator#printClear()} before each
 * page, an explicit {@link PacketGenerator#setPrintQuantity(int)} instead of a copies-count baked
 * into the page-size packet, and completion tracked via {@link NiimbotDevice#waitForPageIndex} - an
 * unsolicited packet the printer pushes on its own, not a status poll.
 */
public class OldD11PrintTask extends AbstractNiimbotPrintTask {

	public OldD11PrintTask(NiimbotDevice device, PrintOptions options) {
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
		pkts.add(PacketGenerator.setPageSize2b(image.getRows()));
		pkts.add(PacketGenerator.setPrintQuantity(quantity));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels()));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}

	@Override
	public void waitForFinished() throws IOException, TimeoutException {
		device.waitForPageIndex(options.getTotalPages(), options.getStatusTimeoutMs());
	}
}
