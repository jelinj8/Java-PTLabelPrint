package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.Collections;
import java.util.List;

/** Per-model capabilities. Ported from niimbluelib's {@code PrinterModelMeta}. */
public class PrinterModelMeta {

	private final PrinterModel model;
	private final int[] ids;
	private final int dpi;
	private final PrintDirection printDirection;
	private final int printheadPixels;
	private final List<LabelType> paperTypes;
	private final int densityMin;
	private final int densityMax;
	private final int densityDefault;

	public PrinterModelMeta(PrinterModel model, int[] ids, int dpi, PrintDirection printDirection,
			int printheadPixels, List<LabelType> paperTypes, int densityMin, int densityMax, int densityDefault) {
		this.model = model;
		this.ids = ids;
		this.dpi = dpi;
		this.printDirection = printDirection;
		this.printheadPixels = printheadPixels;
		this.paperTypes = Collections.unmodifiableList(paperTypes);
		this.densityMin = densityMin;
		this.densityMax = densityMax;
		this.densityDefault = densityDefault;
	}

	public PrinterModel getModel() {
		return model;
	}

	/** A model can report more than one ID (hardware revisions); this is every ID known to map to it. */
	public int[] getIds() {
		return ids;
	}

	public int getDpi() {
		return dpi;
	}

	public PrintDirection getPrintDirection() {
		return printDirection;
	}

	public int getPrintheadPixels() {
		return printheadPixels;
	}

	public List<LabelType> getPaperTypes() {
		return paperTypes;
	}

	public int getDensityMin() {
		return densityMin;
	}

	public int getDensityMax() {
		return densityMax;
	}

	public int getDensityDefault() {
		return densityDefault;
	}
}
