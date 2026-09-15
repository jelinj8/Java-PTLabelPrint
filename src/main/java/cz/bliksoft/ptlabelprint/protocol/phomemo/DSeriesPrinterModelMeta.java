package cz.bliksoft.ptlabelprint.protocol.phomemo;

/** Per-model capabilities for a {@code d-series} model. See {@link DSeriesPrinterModels}. */
public class DSeriesPrinterModelMeta {

	private final DSeriesPrinterModel model;
	private final int dpi;
	private final int printheadPixels;
	private final int densityMin;
	private final int densityMax;

	public DSeriesPrinterModelMeta(DSeriesPrinterModel model, int dpi, int printheadPixels, int densityMin, int densityMax) {
		this.model = model;
		this.dpi = dpi;
		this.printheadPixels = printheadPixels;
		this.densityMin = densityMin;
		this.densityMax = densityMax;
	}

	public DSeriesPrinterModel getModel() {
		return model;
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
