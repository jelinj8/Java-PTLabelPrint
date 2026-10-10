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
	private Double mediaSideGapMm;
	private double leadOffsetMm;
	private boolean leadOffsetKeepsLength = true;

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

	/**
	 * Extra side gap of the loaded media, in mm: how much further from the edge the printer aligns
	 * media to the label starts than the printer assumes by itself. {@code null} or 0 (the default)
	 * leaves the image where the printer puts it; a positive value moves it that far sideways, into
	 * the label. Like {@link #isContinuousMedia()} this describes the media, so it's set per job.
	 *
	 * <p>
	 * The case it exists for: a Phomemo M421 aligns media to the left and prints in place on stock
	 * whose label starts about 1mm in from the backing paper's edge; stock with a wider gap (a 102mm
	 * roll measured ~2.5mm) needs the difference - there, about 1.5.
	 *
	 * <p>
	 * <b>Only {@code PhomemoM110LabelPrinter} honours it so far</b>; the other families ignore it.
	 */
	public Double getMediaSideGapMm() {
		return mediaSideGapMm;
	}

	public PrintJob setMediaSideGapMm(Double mediaSideGapMm) {
		if (mediaSideGapMm != null && mediaSideGapMm < 0) {
			throw new IllegalArgumentException("Media side gap can't be negative: " + mediaSideGapMm);
		}
		this.mediaSideGapMm = mediaSideGapMm;
		return this;
	}

	/**
	 * Moves the image along the feed direction, in mm (default 0): positive starts it later (further
	 * down the label), negative earlier. "Lead", not "top": the feed runs along whichever side of the
	 * label the printer's print direction makes it (Niimbot {@code PrintDirection.LEFT} feeds along
	 * the label's width); applied after every rotation, so it always means the feed axis. For media on which the printer's own gap detection puts the
	 * start of the print slightly off - seen on a Phomemo M421 both ways, about 1mm early on one
	 * stock and slightly late on another, so like {@link #getMediaSideGapMm()} it's per job.
	 * {@link #isLeadOffsetKeepsLength()} decides what happens at the other end.
	 *
	 * <p>
	 * Honoured by {@code PhomemoM110LabelPrinter} and {@code NiimbotLabelPrinter}; the other families
	 * ignore it.
	 */
	public double getLeadOffsetMm() {
		return leadOffsetMm;
	}

	public PrintJob setLeadOffsetMm(double leadOffsetMm) {
		this.leadOffsetMm = leadOffsetMm;
		return this;
	}

	/**
	 * true (default): the job keeps the image's own length - whatever {@link #getLeadOffsetMm()} pushes
	 * past one end is cut off, and the other end is filled with blank rows. false: the job's length
	 * changes by the offset instead - longer for a positive one (blank rows added at the top, nothing
	 * cut), shorter for a negative one. A job longer than the label can run into the gap, which is
	 * why keeping the length is the default.
	 */
	public boolean isLeadOffsetKeepsLength() {
		return leadOffsetKeepsLength;
	}

	public PrintJob setLeadOffsetKeepsLength(boolean leadOffsetKeepsLength) {
		this.leadOffsetKeepsLength = leadOffsetKeepsLength;
		return this;
	}
}
