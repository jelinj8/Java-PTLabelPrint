package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DSeriesPrinterModelsTest {

	@Test
	void tableHasAllFiveModels() {
		assertEquals(5, DSeriesPrinterModels.all().size());
	}

	@Test
	void findsQ30ByCatalogId() {
		Optional<DSeriesPrinterModelMeta> meta = DSeriesPrinterModels.findById("phomemo-q30");
		assertTrue(meta.isPresent());
		assertEquals(DSeriesPrinterModel.Q30, meta.get().getModel());
		assertEquals(203, meta.get().getDpi());
		assertEquals(96, meta.get().getPrintheadPixels());
		assertEquals(1, meta.get().getDensityMin());
		assertEquals(8, meta.get().getDensityMax());
	}

	@Test
	void findsQ30sByCatalogIdDespiteTrailingLetter() {
		Optional<DSeriesPrinterModelMeta> meta = DSeriesPrinterModels.findById("phomemo-q30s");
		assertTrue(meta.isPresent());
		assertEquals(DSeriesPrinterModel.Q30S, meta.get().getModel());
	}

	@Test
	void unknownCatalogIdIsEmpty() {
		assertFalse(DSeriesPrinterModels.findById("phomemo-d999").isPresent());
		assertFalse(DSeriesPrinterModels.findById("niimbot-d11_h").isPresent());
		assertFalse(DSeriesPrinterModels.findById(null).isPresent());
	}
}
