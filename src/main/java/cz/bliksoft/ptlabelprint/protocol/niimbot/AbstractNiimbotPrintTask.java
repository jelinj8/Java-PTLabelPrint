package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Shared plumbing for the {@code cz.bliksoft.ptlabelprint.protocol.niimbot} print-task classes -
 * ported from niimbluelib's own {@code AbstractPrintTask} (src/print_tasks/AbstractPrintTask.ts).
 * Concrete subclasses ({@link D110V4PrintTask}, {@link B1PrintTask}, and the rest of
 * {@link NiimbotPrintTasks}' dispatch table) share {@link #validatePage}, plus the concrete
 * defaults below for {@link #waitForFinished()}/{@link #printEnd()}/{@link #isSupportColor}
 * (confirmed identical across {@code D110V4PrintTask}/{@code B1PrintTask} before this class
 * existed - hoisted here as a pure refactor, not a behavior change) - all overridable, since a
 * newly-ported task's real behavior may differ. {@link #printInit()}/{@link #printPage} stay
 * abstract: this is exactly where real per-model packet-sequence differences live.
 */
public abstract class AbstractNiimbotPrintTask {

	protected final NiimbotDevice device;
	protected final PrintOptions options;
	private int pagesPrinted;

	protected AbstractNiimbotPrintTask(NiimbotDevice device, PrintOptions options) {
		this.device = device;
		this.options = options;
	}

	/**
	 * Checks page color support/match against this task's options and that {@code totalPages}
	 * isn't exceeded, then increments the internal page counter. Ported verbatim from
	 * {@code AbstractPrintTask.ts}'s {@code validatePage}.
	 */
	protected final void validatePage(EncodedImage image, int quantity) {
		if (pagesPrinted + quantity > options.getTotalPages()) {
			throw new IllegalStateException("Trying to print too many pages (totalPages may not be set correctly)");
		}
		if (!isSupportColor(image.getPageColor())) {
			throw new IllegalArgumentException("Page color " + image.getPageColor() + " is not supported by this print task");
		}
		if (options.getPageColor() != image.getPageColor()) {
			throw new IllegalArgumentException(
					"Page color " + image.getPageColor() + " does not match print task color " + options.getPageColor());
		}
		pagesPrinted += quantity;
	}

	/** Pages successfully validated/sent so far - used by tasks (e.g. {@code D110PrintTask}) whose per-page wait target is "however many pages have actually been sent", not the configured {@code totalPages}. */
	protected int getPagesPrinted() {
		return pagesPrinted;
	}

	/** Printer's printhead resolution in pixels, looked up from the connected model's metadata. */
	protected int printheadPixels() {
		return device.getModelMetadata().map(PrinterModelMeta::getPrintheadPixels).orElse(0);
	}

	/**
	 * Default no-op, matching {@code AbstractPrintTask.ts}. Most tasks synchronize entirely in
	 * {@link #waitForFinished()} and never need this; a few (e.g. {@code D110PrintTask},
	 * {@code B21L2BPrintTask}) override it as their real per-page wait step and call it themselves
	 * between {@link #printPage} calls in a per-page print loop - see each such override's javadoc.
	 */
	public void waitForPageFinished() throws IOException, TimeoutException {
	}

	/** Sets label type/density and sends the print-start command - per-model packet sequence, no shared default. */
	public abstract void printInit() throws IOException, TimeoutException;

	/** Prints one page - per-model packet sequence, no shared default. */
	public abstract void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException;

	/** Polls print status until all pages report finished. */
	public void waitForFinished() throws IOException, TimeoutException {
		device.setPacketTimeout(options.getStatusTimeoutMs());
		try {
			device.waitUntilPrintFinishedByStatusPoll(options.getTotalPages(), options.getStatusPollIntervalMs(),
					options.getStatusTimeoutMs() * options.getTotalPages() + 10_000);
		} finally {
			device.setPacketTimeout(1000);
		}
	}

	/** Ends the print job. Default: plain {@code PrintEnd}, no quirks - override for a model-specific one. */
	public boolean printEnd() throws IOException, TimeoutException {
		return device.printEnd();
	}

	/** Default: single-color pages only, matching {@code AbstractPrintTask.ts}'s own conservative default. */
	public boolean isSupportColor(PageColorType pageColor) {
		return pageColor == PageColorType.SINGLE_COLOR;
	}
}
