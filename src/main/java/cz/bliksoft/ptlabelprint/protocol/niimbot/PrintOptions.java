package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Print options shared by every {@link AbstractNiimbotPrintTask} subclass. Ported from niimbluelib's {@code PrintOptions} - {@link #getTubeType()}/{@link #getTubeWidthMm()}/{@link #getHalfCut()} are only consumed by {@link D110V4PrintTask}, matching niimbluelib's own {@code D110MV4PrintTask}-only usage. */
public class PrintOptions {

	private LabelType labelType = LabelType.WITH_GAPS;
	private int density = 3;
	private int totalPages = 1;
	private long statusPollIntervalMs = 300;
	private long statusTimeoutMs = 5_000;
	private long pageTimeoutMs = 10_000;
	private int speed = 1;
	private PageColorType pageColor = PageColorType.SINGLE_COLOR;
	private int cutType = 0;
	private int cutHeight = 0;
	private Integer tubeType;
	private Double tubeWidthMm;
	private Boolean halfCut;

	public LabelType getLabelType() {
		return labelType;
	}

	public PrintOptions setLabelType(LabelType labelType) {
		this.labelType = labelType;
		return this;
	}

	public int getDensity() {
		return density;
	}

	public PrintOptions setDensity(int density) {
		this.density = density;
		return this;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public PrintOptions setTotalPages(int totalPages) {
		this.totalPages = totalPages;
		return this;
	}

	public long getStatusPollIntervalMs() {
		return statusPollIntervalMs;
	}

	public long getStatusTimeoutMs() {
		return statusTimeoutMs;
	}

	public long getPageTimeoutMs() {
		return pageTimeoutMs;
	}

	public int getSpeed() {
		return speed;
	}

	public PrintOptions setSpeed(int speed) {
		this.speed = speed;
		return this;
	}

	public PageColorType getPageColor() {
		return pageColor;
	}

	public int getCutType() {
		return cutType;
	}

	public int getCutHeight() {
		return cutHeight;
	}

	/** Shrink-tube type, or {@code null} if not printing on shrink tube. Only used by {@link D110V4PrintTask} - requires {@link #getLabelType()} to be {@link LabelType#CONTINUOUS}. */
	public Integer getTubeType() {
		return tubeType;
	}

	public PrintOptions setTubeType(Integer tubeType) {
		this.tubeType = tubeType;
		return this;
	}

	/** Shrink-tube width in mm, or {@code null} if not printing on shrink tube. */
	public Double getTubeWidthMm() {
		return tubeWidthMm;
	}

	public PrintOptions setTubeWidthMm(Double tubeWidthMm) {
		this.tubeWidthMm = tubeWidthMm;
		return this;
	}

	/** Whether to half-cut each label, or {@code null} to leave the printer's own default. Only used by {@link D110V4PrintTask}. */
	public Boolean getHalfCut() {
		return halfCut;
	}

	public PrintOptions setHalfCut(Boolean halfCut) {
		this.halfCut = halfCut;
		return this;
	}
}
