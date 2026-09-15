package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DSeriesLabelSizesTest {

	@Test
	void tableHasAllEightPresets() {
		assertEquals(8, DSeriesLabelSizes.all().size());
	}

	@Test
	void defaultPresetMatchesTheOriginalHardwareConfirmedTestPattern() {
		// 12x12mm = 96x96px pre-rotation - the exact dimensions confirmed printing correctly on a
		// real Q30 before this preset table existed (see CLAUDE.md "Status"). Must not change.
		Optional<DSeriesLabelSizes.Preset> preset = DSeriesLabelSizes.find("12x12");
		assertTrue(preset.isPresent());
		assertEquals(12, preset.get().getWidthBytes());
		assertEquals(96, preset.get().getHeightLines());
	}

	@Test
	void widerPresetConvertsMmToPixelsCorrectly() {
		Optional<DSeriesLabelSizes.Preset> preset = DSeriesLabelSizes.find("40x15");
		assertTrue(preset.isPresent());
		assertEquals(40, preset.get().getWidthBytes());
		assertEquals(120, preset.get().getHeightLines());
	}

	@Test
	void unknownKeyIsEmpty() {
		assertFalse(DSeriesLabelSizes.find("99x99").isPresent());
	}
}
