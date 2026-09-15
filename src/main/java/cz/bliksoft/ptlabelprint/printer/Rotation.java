package cz.bliksoft.ptlabelprint.printer;

/**
 * How {@link LabelPrinter#print} should orient an image before each family/model's own mandatory
 * orientation transform is applied - see {@link LabelPrinter}'s own javadoc for the full algorithm.
 */
public enum Rotation {
	/**
	 * Default. Respects the connected printer's mandatory orientation first; only rotates an
	 * additional 90 degrees on top of that if the image wouldn't otherwise fit the printhead axis,
	 * and only if that additional rotation would make it fit. Never overrides an explicit choice.
	 */
	AUTO,
	/** No additional rotation beyond the connected printer's own mandatory orientation, regardless of fit. */
	NONE,
	CW_90,
	CW_180,
	CW_270
}
