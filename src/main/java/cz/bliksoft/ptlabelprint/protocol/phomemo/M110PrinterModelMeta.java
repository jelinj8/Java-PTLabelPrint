package cz.bliksoft.ptlabelprint.protocol.phomemo;

/** Per-model capabilities for an {@code m110}-protocol model. See {@link M110PrinterModels}. */
public class M110PrinterModelMeta {

	private final String catalogId;
	private final int dpi;
	private final int printheadPixels;
	private final int densityMin;
	private final int densityMax;
	private final double leftOffsetMm;

	public M110PrinterModelMeta(String catalogId, int dpi, int printheadPixels, int densityMin, int densityMax,
			double leftOffsetMm) {
		this.catalogId = catalogId;
		this.dpi = dpi;
		this.printheadPixels = printheadPixels;
		this.densityMin = densityMin;
		this.densityMax = densityMax;
		this.leftOffsetMm = leftOffsetMm;
	}

	/**
	 * Subtracted from a job's media side gap before the image is shifted - the side gap the printer
	 * is taken to handle by itself. 0 means the job's value is applied as given; see
	 * {@link M110PrinterModels} for why the M421 is 0 although its first dot sits about 1mm in.
	 */
	public double getLeftOffsetMm() {
		return leftOffsetMm;
	}

	/** {@code printer.PrinterCatalog}'s own definition id, e.g. {@code "phomemo-m421"}. */
	public String getCatalogId() {
		return catalogId;
	}

	public int getDpi() {
		return dpi;
	}

	public int getPrintheadPixels() {
		return printheadPixels;
	}

	public int getDensityMin() {
		return densityMin;
	}

	public int getDensityMax() {
		return densityMax;
	}
}
