package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NiimbotPacketTest {

	@Test
	void roundTripsThroughBytes() {
		NiimbotPacket packet = new NiimbotPacket(RequestCommandId.PRINTER_INFO, new byte[] {8});

		byte[] bytes = packet.toBytes();
		// 55 55 40 01 08 checksum aa aa
		assertEquals(0x40 ^ 0x01 ^ 0x08, packet.checksum());
		assertArrayEquals(new byte[] {0x55, 0x55, 0x40, 0x01, 0x08, (byte) packet.checksum(), (byte) 0xaa, (byte) 0xaa}, bytes);

		NiimbotPacket parsed = NiimbotPacket.fromBytes(bytes);
		assertEquals(RequestCommandId.PRINTER_INFO.getCode(), parsed.getCommand());
		assertArrayEquals(new byte[] {8}, parsed.getData());
	}

	@Test
	void connectRequestIsPrefixedWithA0x03Byte() {
		NiimbotPacket connect = new NiimbotPacket(RequestCommandId.CONNECT, new byte[] {1});
		byte[] bytes = connect.toBytes();

		assertEquals(0x03, bytes[0] & 0xff);
		// the rest of the frame (from index 1) is a normal packet
		NiimbotPacket reparsed = NiimbotPacket.fromBytes(java.util.Arrays.copyOfRange(bytes, 1, bytes.length));
		assertEquals(RequestCommandId.CONNECT.getCode(), reparsed.getCommand());
	}

	@Test
	void nonConnectRequestHasNoPrefix() {
		NiimbotPacket p = new NiimbotPacket(RequestCommandId.HEARTBEAT, new byte[] {1});
		byte[] bytes = p.toBytes();
		assertEquals(0x55, bytes[0] & 0xff);
	}

	@Test
	void rejectsBadChecksum() {
		byte[] bytes = new NiimbotPacket(RequestCommandId.PRINTER_INFO, new byte[] {8}).toBytes();
		bytes[5] = (byte) (bytes[5] ^ 0xff); // corrupt checksum byte

		assertThrows(NiimbotProtocolException.class, () -> NiimbotPacket.fromBytes(bytes));
	}

	@Test
	void rejectsBadHead() {
		byte[] bytes = new NiimbotPacket(RequestCommandId.PRINTER_INFO, new byte[] {8}).toBytes();
		bytes[0] = 0x00;

		assertThrows(NiimbotProtocolException.class, () -> NiimbotPacket.fromBytes(bytes));
	}

	@Test
	void rejectsTooSmallBuffer() {
		assertThrows(NiimbotProtocolException.class, () -> NiimbotPacket.fromBytes(new byte[] {0x55, 0x55}));
	}
}
