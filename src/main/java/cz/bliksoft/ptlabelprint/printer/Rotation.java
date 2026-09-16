package cz.bliksoft.ptlabelprint.printer;

/**
 * How {@link LabelPrinter#print} should orient an image before each family/model's own mandatory
 * orientation transform is applied - see {@link LabelPrinter}'s own javadoc for the full algorithm.
 */
public enum Rotation {
	/**
	 * No additional rotation beyond the connected printer's own mandatory orientation, regardless of
	 * fit - any excess on the printhead axis is left for the family's own pipeline to crop. This is
	 * {@link PrintJob}'s default.
	 *
	 * <p>
	 * <b>Not {@link #AUTO}</b> - deliberately. Confirmed on real hardware (a Niimbot M2, {@code TOP}
	 * print direction, no mandatory rotation of its own): gapped/die-cut media has *both* axes fixed
	 * by the physical label stock, not just the printhead-width one. A label a few pixels too wide
	 * for the printhead (e.g. 50mm content on a printhead whose real capacity is ~48.8mm) is a minor,
	 * croppable overage - but {@link #AUTO} would "fix" it by rotating 90 degrees, which doesn't
	 * remove the overage, it just moves it to the *other* axis (the feed/length direction) - one
	 * that's equally fixed by the die-cut length on gapped media, and now visibly mis-oriented too.
	 * Rotating to satisfy a printhead-width mismatch is only actually safe for continuous media, where
	 * the feed axis is unconstrained - {@link #AUTO} must be requested explicitly, per print, not
	 * assumed as a safe default across both media kinds.
	 */
	NONE,
	/**
	 * Respects the connected printer's mandatory orientation first; only rotates an additional 90
	 * degrees on top of that if the image wouldn't otherwise fit the printhead axis, and only if that
	 * additional rotation would make it fit. Must be requested explicitly - see {@link #NONE}'s own
	 * javadoc for why this isn't the default.
	 */
	AUTO,
	CW_90,
	CW_180,
	CW_270
}
