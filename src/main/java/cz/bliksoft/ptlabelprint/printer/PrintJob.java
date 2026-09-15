package cz.bliksoft.ptlabelprint.printer;

/**
 * The minimal, deliberately family-agnostic print options {@link LabelPrinter#print} accepts - see
 * that method's own javadoc for exactly what this does and doesn't cover, and why.
 */
public class PrintJob {

	private int copies = 1;
	private boolean continuousMedia = false;
	private Integer density;
	private Rotation rotation = Rotation.AUTO;

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

	public Rotation getRotation() {
		return rotation;
	}

	public PrintJob setRotation(Rotation rotation) {
		this.rotation = rotation;
		return this;
	}
}
