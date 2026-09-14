package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PhomemoPrinterModelsTest {

	@Test
	void tableHasAllSeventeenCatalogedModels() {
		assertEquals(17, PhomemoPrinterModels.all().size());
	}

	@Test
	void findsP12ByModel() {
		Optional<PhomemoPrinterModelMeta> meta = PhomemoPrinterModels.findByModel(PhomemoPrinterModel.P12);
		assertTrue(meta.isPresent());
		assertEquals("p12", meta.get().getProtocolTag());
		assertEquals(12, meta.get().getWidthBytes());
		assertTrue(meta.get().isRotated());
		assertTrue(meta.get().isTape());
	}

	@Test
	void findsManualSelectOnlyModelsToo() {
		// M110S has no PrinterCatalog name-detection entry, but is still cataloged here.
		Optional<PhomemoPrinterModelMeta> meta = PhomemoPrinterModels.findByModel(PhomemoPrinterModel.M110S);
		assertTrue(meta.isPresent());
		assertEquals("m110", meta.get().getProtocolTag());
		assertEquals(PhomemoAlignment.RIGHT, meta.get().getAlignment());
	}
}
