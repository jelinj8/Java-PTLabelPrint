package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NiimbotCrc32PacketTest {

	@Test
	void roundTripsThroughBytes() {
		NiimbotCrc32Packet packet = new NiimbotCrc32Packet(RequestCommandId.FIRMWARE_CHUNK.getCode(), 258,
				new byte[] {1, 2, 3, 4});

		byte[] bytes = packet.toBytes();
		assertEquals(NiimbotPacket.HEAD.length + 1 + 2 + 1 + 4 + 4 + NiimbotPacket.TAIL.length, bytes.length);
		assertEquals(0x55, bytes[0] & 0xff);
		assertEquals(0x55, bytes[1] & 0xff);
		assertEquals(RequestCommandId.FIRMWARE_CHUNK.getCode(), bytes[2] & 0xff);
		assertEquals(1, bytes[3] & 0xff); // chunk number hi byte (258 = 0x0102)
		assertEquals(2, bytes[4] & 0xff); // chunk number lo byte
		assertEquals(4, bytes[5] & 0xff); // data length

		NiimbotCrc32Packet parsed = NiimbotCrc32Packet.fromBytes(bytes);
		assertEquals(RequestCommandId.FIRMWARE_CHUNK.getCode(), parsed.getCommand());
		assertEquals(258, parsed.getChunkNumber());
		assertArrayEquals(new byte[] {1, 2, 3, 4}, parsed.getData());
	}

	@Test
	void chunkNumberZeroRoundTrips() {
		NiimbotCrc32Packet packet = new NiimbotCrc32Packet(RequestCommandId.FIRMWARE_CRC.getCode(), 0, new byte[] {9, 9, 9, 9});
		NiimbotCrc32Packet parsed = NiimbotCrc32Packet.fromBytes(packet.toBytes());
		assertEquals(0, parsed.getChunkNumber());
		assertArrayEquals(packet.getData(), parsed.getData());
	}

	@Test
	void rejectsBadChecksum() {
		byte[] bytes = new NiimbotCrc32Packet(RequestCommandId.FIRMWARE_CHUNK.getCode(), 1, new byte[] {1, 2}).toBytes();
		bytes[bytes.length - 3] ^= (byte) 0xff; // corrupt a CRC32 byte (last CRC byte is 3 before the tail)

		assertThrows(NiimbotProtocolException.class, () -> NiimbotCrc32Packet.fromBytes(bytes));
	}

	@Test
	void rejectsBadHead() {
		byte[] bytes = new NiimbotCrc32Packet(RequestCommandId.FIRMWARE_CHUNK.getCode(), 1, new byte[] {1, 2}).toBytes();
		bytes[0] = 0x00;

		assertThrows(NiimbotProtocolException.class, () -> NiimbotCrc32Packet.fromBytes(bytes));
	}

	@Test
	void rejectsTooSmallBuffer() {
		assertThrows(NiimbotProtocolException.class, () -> NiimbotCrc32Packet.fromBytes(new byte[] {0x55, 0x55}));
	}
}
