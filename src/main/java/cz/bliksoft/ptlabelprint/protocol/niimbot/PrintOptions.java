package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Print options for {@link D110V4PrintTask}. Ported from niimbluelib's {@code PrintOptions}, trimmed to the fields that print task actually uses. */
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
}
