package cz.bliksoft.ptlabelprint.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class PrinterCatalogTest {

	@Test
	void detectsRealD11HAdvertisedName() {
		// Confirmed real device name from this project's own hardware testing.
		Optional<PrinterDefinition> match = PrinterCatalog.detectUnambiguous("D11_H-G412010570");
		assertTrue(match.isPresent());
		assertEquals(PrinterFamily.NIIMBOT, match.get().getFamily());
		assertEquals("D11_H", match.get().getId());
		assertTrue(match.get().isConfirmedOnHardware());
	}

	@Test
	void detectsRealQ30AdvertisedName() {
		// Confirmed real device name from this project's own hardware testing.
		Optional<PrinterDefinition> match = PrinterCatalog.detectUnambiguous("Q30");
		assertTrue(match.isPresent());
		assertEquals(PrinterFamily.PHOMEMO_D_SERIES, match.get().getFamily());
		assertEquals("phomemo-q30", match.get().getId());
		assertTrue(match.get().isConfirmedOnHardware());
	}

	@Test
	void longestPrefixWinsOverShorterOne() {
		// "Q30S..." must match the more specific Q30S entry, not the shorter Q30 entry it also
		// technically starts with.
		Optional<PrinterDefinition> match = PrinterCatalog.detectUnambiguous("Q30S-1234");
		assertTrue(match.isPresent());
		assertEquals("phomemo-q30s", match.get().getId());
	}

	@Test
	void longestPrefixWinsWithinNiimbotFamilyToo() {
		// "D11_H..." must match D11_H, not the shorter D11 entry it also technically starts with.
		Optional<PrinterDefinition> match = PrinterCatalog.detectUnambiguous("D11_H-anything");
		assertTrue(match.isPresent());
		assertEquals("D11_H", match.get().getId());
	}

	@Test
	void detectionIsCaseInsensitive() {
		Optional<PrinterDefinition> match = PrinterCatalog.detectUnambiguous("q30-lowercase");
		assertTrue(match.isPresent());
		assertEquals("phomemo-q30", match.get().getId());
	}

	@Test
	void unknownNameMatchesNothing() {
		assertTrue(PrinterCatalog.detect("TotallyUnknownDevice").isEmpty());
		assertFalse(PrinterCatalog.detectUnambiguous("TotallyUnknownDevice").isPresent());
	}

	@Test
	void nullOrEmptyNameMatchesNothing() {
		assertTrue(PrinterCatalog.detect(null).isEmpty());
		assertTrue(PrinterCatalog.detect("").isEmpty());
	}

	@Test
	void catalogHasNoAmbiguousEntriesAsShipped() {
		// Every confirmed-real advertised name pattern in the catalog must resolve unambiguously -
		// if this ever fails after adding an entry, that new entry collides with an existing one.
		for (PrinterDefinition def : PrinterCatalog.all()) {
			for (String prefix : def.getNamePrefixes()) {
				List<PrinterDefinition> matches = PrinterCatalog.detect(prefix);
				assertEquals(1, matches.size(), "Ambiguous catalog entry for prefix " + prefix + ": " + matches);
			}
		}
	}
}
