package cz.bliksoft.ptlabelprint.printer;

/**
 * The minimal, deliberately family-agnostic print options {@link LabelPrinter#print} accepts - see
 * that method's own javadoc for exactly what this does and doesn't cover, and why.
 */
public class PrintJob {

	private int copies = 1;
	private boolean continuousMedia = false;
	private Integer density;
	/** {@link Rotation#NONE}, not {@link Rotation#AUTO} - see {@link Rotation#NONE}'s own javadoc for
	 *  why the auto-fit-rotate behavior must be requested explicitly, not assumed by default. */
	private Rotation rotation = Rotation.NONE;

	public int getCopies() {
		return copies;
	}

	public PrintJob setCopies(int copies) {
		this.copies = copies;
		return this;
	}

	/** false (default) = gapped/die-cut labels; true = continuous tape. */
	public boolean isContinuousMedia() {
		return continuousMedia;
	}

	public PrintJob setContinuousMedia(boolean continuousMedia) {
		this.continuousMedia = continuousMedia;
		return this;
	}

	/**
	 * {@code null} (default) means "use this family's own sensible default". A non-null value is in
	 * whichever family-native scale applies to the connected printer (no cross-family
	 * normalization - see {@link LabelPrinter#print}'s own javadoc) and is validated against that
	 * family's real range at print time, throwing a clear exception naming the valid range if out of
	 * bounds.
	 */
	public Integer getDensity() {
		return density;
	}

	public PrintJob setDensity(Integer density) {
		this.density = density;
		return this;
	}

	/** {@link Rotation#NONE} (default) unless explicitly set otherwise - see {@link Rotation}'s own javadoc. */
	public Rotation getRotation() {
		return rotation;
	}

	public PrintJob setRotation(Rotation rotation) {
		this.rotation = rotation;
		return this;
	}
}
