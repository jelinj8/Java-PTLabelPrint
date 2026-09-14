package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class PacketParserTest {

	/** Same example niimbluelib's own {@code parsePacketBundle} javadoc uses. */
	@Test
	void parsesTwoConcatenatedPackets() {
		byte[] buf = hexToBytes("55554a01044faaaa5555f60101f6aaaa");

		List<NiimbotPacket> packets = PacketParser.parsePacketBundle(buf);

		assertEquals(2, packets.size());
		assertEquals(0x4a, packets.get(0).getCommand());
		assertEquals(1, packets.get(0).getDataLength());
		assertEquals(0x04, packets.get(0).getData()[0] & 0xff);
		assertEquals(0xf6, packets.get(1).getCommand());
		assertEquals(0x01, packets.get(1).getData()[0] & 0xff);
	}

	@Test
	void throwsOnIncompleteTrailingFrame() {
		byte[] complete = new NiimbotPacket(RequestCommandId.PRINTER_INFO, new byte[] {8}).toBytes();
		// drop the last two (tail) bytes to simulate a frame still arriving
		byte[] incomplete = java.util.Arrays.copyOfRange(complete, 0, complete.length - 2);

		assertThrows(NiimbotProtocolException.class, () -> PacketParser.parsePacketBundle(incomplete));
	}

	@Test
	void parsesConnectResponse() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_CONNECT.getCode(), new byte[] {3});
		assertEquals(ConnectResult.CONNECTED_V3, PacketParser.parseConnectResponse(packet));
	}

	@Test
	void batteryLevelBelowFiveIsScaledToPercent() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_PRINTER_INFO_CHARGE_LEVEL.getCode(), new byte[] {3});
		assertEquals(75, PacketParser.parseBatteryChargeLevelResponse(packet));
	}

	@Test
	void batteryLevelAboveFourIsReturnedAsIs() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_PRINTER_INFO_CHARGE_LEVEL.getCode(), new byte[] {60});
		assertEquals(60, PacketParser.parseBatteryChargeLevelResponse(packet));
	}

	@Test
	void serialNumberShorterThanFourBytesIsUnknown() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_PRINTER_INFO_SERIAL_NUMBER.getCode(), new byte[] {1, 2});
		assertEquals("-1", PacketParser.parsePrinterSerialNumberResponse(packet));
	}

	@Test
	void serialNumberAtLeastEightBytesIsAscii() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_PRINTER_INFO_SERIAL_NUMBER.getCode(), "ABCDEFGH".getBytes());
		assertEquals("ABCDEFGH", PacketParser.parsePrinterSerialNumberResponse(packet));
	}

	@Test
	void serialNumberBetweenFourAndSevenBytesIsHex() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_PRINTER_INFO_SERIAL_NUMBER.getCode(),
				new byte[] {0x01, 0x02, 0x03, 0x04, 0x05});
		assertEquals("01020304", PacketParser.parsePrinterSerialNumberResponse(packet));
	}

	private static byte[] hexToBytes(String hex) {
		byte[] out = new byte[hex.length() / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return out;
	}
}
