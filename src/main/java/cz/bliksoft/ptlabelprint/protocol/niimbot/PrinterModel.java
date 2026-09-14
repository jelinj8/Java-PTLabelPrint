package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Known Niimbot-protocol printer models, identified by the ID reported via
 * {@link PrinterInfoType#PRINTER_MODEL_ID}. Ported from a subset of niimbluelib's
 * {@code PrinterModel} (src/printer_models.ts, auto-generated there from the Niimbot app) -
 * covering only the D-series (closest reference for Phomemo's D/Q-series, per phomymo's own
 * grouping of the Q30 as "similar to D30") and M2_H (the closest published entry for the M2 in
 * hand). Extend {@link PrinterModels#TABLE} as more models are confirmed against real hardware,
 * rather than guessing IDs for models not yet tested - a Phomemo Q30's actual reported model ID is
 * not yet known and deliberately not guessed here (see {@link PrinterModels}).
 */
public enum PrinterModel {
	D101,
	D11,
	D11_H,
	D11_PRO,
	D110,
	D110_M,
	D11S,
	HI_D110,
	HI_NB_D11,
	M2_H
}
