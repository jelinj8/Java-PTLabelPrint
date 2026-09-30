package cz.bliksoft.ptlabelprint.printer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import cz.bliksoft.ptlabelprint.protocol.Transport;

class PhomemoM110LabelPrinterTest {

	private static final byte[] FOOTER = {0x1f, (byte) 0xf0, 0x05, 0x00, 0x1f, (byte) 0xf0, 0x03, 0x00};

	/**
	 * Records everything written, as one stream, and - like the real M421 - reports the job finished
	 * once the footer arrives, so print() doesn't sit out its completion timeout.
	 */
	private static final class RecordingTransport implements Transport {
		final ByteArrayOutputStream written = new ByteArrayOutputStream();
		private Consumer<byte[]> listener;
		boolean failJob;

		@Override
		public void connect() {
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void write(byte[] data) {
			written.write(data, 0, data.length);
			if (Arrays.equals(data, FOOTER) && listener != null) {
				listener.accept(failJob ? new byte[] {0x1a, 0x0b, (byte) 0xb8} : new byte[] {0x1a, 0x0f, 0x0c});
			}
		}

		@Override
		public void writeStream(byte[] data, int chunkSize, long chunkDelayMs) {
			write(data);
		}

		@Override
		public void setRawDataListener(Consumer<byte[]> listener) {
			this.listener = listener;
		}
	}

	@Test
	void failedJobIsReportedAsAnException() {
		RecordingTransport transport = new RecordingTransport();
		transport.failJob = true;

		IOException e = assertThrows(IOException.class, () -> m421(transport).print(solid(320, 16), new PrintJob()));
		assertTrue(e.getMessage().contains("failed"));
	}

	/**
	 * Like the real M421: announces 7 write credits and a 182-byte maximum write on connect, then
	 * hands a credit back per write taken - unless {@code full}, when it withholds them the way a
	 * printer with a full receive buffer does.
	 */
	private static final class CreditingTransport implements Transport {
		final List<byte[]> writes = new java.util.concurrent.CopyOnWriteArrayList<>();
		private volatile Consumer<byte[]> listener;
		private boolean full;
		private int withheld;

		CreditingTransport(boolean full) {
			this.full = full;
		}

		@Override
		public void connect() {
			listener.accept(new byte[] {0x01, 0x07});
			listener.accept(new byte[] {0x02, (byte) 0xb6, 0x00});
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public synchronized void write(byte[] data) {
			writes.add(data);
			if (Arrays.equals(data, FOOTER)) {
				listener.accept(new byte[] {0x1a, 0x0f, 0x0c});
			} else if (full) {
				withheld++;
			} else {
				listener.accept(new byte[] {0x01, 0x01});
			}
		}

		synchronized void drain() {
			full = false;
			for (; withheld > 0; withheld--) {
				listener.accept(new byte[] {0x01, 0x01});
			}
		}

		@Override
		public void setRawDataListener(Consumer<byte[]> listener) {
			this.listener = listener;
		}
	}

	@Test
	void creditedPrinterGetsFullSizeWritesWithNoFixedDelay() throws Exception {
		CreditingTransport transport = new CreditingTransport(false);
		LabelPrinter printer = m421(transport);
		printer.connect();

		long start = System.currentTimeMillis();
		printer.print(solid(800, 400), new PrintJob());
		long elapsed = System.currentTimeMillis() - start;

		// 100 bytes x 400 rows = 40000 bytes = 220 writes of up to 182 bytes, after 4 commands, before the footer
		assertEquals(4 + 220 + 1, transport.writes.size());
		assertEquals(182, transport.writes.get(4).length);
		// phomymo's pacing would need 313 chunks x 20ms = over 6s
		assertTrue(elapsed < 2000, "took " + elapsed + " ms");
	}

	@Test
	void creditedSendStallsAtTheWindowWhenCreditsAreWithheld() throws Exception {
		CreditingTransport transport = new CreditingTransport(true);
		LabelPrinter printer = m421(transport);
		printer.connect();

		Thread printing = new Thread(() -> {
			try {
				printer.print(solid(800, 400), new PrintJob());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		printing.start();
		Thread.sleep(500);
		assertEquals(7, transport.writes.size());

		transport.drain();
		printing.join(10000);
		assertEquals(4 + 220 + 1, transport.writes.size());
	}

	private static LabelPrinter m421(Transport transport) {
		List<PrinterDefinition> detected = PrinterCatalog.detect("M421");
		assertEquals(1, detected.size());
		assertEquals(PrinterFamily.PHOMEMO_M110, detected.get(0).getFamily());
		return PrinterFactory.create(detected.get(0), transport);
	}

	private static BufferedImage solid(int width, int height) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return img;
	}

	@Test
	void m421IsDetectedAndReportsCapabilities() {
		LabelPrinter printer = m421(new RecordingTransport());
		assertTrue(printer instanceof PhomemoM110LabelPrinter);
		assertEquals(203, printer.getCapabilities().getDpi());
		assertEquals(912, printer.getCapabilities().getPrintheadPixels());
	}

	@Test
	void printsImageAtItsOwnWidthUnrotated() throws Exception {
		RecordingTransport transport = new RecordingTransport();
		m421(transport).print(solid(320, 160), new PrintJob());

		byte[] out = transport.written.toByteArray();
		byte[] expectedPrefix = {
				0x1b, 0x4e, 0x0d, 5,
				0x1b, 0x4e, 0x04, 10,
				0x1f, 0x11, 0x0a,
				0x1d, 0x76, 0x30, 0x00, 40, 0, (byte) 160, 0,
		};
		assertArrayEquals(expectedPrefix, Arrays.copyOfRange(out, 0, expectedPrefix.length));
		assertEquals(expectedPrefix.length + 40 * 160 + 8, out.length);
		assertEquals((byte) 0xff, out[expectedPrefix.length]);
		assertArrayEquals(FOOTER, Arrays.copyOfRange(out, out.length - 8, out.length));
	}

	/** On the M421 the gap is applied as given: 1mm at 203 DPI is 8 dots further right. */
	@Test
	void mediaSideGapMovesTheImageRight() throws Exception {
		RecordingTransport gap = new RecordingTransport();
		m421(gap).print(solid(320, 4), new PrintJob().setMediaSideGapMm(1.0));
		byte[] out = gap.written.toByteArray();
		assertEquals(41, out[15] & 0xff);
		assertEquals(0x00, out[19]);
		assertEquals((byte) 0xff, out[20]);

		for (Double none : new Double[] {null, 0.0}) {
			RecordingTransport unshifted = new RecordingTransport();
			m421(unshifted).print(solid(320, 4), new PrintJob().setMediaSideGapMm(none));
			out = unshifted.written.toByteArray();
			assertEquals(40, out[15] & 0xff);
			assertEquals((byte) 0xff, out[19]);
		}
	}

	/** Row {@code y} of a job's raster as sent, for an 8px-wide (1 byte per row) image. */
	private static int[] rows(RecordingTransport transport) {
		byte[] out = transport.written.toByteArray();
		int height = (out[17] & 0xff) | ((out[18] & 0xff) << 8);
		int[] rows = new int[height];
		for (int y = 0; y < height; y++) {
			rows[y] = out[19 + y] & 0xff;
		}
		return rows;
	}

	/** 8px wide, 24 rows: only row 0 and row 23 are black - so both ends of the image are recognisable. */
	private static BufferedImage endMarkers() {
		BufferedImage img = new BufferedImage(8, 24, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 8, 24);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 8, 1);
		g.fillRect(0, 23, 8, 1);
		g.dispose();
		return img;
	}

	@Test
	void positiveTopOffsetKeepingLengthShiftsDownAndCutsTheBottom() throws Exception {
		RecordingTransport transport = new RecordingTransport();
		// 1mm at 203 DPI = 8 rows
		m421(transport).print(endMarkers(), new PrintJob().setTopOffsetMm(1));

		int[] rows = rows(transport);
		assertEquals(24, rows.length);
		assertEquals(0x00, rows[0]);
		assertEquals(0xff, rows[8]);
		assertEquals(0x00, rows[23]); // the bottom marker was pushed off the end
	}

	@Test
	void positiveTopOffsetChangingLengthKeepsTheBottom() throws Exception {
		RecordingTransport transport = new RecordingTransport();
		m421(transport).print(endMarkers(), new PrintJob().setTopOffsetMm(1).setTopOffsetKeepsLength(false));

		int[] rows = rows(transport);
		assertEquals(32, rows.length);
		assertEquals(0xff, rows[8]);
		assertEquals(0xff, rows[31]);
	}

	@Test
	void negativeTopOffsetShiftsUp() throws Exception {
		RecordingTransport keeping = new RecordingTransport();
		m421(keeping).print(endMarkers(), new PrintJob().setTopOffsetMm(-1));
		int[] rows = rows(keeping);
		assertEquals(24, rows.length);
		assertEquals(0x00, rows[0]); // the top marker was pushed off the start
		assertEquals(0xff, rows[15]);
		assertEquals(0x00, rows[23]);

		RecordingTransport changing = new RecordingTransport();
		m421(changing).print(endMarkers(), new PrintJob().setTopOffsetMm(-1).setTopOffsetKeepsLength(false));
		rows = rows(changing);
		assertEquals(16, rows.length);
		assertEquals(0xff, rows[15]);
	}

	@Test
	void imageWiderThanPrintheadIsCropped() throws Exception {
		RecordingTransport transport = new RecordingTransport();
		m421(transport).print(solid(1000, 8), new PrintJob());

		byte[] out = transport.written.toByteArray();
		// 912px -> 114 bytes per row
		assertEquals(114, out[15] & 0xff);
		assertEquals(8, out[17] & 0xff);
	}
}
