package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.IntConsumer;

import cz.bliksoft.ptlabelprint.protocol.Transport;

/**
 * Print flow for Phomemo's {@code d-series} protocol (D30/D35/D50/D110/Q30/Q30S). Ported from
 * phomymo's {@code printDSeries} (src/web/printer.js). Confirmed against real hardware (a Phomemo
 * Q30) that this protocol's BLE channel is the same one {@code BleTransport} already discovers
 * (service {@code 0xff00}, write {@code 0xff02}, notify {@code 0xff03}) - but the print flow
 * itself is <b>one-way</b>: no command here waits for or expects a response (unlike
 * {@code protocol.niimbot}'s request/response model).
 *
 * <p>
 * Matches phomymo's own write mode (write-without-response) and fixed 128-byte chunking exactly -
 * confirmed via real-hardware debugging that this is correct as-is. An early test print on a Q30
 * came out weak/striped (bands along the feed direction, like gaps between printhead elements);
 * write-with-response pacing and row-aligned chunking were both tried as fixes and each made no
 * difference, which pointed away from a data/timing bug in this class. A clean retest - this exact
 * code, unchanged, with fresh/reliable batteries in the printer - printed a clean, correctly dark
 * square. The original symptom was an unreliable power source on the test rig, not anything in the
 * protocol port.
 */
public final class DSeriesPrinter {

	private static final int CHUNK_SIZE = 128;
	private static final int CHUNK_DELAY_MS = 20;
	/** ~7mm at 203 DPI - the D30/Q30's head-to-cutter distance, needed to push continuous-tape content past the cutter. */
	private static final int CUTTER_OFFSET_ROWS = 56;

	private DSeriesPrinter() {
	}

	/**
	 * Prints {@code image} (in normal, non-rotated orientation - this method rotates it 90°CW
	 * itself, matching the D-series printhead orientation).
	 *
	 * <p>
	 * <b>{@code image}'s height (before rotation) must equal the printer's physical printhead
	 * width in dots</b> - after rotation it becomes the row width the printer's raster header
	 * declares, and confirmed on real hardware that getting this wrong (e.g. an arbitrary small
	 * test value) produces a garbled, misaligned print rather than a clean error: the printer
	 * appears to reinterpret the incoming bytes against its own fixed row width instead of the one
	 * declared. Use one of phomymo's own confirmed label sizes as a reference for a given model
	 * (its {@code D_SERIES_LABEL_SIZES}, e.g. 12mm/14mm/15mm printhead widths at 8px/mm) rather than
	 * picking a height freely.
	 *
	 * @param transport   an already-{@link Transport#connect() connect}ed transport
	 * @param image       the label content, top-to-bottom as a human would read it - see the
	 *                    height constraint above
	 * @param density     1-8, higher is darker
	 * @param continuous  true for continuous (gapless) tape, false for die-cut/gap labels
	 * @param feedDots    extra feed in dots after the cut point, only meaningful when {@code continuous}
	 * @param onProgress  called with 0-100 as data chunks are sent; may be {@code null}
	 */
	public static void print(Transport transport, RasterImage image, int density, boolean continuous, int feedDots,
			IntConsumer onProgress) throws IOException {
		RasterImage rotated = image.rotate90Clockwise();

		byte[] printData = rotated.getData();
		int printRows = rotated.getHeightLines();

		if (continuous && feedDots > 0) {
			int paddingRows = CUTTER_OFFSET_ROWS + feedDots;
			int paddingBytes = paddingRows * rotated.getWidthBytes();
			byte[] padded = Arrays.copyOf(rotated.getData(), rotated.getData().length + paddingBytes);
			printData = padded;
			printRows = rotated.getHeightLines() + paddingRows;
		}

		int heatTime = DSeriesCommands.densityToHeatTime(density);
		transport.write(DSeriesCommands.heatSettings(7, heatTime, 2));
		sleep(30);

		transport.write(DSeriesCommands.mediaType(continuous));
		sleep(30);

		transport.write(DSeriesCommands.header(rotated.getWidthBytes(), printRows));

		for (int i = 0; i < printData.length; i += CHUNK_SIZE) {
			byte[] chunk = Arrays.copyOfRange(printData, i, Math.min(i + CHUNK_SIZE, printData.length));
			transport.write(chunk);
			sleep(CHUNK_DELAY_MS);

			if (onProgress != null) {
				int sent = Math.min(i + chunk.length, printData.length);
				onProgress.accept((int) Math.round(sent * 100.0 / printData.length));
			}
		}

		sleep(100);
		transport.write(DSeriesCommands.END);
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during print", e);
		}
	}
}
