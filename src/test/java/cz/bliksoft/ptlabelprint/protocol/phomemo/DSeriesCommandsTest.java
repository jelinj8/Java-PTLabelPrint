package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DSeriesCommandsTest {

	@Test
	void densityToHeatTimeMapsFullRange() {
		assertEquals(40, DSeriesCommands.densityToHeatTime(1));
		assertEquals(140, DSeriesCommands.densityToHeatTime(6));
		assertEquals(200, DSeriesCommands.densityToHeatTime(8));
	}

	@Test
	void densityToHeatTimeClampsOutOfRange() {
		assertEquals(40, DSeriesCommands.densityToHeatTime(0));
		assertEquals(200, DSeriesCommands.densityToHeatTime(99));
	}

	@Test
	void heatSettingsBytes() {
		assertArrayEquals(new byte[] {0x1b, 0x37, 7, 100, 2}, DSeriesCommands.heatSettings(7, 100, 2));
	}

	@Test
	void mediaTypeBytes() {
		assertArrayEquals(new byte[] {0x1f, 0x11, 0x0a}, DSeriesCommands.mediaType(false));
		assertArrayEquals(new byte[] {0x1f, 0x11, 0x0b}, DSeriesCommands.mediaType(true));
	}

	@Test
	void headerBytesLittleEndianSplit() {
		// widthBytes=12, rows=300 (300 = 1*256 + 44)
		byte[] header = DSeriesCommands.header(12, 300);
		assertArrayEquals(new byte[] {
				0x1b, 0x40,
				0x1d, 0x76, 0x30, 0x00,
				12, 0,
				44, 1,
		}, header);
	}

	@Test
	void feedBytes() {
		assertArrayEquals(new byte[] {0x1b, 0x4a, 32}, DSeriesCommands.feed(32));
	}
}
