package cz.bliksoft.ptlabelprint.printer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PrinterFactoryTest {

	@ParameterizedTest
	@EnumSource(value = PrinterFamily.class,
			names = {"PHOMEMO_M02", "PHOMEMO_M04", "PHOMEMO_M110", "PHOMEMO_M_SERIES", "PHOMEMO_P12", "PHOMEMO_TSPL"})
	void unimplementedFamiliesThrowADistinctException(PrinterFamily family) {
		PrinterDefinition definition = new PrinterDefinition("test-id", family, Collections.emptyList(), false);

		UnimplementedPrinterFamilyException e = assertThrows(UnimplementedPrinterFamilyException.class,
				() -> PrinterFactory.create(definition, null));

		assertTrue(e instanceof IllegalArgumentException);
		assertTrue(e.getDefinition() == definition);
	}
}
