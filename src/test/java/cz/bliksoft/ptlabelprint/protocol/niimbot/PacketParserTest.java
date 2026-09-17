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
	void parsesPaperInfoResponse() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_CALIBRATE_HEIGHT.getCode(), new byte[] {
				0x00, 0x1e, // gapHeightPixel = 30
				0x02, 0x76, // totalHeightPixel = 630
				0x01, // paperType = WITH_GAPS
				0x01, 0x2c, // gapHeight = 300 -> 30.0mm
				0x02, 0x76, // totalHeight = 630 -> 63.0mm
				0x01, (byte) 0x80, // paperWidthPixel = 384
				0x01, 0x40, // paperWidth = 320 -> 32.0mm
				0x00, // direction = 0
				0x00, 0x14, // tailLengthPixel = 20
				0x00, 0x14, // tailLength = 20 -> 2.0mm
		});

		PaperInfo info = PacketParser.parsePaperInfoResponse(packet);

		assertEquals(true, info.isValid());
		assertEquals(30, info.getGapHeightPixel());
		assertEquals(630, info.getTotalHeightPixel());
		assertEquals(LabelType.WITH_GAPS, info.getPaperType());
		assertEquals(30.0, info.getGapHeight());
		assertEquals(63.0, info.getTotalHeight());
		assertEquals(384, info.getPaperWidthPixel());
		assertEquals(32.0, info.getPaperWidth());
		assertEquals(0, info.getDirection());
		assertEquals(20, info.getTailLengthPixel());
		assertEquals(2.0, info.getTailLength());
		// derived fields, not read directly off the wire
		assertEquals(33.0, info.getPaperHeight());
		assertEquals(600, info.getPaperHeightPixel());
	}

	@Test
	void paperInfoResponseWithUnrecognizedLengthIsInvalid() {
		NiimbotPacket packet = new NiimbotPacket(ResponseCommandId.IN_CALIBRATE_HEIGHT.getCode(), new byte[] {1, 2, 3, 4});
		assertEquals(false, PacketParser.parsePaperInfoResponse(packet).isValid());
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
