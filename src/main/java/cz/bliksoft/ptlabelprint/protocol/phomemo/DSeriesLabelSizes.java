package cz.bliksoft.ptlabelprint.protocol.phomemo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Label-size presets for Phomemo's {@code d-series} protocol, ported verbatim from phomymo's
 * {@code D_SERIES_LABEL_SIZES} (src/web/constants.js) - all in mm, {@code width} the feed-axis
 * length, {@code height} the printhead-axis width (must match the physical printhead's dot
 * capacity - see {@link DSeriesPrinter#print}'s own javadoc for why getting this wrong produces a
 * garbled print rather than a clean error). Pixel conversion uses phomymo's own {@code PX_PER_MM}
 * (8, at 203 DPI) - the same figure already implicit in this project's original hardware-confirmed
 * 12x12mm/96x96px Q30 test pattern (still this table's {@code "12x12"} entry, unchanged).
 *
 * <p>
 * phomymo also defines {@code D_SERIES_CONTINUOUS_SIZES} - a curated subset of these same 8 entries
 * (40x12, 30x12, 22x12, 40x15, 30x15) it recommends specifically for continuous-tape mode - and
 * {@code D_SERIES_ROUND_LABELS} (a single 14mm round preset). Neither is modeled as its own table
 * here: continuous vs. gapped is an independent runtime choice
 * ({@link DSeriesPrinter#print}'s own {@code continuous} parameter), not a fixed property of a
 * size, and there is no round test-pattern shape to pair the round preset with yet - documentation
 * only, matching this project's existing precedent for cataloged-but-unconsumed data (see
 * {@code PhomemoPrinterModelMeta#getLabelPresetsGroup}'s own javadoc).
 */
public final class DSeriesLabelSizes {

	private static final int PX_PER_MM = 8;

	public static final class Preset {

		private final String key;
		private final int widthMm;
		private final int heightMm;

		private Preset(String key, int widthMm, int heightMm) {
			this.key = key;
			this.widthMm = widthMm;
			this.heightMm = heightMm;
		}

		public String getKey() {
			return key;
		}

		public int getWidthMm() {
			return widthMm;
		}

		public int getHeightMm() {
			return heightMm;
		}

		/** Pre-rotation raster width in bytes - the feed-axis length, in whole bytes (8px each). */
		public int getWidthBytes() {
			return widthMm * PX_PER_MM / 8;
		}

		/** Pre-rotation raster height in lines - the printhead-axis width, must match the physical printhead's dot capacity. */
		public int getHeightLines() {
			return heightMm * PX_PER_MM;
		}
	}

	private static final List<Preset> TABLE = Arrays.asList(
			new Preset("40x12", 40, 12),
			new Preset("30x12", 30, 12),
			new Preset("22x12", 22, 12),
			new Preset("12x12", 12, 12),
			new Preset("30x14", 30, 14),
			new Preset("22x14", 22, 14),
			new Preset("40x15", 40, 15),
			new Preset("30x15", 30, 15));

	private DSeriesLabelSizes() {
	}

	public static List<Preset> all() {
		return TABLE;
	}

	public static Optional<Preset> find(String key) {
		return TABLE.stream().filter(p -> p.getKey().equals(key)).findFirst();
	}

	/** Converts a feed-axis length in mm to whole bytes at this table's {@code PX_PER_MM} - for building a custom-length test/print beyond these presets. */
	public static int lengthMmToWidthBytes(int lengthMm) {
		return lengthMm * PX_PER_MM / 8;
	}
}
