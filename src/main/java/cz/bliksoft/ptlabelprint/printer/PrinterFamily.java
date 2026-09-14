package cz.bliksoft.ptlabelprint.printer;

/**
 * A wire-protocol family this library implements. One printer family can cover several brands
 * (see {@code cz.bliksoft.ptlabelprint.protocol.niimbot}'s own package-info for why
 * "Niimbot-protocol" isn't the same as "Niimbot-branded"). Only families with a real, tested
 * implementation are listed here - see the project CLAUDE.md's "Protocol families" section for
 * ones that are documented/planned but not implemented (zebra, brother, phomemo's other
 * sub-protocols).
 */
public enum PrinterFamily {
	/** {@code cz.bliksoft.ptlabelprint.protocol.niimbot} - Niimbot-branded printers (confirmed: D11_H). */
	NIIMBOT,
	/** {@code cz.bliksoft.ptlabelprint.protocol.phomemo}'s {@code d-series} sub-protocol (confirmed: Phomemo Q30). */
	PHOMEMO_D_SERIES
}
