package cz.bliksoft.ptlabelprint.printer;

/**
 * A wire-protocol family this library either implements or has cataloged. One printer family can
 * cover several brands (see {@code cz.bliksoft.ptlabelprint.protocol.niimbot}'s own package-info
 * for why "Niimbot-protocol" isn't the same as "Niimbot-branded"). Most families here have a real,
 * tested implementation (a {@link LabelPrinter} subclass exists) - see the project CLAUDE.md's
 * "Protocol families" section for what's documented/planned but has no enum constant at all yet
 * (zebra, brother).
 *
 * <p>
 * The 6 {@code PHOMEMO_*} constants below {@link #PHOMEMO_D_SERIES} are the deliberate exception:
 * cataloged (real specs in {@code protocol.phomemo.PhomemoPrinterModels}) but <b>not</b>
 * implemented (no command-builder/print-flow code, no {@link LabelPrinter} subclass) - see
 * {@link PrinterFactory} for the distinct {@link UnimplementedPrinterFamilyException} this throws
 * for them. This is narrower than it looks: no new {@code protocol.phomemo.m02}-style *package* is
 * created for any of them (that empty-scaffolding avoidance still holds, same as it does for
 * zebra/brother). What's added is only the enum constant + catalog data needed for
 * {@link PrinterCatalog}/{@link PrinterFactory} to correctly *identify* one of these devices
 * instead of misdispatching it into {@link #PHOMEMO_D_SERIES} - exactly the class of bug this
 * project's CLAUDE.md "Corrected finding" postmortem is about (a real Phomemo Q30 once got mistaken
 * for a Niimbot-protocol device from name/README wording alone).
 */
public enum PrinterFamily {
	/** {@code cz.bliksoft.ptlabelprint.protocol.niimbot} - Niimbot-branded printers (confirmed: D11_H, M2_H). */
	NIIMBOT,
	/** {@code cz.bliksoft.ptlabelprint.protocol.phomemo}'s {@code d-series} sub-protocol (confirmed: Phomemo Q30). */
	PHOMEMO_D_SERIES,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_M02,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_M04,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_M110,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_M_SERIES,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_P12,
	/** Cataloged, NOT implemented - see {@link PrinterFactory}/{@link UnimplementedPrinterFamilyException}. */
	PHOMEMO_TSPL
}
