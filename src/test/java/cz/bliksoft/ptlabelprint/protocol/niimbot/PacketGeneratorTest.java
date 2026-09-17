package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PacketGeneratorTest {

	@Test
	void connectBuildsExpectedPacket() {
		NiimbotPacket p = PacketGenerator.connect();
		assertEquals(RequestCommandId.CONNECT.getCode(), p.getCommand());
		assertArrayEquals(new byte[] {1}, p.getData());
		assertFalse(p.isOneWay());
		assertTrue(p.getValidResponseIds().contains(ResponseCommandId.IN_CONNECT));
	}

	@Test
	void getPrinterInfoEncodesRequestedType() {
		NiimbotPacket p = PacketGenerator.getPrinterInfo(PrinterInfoType.SERIAL_NUMBER);
		assertEquals(RequestCommandId.PRINTER_INFO.getCode(), p.getCommand());
		assertArrayEquals(new byte[] {(byte) PrinterInfoType.SERIAL_NUMBER.getCode()}, p.getData());
	}

	@Test
	void heartbeatEncodesRequestedType() {
		NiimbotPacket p = PacketGenerator.heartbeat(HeartbeatType.ADVANCED_2);
		assertArrayEquals(new byte[] {(byte) HeartbeatType.ADVANCED_2.getCode()}, p.getData());
	}

	@Test
	void unmappedCommandIsRejected() {
		// ANTI_FAKE isn't wired up yet - mapped() must not silently misclassify it as one-way.
		assertThrows(IllegalArgumentException.class, () -> PacketGenerator.mapped(RequestCommandId.ANTI_FAKE, new byte[] {1}));
	}

	@Test
	void printStart9bBytes() {
		NiimbotPacket p = PacketGenerator.printStart9b(1, PageColorType.SINGLE_COLOR, 1, false);
		assertEquals(RequestCommandId.PRINT_START.getCode(), p.getCommand());
		assertArrayEquals(new byte[] {0, 1, 0, 0, 0, 0, 0, 1, 0}, p.getData());
	}

	@Test
	void setPageSize13bWithoutSerialIs13Bytes() {
		NiimbotPacket p = PacketGenerator.setPageSize13b(300, 144, 1, 0, 0, 0, 0, null);
		assertEquals(RequestCommandId.SET_PAGE_SIZE.getCode(), p.getCommand());
		assertEquals(13, p.getDataLength());
		// rows=300 (0x01,0x2c), cols=144 (0x00,0x90), copies=1, cutHeight=0, cutType=0, 0x00, sendAll=0, partHeight=0
		assertArrayEquals(new byte[] {0x01, 0x2c, 0x00, (byte) 0x90, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
				p.getData());
	}

	@Test
	void setPageSize13bRejectsWrongSerialLength() {
		assertThrows(IllegalArgumentException.class,
				() -> PacketGenerator.setPageSize13b(1, 1, 1, 0, 0, 0, 0, new byte[] {1, 2, 3}));
	}

	@Test
	void printBitmapRowIsOneWay() {
		NiimbotPacket p = PacketGenerator.printBitmapRow(0, 1, new byte[] {(byte) 0xff}, 144, "auto");
		assertEquals(RequestCommandId.PRINT_BITMAP_ROW.getCode(), p.getCommand());
		assertTrue(p.isOneWay());
	}

	@Test
	void printBitmapRowIndexedRejectsMoreThanSixBlackPixels() {
		assertThrows(IllegalArgumentException.class,
				() -> PacketGenerator.printBitmapRowIndexed(0, 1, new byte[] {(byte) 0xff}, 144, "auto"));
	}

	@Test
	void printBitmapRowIndexedIsOneWayForFewBlackPixels() {
		// 0x01 = a single black pixel (bit 0 in the JS-style population count, position irrelevant here)
		NiimbotPacket p = PacketGenerator.printBitmapRowIndexed(0, 1, new byte[] {0x01}, 144, "auto");
		assertEquals(RequestCommandId.PRINT_BITMAP_ROW_INDEXED.getCode(), p.getCommand());
		assertTrue(p.isOneWay());
	}

	@Test
	void printEmptySpaceIsOneWay() {
		NiimbotPacket p = PacketGenerator.printEmptySpace(5, 2);
		assertEquals(RequestCommandId.PRINT_EMPTY_ROW.getCode(), p.getCommand());
		assertArrayEquals(new byte[] {0, 5, 2}, p.getData());
		assertTrue(p.isOneWay());
	}
}
