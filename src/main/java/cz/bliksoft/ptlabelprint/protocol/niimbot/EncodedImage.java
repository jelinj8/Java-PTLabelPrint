package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.Collections;
import java.util.List;

/** A label image encoded into per-row packet data. Ported from niimbluelib's {@code EncodedImage}. */
public class EncodedImage {

	private final PageColorType pageColor;
	private final int cols;
	private final int rows;
	private final List<ImageRow> rowsData;

	public EncodedImage(PageColorType pageColor, int cols, int rows, List<ImageRow> rowsData) {
		this.pageColor = pageColor;
		this.cols = cols;
		this.rows = rows;
		this.rowsData = Collections.unmodifiableList(rowsData);
	}

	public PageColorType getPageColor() {
		return pageColor;
	}

	/** Row width in pixels, padded to a multiple of 8 - must match the printer's physical printhead width. */
	public int getCols() {
		return cols;
	}

	public int getRows() {
		return rows;
	}

	public List<ImageRow> getRowsData() {
		return rowsData;
	}
}
