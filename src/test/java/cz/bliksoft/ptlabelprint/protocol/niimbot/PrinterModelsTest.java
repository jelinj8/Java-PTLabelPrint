package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PrinterModelsTest {

	@Test
	void findsD11HByReportedId() {
		Optional<PrinterModelMeta> meta = PrinterModels.findById(528);
		assertTrue(meta.isPresent());
		assertEquals(PrinterModel.D11_H, meta.get().getModel());
		assertEquals(300, meta.get().getDpi());
	}

	@Test
	void findsM2HByReportedId() {
		Optional<PrinterModelMeta> meta = PrinterModels.findById(4608);
		assertTrue(meta.isPresent());
		assertEquals(PrinterModel.M2_H, meta.get().getModel());
		assertEquals(PrintDirection.TOP, meta.get().getPrintDirection());
	}

	@Test
	void unknownIdIsEmptyRatherThanGuessed() {
		// Any ID not already in the table (this one is arbitrary) must resolve to empty, not a
		// nearest-guess entry - see PrinterModels' javadoc (a Phomemo Q30 will never be in this
		// table at all: it speaks a different protocol entirely, not niimbot).
		Optional<PrinterModelMeta> meta = PrinterModels.findById(999999);
		assertFalse(meta.isPresent());
	}
}
