package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import cz.bliksoft.ptlabelprint.protocol.Transport;

/**
 * Print flow for Phomemo's {@code m110} protocol (M110/M110S/M120). Ported from phomymo's
 * {@code printM110} (src/web/printer.js). Same BLE channel as {@code d-series} (service
 * {@code 0xff00}, write {@code 0xff02}, notify {@code 0xff03}), but no rotation - the image is sent
 * in the orientation it's read in, rows across the printhead.
 *
 * <p>
 * Also the protocol the <b>Phomemo M421</b> speaks, which phomymo itself doesn't know about: a real
 * M421 answers this family's {@code 1F 11 xx} status queries over that same channel (firmware
 * {@code 1a 07 ..}, power {@code 1a 04 ..}, paper {@code 1a 06 ..}, cover {@code 1a 05 ..}), and
 * phomemo-tools' own M421 CUPS driver definition routes it through its M110 filter - used here as a
 * source of wire-format facts only (GPL-3.0, no code ported from it).
 *
 * <p>
 * Three things here go beyond phomymo's flow, each from a real-hardware failure on the M421. For
 * all of them, {@link #print} takes over the transport's raw-data listener for the duration of the
 * call and clears it afterwards.
 * <ul>
 * <li><b>It waits for the printer to finish</b> before returning: the M421 notifies {@code 1a 0f 0c}
 * once a job has physically printed, and dropping the BLE link before that cuts the label short (a
 * caller that tore its connection down right after sending left a label printed only a few
 * millimetres in).</li>
 * <li><b>It reports a failed job.</b> The M421 notifies {@code 1a 0b b8} when a print fails (the
 * meaning is phomymo's own, from its status parser); {@link #print} then stops sending and throws.</li>
 * <li><b>It sends as fast as the printer's own flow control allows</b>, when the printer has one.
 * phomymo's fixed pacing (128 bytes every 20ms, ~5 KB/s in practice) is fine for a narrow label but
 * starves a wide one: the M421 prints while it receives, and a 100x150mm label - 100 bytes per
 * row, so that pacing delivers only ~5mm of label per second - visibly slowed down, stopped after
 * ~20mm and reported the job failed. See {@link LinkInfo} for the credit scheme this uses instead; a
 * printer that doesn't announce one (nothing is known about the real M110/M120 here) keeps phomymo's
 * pacing unchanged.</li>
 * </ul>
 */
public final class M110Printer {

	private static final int CHUNK_SIZE = 128;
	private static final int CHUNK_DELAY_MS = 20;
	private static final int SPEED = 5;
	/** Notified by the printer once a job has finished printing. */
	private static final byte[] JOB_FINISHED = {0x1a, 0x0f, 0x0c};
	/** Notified by the printer when a job has failed. */
	private static final byte[] JOB_FAILED = {0x1a, 0x0b, (byte) 0xb8};
	/** First byte of a two-byte "grant this many write credits" notification. */
	private static final int CREDIT_GRANT = 0x01;
	/** First byte of a three-byte "largest write I take" notification (little-endian size follows). */
	private static final int MAX_PAYLOAD = 0x02;
	private static final long FINISH_TIMEOUT_BASE_MS = 5000;
	private static final long FINISH_TIMEOUT_PER_LINE_MS = 10;
	/** How long the printer may hold a write back before the job is given up on. */
	private static final long CREDIT_TIMEOUT_MS = 30000;
	/** How long {@link #print} gives a just-connected printer to announce its {@link LinkInfo}. */
	private static final long LINK_INFO_WAIT_MS = 500;

	private M110Printer() {
	}

	/**
	 * What the printer announces about its own flow control right after the notify characteristic is
	 * subscribed - so it has to be listened for <em>before</em> {@link Transport#connect()}, see
	 * {@link #listenForLinkInfo}. Observed on a real M421, identically over a Windows adapter and an
	 * ESP32 bridge:
	 * <ul>
	 * <li>{@code 01 07} - 7 write credits. Every write spends one; the printer hands one back
	 * ({@code 01 01}) per write it has taken in.</li>
	 * <li>{@code 02 b6 00} - the largest write it takes, 182 bytes (the link's MTU of 185, less the
	 * 3-byte ATT header).</li>
	 * </ul>
	 * Both stay 0 for a printer that announces neither.
	 */
	public static final class LinkInfo {
		private volatile int initialCredits;
		private volatile int maxPayload;

		public int getInitialCredits() {
			return initialCredits;
		}

		public int getMaxPayload() {
			return maxPayload;
		}

		private void onNotification(byte[] data) {
			if (data.length == 2 && (data[0] & 0xff) == CREDIT_GRANT && initialCredits == 0) {
				initialCredits = data[1] & 0xff;
			} else if (data.length == 3 && (data[0] & 0xff) == MAX_PAYLOAD) {
				maxPayload = (data[1] & 0xff) | ((data[2] & 0xff) << 8);
			}
		}
	}

	/**
	 * Installs a raw-data listener on {@code transport} that records the printer's {@link LinkInfo} -
	 * call it before {@link Transport#connect()}, and pass the result to {@link #print}.
	 */
	public static LinkInfo listenForLinkInfo(Transport transport) {
		LinkInfo info = new LinkInfo();
		transport.setRawDataListener(info::onNotification);
		return info;
	}

	/** {@link #print(Transport, RasterImage, int, boolean, IntConsumer, LinkInfo, Consumer)} with no link info and no notification tap. */
	public static boolean print(Transport transport, RasterImage image, int density, boolean continuous,
			IntConsumer onProgress) throws IOException {
		return print(transport, image, density, continuous, onProgress, null, null);
	}

	/**
	 * Prints {@code image} as-is (no rotation).
	 *
	 * @param transport        an already-{@link Transport#connect() connect}ed transport
	 * @param image            the label content, rows across the printhead
	 * @param density          1-8, higher is darker
	 * @param continuous       true for continuous (gapless) media, false for die-cut/gap labels
	 * @param onProgress       called with 0-100 as the data is sent; may be {@code null}
	 * @param linkInfo         from {@link #listenForLinkInfo}, or {@code null} if it wasn't listened
	 *                         for - the data then goes out at phomymo's slow fixed pacing, which is
	 *                         only enough for narrow labels
	 * @param notificationTap  sees every raw notification the printer sends during the call, for
	 *                         diagnostics; may be {@code null}
	 * @return true if the printer reported the job finished; false if it didn't within a generous,
	 *         length-scaled timeout (the job may still have printed - a model that doesn't send the
	 *         notification at all just costs that wait)
	 * @throws IOException if the printer reported the job failed, or stopped taking data
	 */
	public static boolean print(Transport transport, RasterImage image, int density, boolean continuous,
			IntConsumer onProgress, LinkInfo linkInfo, Consumer<byte[]> notificationTap) throws IOException {
		try {
			// The announcement follows the subscribe by a moment - don't race a caller that prints
			// straight after connecting into the slow uncredited path.
			long deadline = System.currentTimeMillis() + LINK_INFO_WAIT_MS;
			while (linkInfo != null && linkInfo.getInitialCredits() == 0 && System.currentTimeMillis() < deadline) {
				Thread.sleep(20);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted during print", e);
		}

		Session session = new Session(linkInfo != null ? linkInfo.getInitialCredits() : 0);
		transport.setRawDataListener(data -> {
			if (notificationTap != null) {
				notificationTap.accept(data);
			}
			session.onNotification(data);
		});
		try {
			if (session.credited) {
				int chunkSize = linkInfo.getMaxPayload() > 0 ? linkInfo.getMaxPayload() : CHUNK_SIZE;
				sendCredited(transport, session, image, density, continuous, onProgress, chunkSize);
			} else {
				sendPaced(transport, image, density, continuous, onProgress);
			}
			long timeoutMs = FINISH_TIMEOUT_BASE_MS + FINISH_TIMEOUT_PER_LINE_MS * image.getHeightLines();
			return session.awaitFinished(timeoutMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted during print", e);
		} finally {
			transport.setRawDataListener(null);
		}
	}

	/** phomymo's own flow, unchanged: fixed delays, no feedback from the printer. */
	private static void sendPaced(Transport transport, RasterImage image, int density, boolean continuous,
			IntConsumer onProgress) throws IOException, InterruptedException {
		transport.write(M110Commands.speed(SPEED));
		Thread.sleep(30);

		transport.write(M110Commands.density(M110Commands.densityToM110(density)));
		Thread.sleep(30);

		transport.write(M110Commands.mediaType(continuous));
		Thread.sleep(30);

		transport.write(M110Commands.rasterHeader(image.getWidthBytes(), image.getHeightLines()));

		// A single writeStream() call rather than a per-chunk write() loop - see DSeriesPrinter.
		if (onProgress != null) {
			onProgress.accept(0);
		}
		transport.writeStream(image.getData(), CHUNK_SIZE, CHUNK_DELAY_MS);
		if (onProgress != null) {
			onProgress.accept(100);
		}

		Thread.sleep(300);
		transport.write(M110Commands.FOOTER);
	}

	/** Same commands and data, but every write waits only for a credit from the printer - no fixed delays. */
	private static void sendCredited(Transport transport, Session session, RasterImage image, int density,
			boolean continuous, IntConsumer onProgress, int chunkSize) throws IOException, InterruptedException {
		session.write(transport, M110Commands.speed(SPEED));
		session.write(transport, M110Commands.density(M110Commands.densityToM110(density)));
		session.write(transport, M110Commands.mediaType(continuous));
		session.write(transport, M110Commands.rasterHeader(image.getWidthBytes(), image.getHeightLines()));

		byte[] data = image.getData();
		int lastPercent = -1;
		for (int offset = 0; offset < data.length; offset += chunkSize) {
			int len = Math.min(chunkSize, data.length - offset);
			session.write(transport, Arrays.copyOfRange(data, offset, offset + len));

			int percent = (int) ((offset + len) * 100L / data.length);
			if (onProgress != null && percent != lastPercent) {
				onProgress.accept(percent);
				lastPercent = percent;
			}
		}

		session.write(transport, M110Commands.FOOTER);
	}

	/** Credit and outcome state for one {@link #print} call, fed by the printer's notifications. */
	private static final class Session {
		final boolean credited;
		private int credits;
		private boolean finished;
		private boolean failed;

		Session(int initialCredits) {
			this.credits = initialCredits;
			this.credited = initialCredits > 0;
		}

		synchronized void onNotification(byte[] data) {
			if (data.length == 2 && (data[0] & 0xff) == CREDIT_GRANT) {
				credits += data[1] & 0xff;
			} else if (contains(data, JOB_FAILED)) {
				failed = true;
			} else if (contains(data, JOB_FINISHED)) {
				finished = true;
			} else {
				return;
			}
			notifyAll();
		}

		/** Waits for a write credit, spends it, and writes {@code data}. */
		void write(Transport transport, byte[] data) throws IOException, InterruptedException {
			synchronized (this) {
				long deadline = System.currentTimeMillis() + CREDIT_TIMEOUT_MS;
				while (credits == 0 && !failed) {
					long remaining = deadline - System.currentTimeMillis();
					if (remaining <= 0) {
						throw new IOException("Printer stopped accepting data (no write credit for "
								+ CREDIT_TIMEOUT_MS + " ms)");
					}
					wait(remaining);
				}
				throwIfFailed();
				credits--;
			}
			transport.write(data);
		}

		synchronized boolean awaitFinished(long timeoutMs) throws IOException, InterruptedException {
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (!finished) {
				throwIfFailed();
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					return false;
				}
				wait(remaining);
			}
			return true;
		}

		private void throwIfFailed() throws IOException {
			if (failed) {
				throw new IOException("Printer reported the print job failed (1a 0b b8)");
			}
		}
	}

	static boolean contains(byte[] data, byte[] needle) {
		outer:
		for (int i = 0; i + needle.length <= data.length; i++) {
			for (int j = 0; j < needle.length; j++) {
				if (data[i + j] != needle[j]) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}
}
