package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Loaded-paper geometry, read via {@link RequestCommandId#GET_PAPER_INFO}. Ported from
 * niimbluelib's {@code PaperInfo}/{@code getPaperInfo}. All fields beyond {@link #isValid()} are
 * {@code null} unless the printer's response has the one recognized 18-byte length - niimbluelib's
 * own comment notes shorter responses are seen too, with an as-yet-unknown layout, so those are
 * left unparsed here rather than guessed at.
 */
public class PaperInfo {

	private boolean valid;
	private Integer gapHeightPixel;
	private Integer totalHeightPixel;
	private LabelType paperType;
	private Double gapHeight;
	private Double totalHeight;
	private Integer paperWidthPixel;
	private Double paperWidth;
	private Integer paperHeightPixel;
	private Double paperHeight;
	private Integer direction;
	private Integer tailLengthPixel;
	private Double tailLength;

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public Integer getGapHeightPixel() {
		return gapHeightPixel;
	}

	public void setGapHeightPixel(Integer gapHeightPixel) {
		this.gapHeightPixel = gapHeightPixel;
	}

	public Integer getTotalHeightPixel() {
		return totalHeightPixel;
	}

	public void setTotalHeightPixel(Integer totalHeightPixel) {
		this.totalHeightPixel = totalHeightPixel;
	}

	public LabelType getPaperType() {
		return paperType;
	}

	public void setPaperType(LabelType paperType) {
		this.paperType = paperType;
	}

	public Double getGapHeight() {
		return gapHeight;
	}

	public void setGapHeight(Double gapHeight) {
		this.gapHeight = gapHeight;
	}

	public Double getTotalHeight() {
		return totalHeight;
	}

	public void setTotalHeight(Double totalHeight) {
		this.totalHeight = totalHeight;
	}

	public Integer getPaperWidthPixel() {
		return paperWidthPixel;
	}

	public void setPaperWidthPixel(Integer paperWidthPixel) {
		this.paperWidthPixel = paperWidthPixel;
	}

	public Double getPaperWidth() {
		return paperWidth;
	}

	public void setPaperWidth(Double paperWidth) {
		this.paperWidth = paperWidth;
	}

	public Integer getPaperHeightPixel() {
		return paperHeightPixel;
	}

	public void setPaperHeightPixel(Integer paperHeightPixel) {
		this.paperHeightPixel = paperHeightPixel;
	}

	public Double getPaperHeight() {
		return paperHeight;
	}

	public void setPaperHeight(Double paperHeight) {
		this.paperHeight = paperHeight;
	}

	public Integer getDirection() {
		return direction;
	}

	public void setDirection(Integer direction) {
		this.direction = direction;
	}

	public Integer getTailLengthPixel() {
		return tailLengthPixel;
	}

	public void setTailLengthPixel(Integer tailLengthPixel) {
		this.tailLengthPixel = tailLengthPixel;
	}

	public Double getTailLength() {
		return tailLength;
	}

	public void setTailLength(Double tailLength) {
		this.tailLength = tailLength;
	}

	@Override
	public String toString() {
		return "PaperInfo{" + "valid=" + valid + ", gapHeightPixel=" + gapHeightPixel + ", totalHeightPixel="
				+ totalHeightPixel + ", paperType=" + paperType + ", gapHeight=" + gapHeight + ", totalHeight="
				+ totalHeight + ", paperWidthPixel=" + paperWidthPixel + ", paperWidth=" + paperWidth
				+ ", paperHeightPixel=" + paperHeightPixel + ", paperHeight=" + paperHeight + ", direction="
				+ direction + ", tailLengthPixel=" + tailLengthPixel + ", tailLength=" + tailLength + '}';
	}
}
