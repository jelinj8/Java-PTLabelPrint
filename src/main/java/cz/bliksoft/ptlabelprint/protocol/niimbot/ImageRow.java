package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** One row (or a repeated run of identical rows) of an {@link EncodedImage}. Ported from niimbluelib's {@code ImageRow}. */
public class ImageRow {

	public enum DataType {
		VOID,
		PIXELS,
		CHECK
	}

	private final DataType dataType;
	private final int rowNumber;
	private int repeat;
	private final int blackPixelsCount;
	private final int redPixelsCount;
	private final byte[] rowDataBlack;
	private final byte[] rowDataRed;

	public ImageRow(DataType dataType, int rowNumber, int repeat, int blackPixelsCount, int redPixelsCount,
			byte[] rowDataBlack, byte[] rowDataRed) {
		this.dataType = dataType;
		this.rowNumber = rowNumber;
		this.repeat = repeat;
		this.blackPixelsCount = blackPixelsCount;
		this.redPixelsCount = redPixelsCount;
		this.rowDataBlack = rowDataBlack;
		this.rowDataRed = rowDataRed;
	}

	public DataType getDataType() {
		return dataType;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public int getRepeat() {
		return repeat;
	}

	public void incrementRepeat() {
		repeat++;
	}

	public int getBlackPixelsCount() {
		return blackPixelsCount;
	}

	public int getRedPixelsCount() {
		return redPixelsCount;
	}

	public byte[] getRowDataBlack() {
		return rowDataBlack;
	}

	public byte[] getRowDataRed() {
		return rowDataRed;
	}
}
