package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code B21_L2B} print task - ported from niimbluelib's
 * {@code B21L2BPrintTask} (src/print_tasks/B21L2BPrintTask.ts). Per {@link NiimbotPrintTasks}'
 * dispatch table, this is the print task the B21_L2B uses. The most structurally different task in
 * this package: instead of a status/PrintEnd poll, each copy in {@link #printPage} retries
 * {@link NiimbotDevice#pageStart()} until it returns {@code true} (ported from niimbluelib's
 * {@code Utils.doUntilTrue}), and per-page completion is a separate retry loop on
 * {@link NiimbotDevice#pageEnd()} in {@link #waitForPageFinished()} - which, unlike every other
 * task here, is the method that actually does the waiting. {@link #waitForFinished()} itself is a
 * deliberate no-op, matching niimbluelib exactly: <b>a caller printing multiple pages with this
 * task must call {@link #waitForPageFinished()} once per page</b> (e.g. after each
 * {@link #printPage} call in a per-page loop), not just once at the end - this project's simpler
 * {@code Cli} single-page usage doesn't currently exercise that multi-page loop.
 */
public class B21L2BPrintTask extends AbstractNiimbotPrintTask {

	public B21L2BPrintTask(NiimbotDevice device, PrintOptions options) {
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
			doUntilTrue(device::pageStart, 5, 500);

			List<NiimbotPacket> pkts = new ArrayList<>();
			pkts.add(PacketGenerator.setPageSize4b(image.getRows(), image.getCols()));
			pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels(), "total"));
			pkts.add(PacketGenerator.pageEnd());

			device.sendAllRaw(pkts, options.getPageTimeoutMs());
		}
	}

	/** The real per-page wait step for this task - see class javadoc. */
	@Override
	public void waitForPageFinished() throws IOException, TimeoutException {
		doUntilTrue(device::pageEnd, 20, 500);
	}

	/** Deliberate no-op - see class javadoc. */
	@Override
	public void waitForFinished() throws IOException, TimeoutException {
	}

	private interface BooleanCall {
		boolean call() throws IOException, TimeoutException;
	}

	/** Ported from niimbluelib's {@code Utils.doUntilTrue}. */
	private static void doUntilTrue(BooleanCall fn, int attempts, long delayMs) throws IOException, TimeoutException {
		IOException lastIo = null;
		TimeoutException lastTimeout = null;

		for (int attempt = 0; attempt < attempts; attempt++) {
			try {
				if (fn.call()) {
					return;
				}
			} catch (IOException e) {
				lastIo = e;
			} catch (TimeoutException e) {
				lastTimeout = e;
			}
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted during doUntilTrue retry", e);
			}
		}

		if (lastTimeout != null) {
			throw lastTimeout;
		}
		if (lastIo != null) {
			throw lastIo;
		}
		throw new TimeoutException("Maximum attempts reached");
	}
}
