package cz.bliksoft.ptlabelprint.image;

/**
 * A connected printer's physical/protocol characteristics, exposed uniformly across families via
 * {@code printer.LabelPrinter#getCapabilities()}.
 */
public class PrinterCapabilities {

	private final int dpi;
	private final int printheadPixels;
	private final int densityMin;
	private final int densityMax;

	public PrinterCapabilities(int dpi, int printheadPixels, int densityMin, int densityMax) {
		this.dpi = dpi;
		this.printheadPixels = printheadPixels;
		this.densityMin = densityMin;
		this.densityMax = densityMax;
	}

	public int getDpi() {
		return dpi;
	}

	/** The printhead's fixed physical dot-width - the axis a print's content must fit after rotation/canvas-resize. */
	public int getPrintheadPixels() {
		return printheadPixels;
	}

	public int getDensityMin() {
		return densityMin;
	}

	public int getDensityMax() {
		return densityMax;
	}

	@Override
	public String toString() {
		return "PrinterCapabilities{dpi=" + dpi + ", printheadPixels=" + printheadPixels + ", densityMin=" + densityMin
				+ ", densityMax=" + densityMax + '}';
	}
}
