package cz.bliksoft.ptlabelprint.protocol.phomemo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class M110CommandsTest {

	@Test
	void densityMapsToProtocolScale() {
		assertEquals(6, M110Commands.densityToM110(1));
		assertEquals(10, M110Commands.densityToM110(4));
		assertEquals(13, M110Commands.densityToM110(6));
		assertEquals(15, M110Commands.densityToM110(8));
	}

	@Test
	void densityClampsOutOfRange() {
		assertEquals(6, M110Commands.densityToM110(0));
		assertEquals(15, M110Commands.densityToM110(99));
	}

	@Test
	void speedAndDensityBytes() {
		assertArrayEquals(new byte[] {0x1b, 0x4e, 0x0d, 5}, M110Commands.speed(5));
		assertArrayEquals(new byte[] {0x1b, 0x4e, 0x04, 10}, M110Commands.density(10));
	}

	@Test
	void mediaTypeBytes() {
		assertArrayEquals(new byte[] {0x1f, 0x11, 0x0a}, M110Commands.mediaType(false));
		assertArrayEquals(new byte[] {0x1f, 0x11, 0x0b}, M110Commands.mediaType(true));
	}

	@Test
	void rasterHeaderLittleEndianSplit() {
		// widthBytes=40, heightLines=300 (300 = 1*256 + 44)
		assertArrayEquals(new byte[] {0x1d, 0x76, 0x30, 0x00, 40, 0, 44, 1}, M110Commands.rasterHeader(40, 300));
	}

	@Test
	void footerBytes() {
		assertArrayEquals(new byte[] {0x1f, (byte) 0xf0, 0x05, 0x00, 0x1f, (byte) 0xf0, 0x03, 0x00}, M110Commands.FOOTER);
	}
}
