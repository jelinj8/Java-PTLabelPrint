package cz.bliksoft.ptlabelprint.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;

class LabelPrinterBatchTest {

	/** Records the copies of every {@code print} call; fails them on demand. */
	private static final class RecordingPrinter implements LabelPrinter {
		final List<Integer> copies = new ArrayList<>();
		boolean fail;

		@Override
		public PrinterDefinition getDefinition() {
			return null;
		}

		@Override
		public void connect() {
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void print(BufferedImage image, PrintJob job) throws IOException, TimeoutException {
			copies.add(job.getCopies());
			if (fail)
				throw new TimeoutException("no answer");
		}

		@Override
		public PrinterCapabilities getCapabilities() {
			return null;
		}
	}

	private static BufferedImage image() {
		return new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
	}

	@Test
	void identicalImagesCollapseIntoCopies() throws Exception {
		RecordingPrinter printer = new RecordingPrinter();
		BufferedImage a = image();
		BufferedImage b = image();
		PrintJob job = new PrintJob();

		printer.printBatch(Arrays.asList(a, a, a, b), job);

		assertEquals(Arrays.asList(3, 1), printer.copies);
		assertEquals(1, job.getCopies());
	}

	@Test
	void copiesAreRestoredAfterAFailedPrint() {
		// A caller that prints the same job again after a failure must get the same
		// number of labels, not the square of it.
		RecordingPrinter printer = new RecordingPrinter();
		printer.fail = true;
		BufferedImage a = image();
		PrintJob job = new PrintJob();

		assertThrows(TimeoutException.class, () -> printer.printBatch(Arrays.asList(a, a, a), job));
		assertEquals(1, job.getCopies());

		assertThrows(TimeoutException.class, () -> printer.printBatch(Arrays.asList(a, a, a), job));
		assertEquals(Arrays.asList(3, 3), printer.copies);
	}
}
