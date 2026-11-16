package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * {@link B1PrintTask} for printers that ignore the quantity of
 * {@code SetPageSize} - confirmed on a real Niimbot N1 (protocol v1): a page
 * sent with quantity 2 came out once, its status stayed at page 1 (100 %/100 %)
 * and the wait for page 2 ran into its timeout. So every copy is sent as a page
 * of its own, with quantity 1, and the next one only after the printer reported
 * the previous one finished. Not part of niimbluelib, which sends the quantity
 * to every {@code B1} model.
 */
public class N1PrintTask extends B1PrintTask {

	private int pagesSent;

	public N1PrintTask(NiimbotDevice device, PrintOptions options) {
		super(device, options);
	}

	@Override
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		for (int copy = 0; copy < quantity; copy++) {
			super.printPage(image, 1);
			pagesSent++;
			if (copy < quantity - 1) {
				waitForPages(pagesSent);
			}
		}
	}

	private void waitForPages(int pages) throws IOException, TimeoutException {
		device.setPacketTimeout(options.getStatusTimeoutMs());
		try {
			device.waitUntilPrintFinishedByStatusPoll(pages, options.getStatusPollIntervalMs(),
					options.getStatusTimeoutMs() + 10_000);
		} finally {
			device.setPacketTimeout(1000);
		}
	}
}
