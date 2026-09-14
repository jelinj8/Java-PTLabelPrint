package cz.bliksoft.ptlabelprint.protocol.phomemo;

/** Per-model catalog data, ported from phomymo's {@code printers.json}. See {@link PhomemoPrinterModels}. */
public class PhomemoPrinterModelMeta {

	private final PhomemoPrinterModel model;
	private final String name;
	private final String protocolTag;
	private final Integer widthBytes;
	private final int dpi;
	private final PhomemoAlignment alignment;
	private final boolean rotated;
	private final boolean tape;
	private final int[] tapeWidths;
	private final Integer defaultTapeWidth;
	private final String labelPresetsGroup;

	public PhomemoPrinterModelMeta(PhomemoPrinterModel model, String name, String protocolTag, Integer widthBytes,
			int dpi, PhomemoAlignment alignment, boolean rotated, boolean tape, int[] tapeWidths,
			Integer defaultTapeWidth, String labelPresetsGroup) {
		this.model = model;
		this.name = name;
		this.protocolTag = protocolTag;
		this.widthBytes = widthBytes;
		this.dpi = dpi;
		this.alignment = alignment;
		this.rotated = rotated;
		this.tape = tape;
		this.tapeWidths = tapeWidths;
		this.defaultTapeWidth = defaultTapeWidth;
		this.labelPresetsGroup = labelPresetsGroup;
	}

	public PhomemoPrinterModel getModel() {
		return model;
	}

	public String getName() {
		return name;
	}

	/** phomymo's {@code protocol} tag for this model, e.g. {@code "m02"}/{@code "tspl"} - none of these 6 tags have print-flow code in this project yet. */
	public String getProtocolTag() {
		return protocolTag;
	}

	/** Fixed printer paper width in bytes, or {@code null} if computed from label size rather than fixed (matches {@code d-series}, though that protocol isn't cataloged in this table - see {@link PhomemoPrinterModel}). */
	public Integer getWidthBytes() {
		return widthBytes;
	}

	public int getDpi() {
		return dpi;
	}

	public PhomemoAlignment getAlignment() {
		return alignment;
	}

	/** Whether the raster image is rotated 90 degrees before sending, like {@code d-series}/{@code p12} - {@code false} for every other tag. */
	public boolean isRotated() {
		return rotated;
	}

	public boolean isTape() {
		return tape;
	}

	/** Supported tape widths in mm, or {@code null} if this model isn't tape-based. */
	public int[] getTapeWidths() {
		return tapeWidths;
	}

	public Integer getDefaultTapeWidth() {
		return defaultTapeWidth;
	}

	/**
	 * Which of phomymo's label-size preset groups (mm dimensions - {@code d-series}, {@code m-series},
	 * {@code tape}, {@code pm241}) this model draws from. Documentation only in this project so far -
	 * not modeled as its own class/table, since nothing consumes it until a real print flow exists
	 * for one of these 6 tags (mirrors this project's existing {@code .image} package-info-only
	 * precedent for other not-yet-built pipelines). The mm values themselves:
	 * <ul>
	 * <li>{@code d-series}: 40x12, 30x12, 22x12, 12x12, 30x14, 22x14, 40x15, 30x15 (+ continuous
	 * variants 40x12/30x12/22x12/40x15/30x15 + a 14mm round label)</li>
	 * <li>{@code m-series}: 12x40, 15x30, 20x30, 25x50, 30x20, 30x40, 40x30, 40x60, 50x25, 50x30,
	 * 50x80, 60x40 (+ round labels at 20/30/40/50mm diameter)</li>
	 * <li>{@code tape}: 40x12, 30x12, 22x12, 12x12 (at 12mm tape) / 40x14, 30x14, 22x14, 14x14 (at
	 * 14mm) / 40x15, 30x15, 22x15, 15x15 (at 15mm)</li>
	 * <li>{@code pm241}: 102x152, 102x102, 102x76, 102x51, 100x150, 100x100</li>
	 * </ul>
	 */
	public String getLabelPresetsGroup() {
		return labelPresetsGroup;
	}
}
