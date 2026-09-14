package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link PrinterModelMeta} entries, looked up by the ID reported via
 * {@link PrinterInfoType#PRINTER_MODEL_ID}. Full port (77 models) of niimbluelib's
 * {@code modelsLibrary} / {@code getPrinterMetaById} (src/printer_models.ts) - every model
 * niimbluelib itself ships metadata for (see {@link PrinterModel}'s javadoc for the 3 enum members
 * niimbluelib declares but has no metadata for, deliberately not ported here).
 *
 * <p>
 * A Phomemo Q30 is <b>not</b> in this table, and never will need to be: confirmed against real
 * hardware (see the project's CLAUDE.md "Status" section) that the Q30 doesn't speak this protocol
 * at all - it and the rest of Phomemo's D-series speak phomymo's own ESC/POS-derived
 * {@code d-series} protocol instead, which belongs in a separate
 * {@code cz.bliksoft.ptlabelprint.protocol.phomemo} family, not here. (An earlier version of this
 * project's docs incorrectly assumed "similar to D30" in phomymo's README meant Niimbot-protocol
 * compatibility - it didn't; that assumption has been corrected.) {@link #findById(int)} returns
 * empty for any unrecognized ID - callers must handle that gracefully rather than assuming every
 * connected printer resolves to a known entry.
 *
 * <p>
 * <b>Known ID collisions</b> (verbatim from niimbluelib's own vendor data, not a bug in this port):
 * id {@code 512} (D11 &amp; HI_NB_D11), id {@code 2305} (D110 &amp; HI_D110), and id {@code 256}
 * (A8, B3S, JCB3S) each map to more than one model. {@link #findById(int)} returns the first table
 * match for a colliding ID - table order therefore matters for those three IDs specifically, same
 * as it does in niimbluelib's own {@code getPrinterMetaById}.
 */
public final class PrinterModels {

	private static final List<PrinterModelMeta> TABLE = Arrays.asList(
			new PrinterModelMeta(PrinterModel.A1_PRO, new int[] {7424}, 300, PrintDirection.LEFT, 296,
					Arrays.asList(LabelType.PERFORATED, LabelType.CONTINUOUS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.A20, new int[] {2817}, 203, PrintDirection.TOP, 400,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.A203, new int[] {2818}, 203, PrintDirection.TOP, 400,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.A63, new int[] {2054}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK), 1, 15, 10),
			new PrinterModelMeta(PrinterModel.A8, new int[] {256}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.BLACK, LabelType.WITH_GAPS, LabelType.CONTINUOUS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.A8_P, new int[] {273}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B1, new int[] {4096}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B1_PRO, new int[] {4097}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B1_SE, new int[] {4098}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B11, new int[] {51457}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED,
							LabelType.TRANSPARENT),
					6, 15, 10),
			new PrinterModelMeta(PrinterModel.B16, new int[] {1792}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.B18, new int[] {3584}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK_MARK_GAP,
							LabelType.HEAT_SHRINK_TUBE, LabelType.CONTINUOUS),
					1, 3, 2),
			new PrinterModelMeta(PrinterModel.B18S, new int[] {3585}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK_MARK_GAP,
							LabelType.HEAT_SHRINK_TUBE, LabelType.CONTINUOUS),
					1, 3, 2),
			new PrinterModelMeta(PrinterModel.B2, new int[] {6913}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B2_PRO, new int[] {6912}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B203, new int[] {2816}, 203, PrintDirection.TOP, 400,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B21, new int[] {768}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B21_PRO, new int[] {785}, 300, PrintDirection.TOP, 591,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B21_C2B, new int[] {771, 775}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.CONTINUOUS, LabelType.TRANSPARENT, LabelType.BLACK), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B21_L2B, new int[] {769}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B21S, new int[] {777}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B21S_C2B, new int[] {776}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B3, new int[] {52993}, 203, PrintDirection.TOP, 600,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B31, new int[] {5632}, 203, PrintDirection.TOP, 600,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B32, new int[] {2049}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 15, 10),
			new PrinterModelMeta(PrinterModel.B32R, new int[] {2050}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS), 1, 15, 10),
			new PrinterModelMeta(PrinterModel.B3S, new int[] {256, 260, 262}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B3S_A, new int[] {275}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B3S_P, new int[] {272}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 3),
			new PrinterModelMeta(PrinterModel.B4, new int[] {6656}, 203, PrintDirection.TOP, 832,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B4_PRO, new int[] {6657}, 300, PrintDirection.TOP, 1229,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.B50, new int[] {51713}, 203, PrintDirection.TOP, 400,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.B50W, new int[] {51714}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.BETTY, new int[] {2561}, 203, PrintDirection.LEFT, 192,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.C1, new int[] {5120}, 300, PrintDirection.LEFT, 178,
					Arrays.asList(LabelType.CONTINUOUS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D101, new int[] {2560}, 203, PrintDirection.LEFT, 192,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D11, new int[] {512}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D11_H, new int[] {528}, 300, PrintDirection.LEFT, 142,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D11_PRO, new int[] {531}, 300, PrintDirection.LEFT, 142,
					Arrays.asList(LabelType.TRANSPARENT, LabelType.WITH_GAPS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D110, new int[] {2304, 2305}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.D110_M, new int[] {2320}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.D11S, new int[] {514}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.EP1C, new int[] {5121}, 300, PrintDirection.LEFT, 178,
					Arrays.asList(LabelType.CONTINUOUS), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.EP2M_H, new int[] {4610}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK, LabelType.BLACK_MARK_GAP),
					1, 5, 3),
			new PrinterModelMeta(PrinterModel.EP3M, new int[] {6402}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK, LabelType.BLACK_MARK_GAP),
					1, 5, 3),
			new PrinterModelMeta(PrinterModel.ET10, new int[] {5376}, 203, PrintDirection.TOP, 1600,
					Arrays.asList(LabelType.CONTINUOUS), 3, 3, 3),
			new PrinterModelMeta(PrinterModel.FUST, new int[] {513}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.H1, new int[] {3840}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.H1S, new int[] {4352}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.HI_D110, new int[] {2305}, 203, PrintDirection.LEFT, 120,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 3),
			new PrinterModelMeta(PrinterModel.HI_NB_D11, new int[] {512}, 203, PrintDirection.LEFT, 120,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 3, 2),
			new PrinterModelMeta(PrinterModel.JC_M90, new int[] {51461}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.JCB3S, new int[] {256}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.TRANSPARENT), 1,
					5, 2),
			new PrinterModelMeta(PrinterModel.K2, new int[] {6144}, 203, PrintDirection.TOP, 448,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.K3, new int[] {4864}, 203, PrintDirection.TOP, 640,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.K3_ITD, new int[] {4868}, 203, PrintDirection.TOP, 640,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.K3_W, new int[] {4865}, 203, PrintDirection.TOP, 640,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.K4, new int[] {7168}, 203, PrintDirection.TOP, 832,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 15, 7),
			new PrinterModelMeta(PrinterModel.M2_H, new int[] {4608}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK, LabelType.BLACK_MARK_GAP),
					1, 5, 3),
			new PrinterModelMeta(PrinterModel.M3, new int[] {6400}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT, LabelType.BLACK, LabelType.BLACK_MARK_GAP),
					1, 5, 3),
			new PrinterModelMeta(PrinterModel.MP3K, new int[] {4866}, 203, PrintDirection.TOP, 640,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.MP3K_W, new int[] {4867}, 203, PrintDirection.TOP, 640,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.N1, new int[] {3586}, 203, PrintDirection.LEFT, 96,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.HEAT_SHRINK_TUBE, LabelType.TRANSPARENT,
							LabelType.BLACK_MARK_GAP, LabelType.CONTINUOUS),
					1, 3, 2),
			new PrinterModelMeta(PrinterModel.P1, new int[] {1024}, 300, PrintDirection.LEFT, 697,
					Arrays.asList(LabelType.PVC_TAG), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.P18, new int[] {1026}, 300, PrintDirection.LEFT, 662,
					Arrays.asList(LabelType.PVC_TAG), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.P1S, new int[] {1025}, 300, PrintDirection.LEFT, 662,
					Arrays.asList(LabelType.PVC_TAG), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.S1, new int[] {51458}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.S3, new int[] {51460}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.S6, new int[] {261, 259, 258, 257}, 203, PrintDirection.TOP, 576,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.S6_P, new int[] {274}, 203, PrintDirection.TOP, 600,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.T2S, new int[] {53250}, 203, PrintDirection.TOP, 832,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK), 1, 20, 15),
			new PrinterModelMeta(PrinterModel.T6, new int[] {51715}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.T7, new int[] {51717}, 203, PrintDirection.TOP, 384,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.T8, new int[] {51718}, 300, PrintDirection.TOP, 567,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.CONTINUOUS, LabelType.PERFORATED), 6,
					15, 10),
			new PrinterModelMeta(PrinterModel.T8S, new int[] {2053}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS), 1, 15, 10),
			new PrinterModelMeta(PrinterModel.TP2M_H, new int[] {4609}, 300, PrintDirection.TOP, 591,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.BLACK, LabelType.TRANSPARENT), 1, 5, 3),
			new PrinterModelMeta(PrinterModel.Z401, new int[] {2051}, 300, PrintDirection.TOP, 851,
					Arrays.asList(LabelType.WITH_GAPS, LabelType.TRANSPARENT), 1, 15, 10));

	private PrinterModels() {
	}

	public static List<PrinterModelMeta> all() {
		return TABLE;
	}

	public static Optional<PrinterModelMeta> findById(int modelId) {
		List<PrinterModelMeta> matches = new ArrayList<>();

		for (PrinterModelMeta meta : TABLE) {
			for (int id : meta.getIds()) {
				if (id == modelId) {
					matches.add(meta);
				}
			}
		}

		return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
	}
}
