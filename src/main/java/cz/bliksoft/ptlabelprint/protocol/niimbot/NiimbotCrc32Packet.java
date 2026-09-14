package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * The firmware-upgrade wire frame: {@code [0x55, 0x55, CMD, CHUNK_HI, CHUNK_LO, DATA_LEN, DATA...,
 * CRC32_CHECKSUM (4 bytes), 0xAA, 0xAA]} - a genuinely different layout from {@link NiimbotPacket}'s
 * normal frame (a 2-byte chunk-number field, and a 4-byte CRC32 checksum instead of a 1-byte XOR
 * one). Ported from niimbluelib's {@code NiimbotCrc32Packet} (src/packets/packet.ts), used only by
 * {@link NiimbotDevice#firmwareUpgrade}.
 *
 * <p>
 * Deliberately <b>not</b> a subclass of {@link NiimbotPacket}: that class's fields/methods aren't
 * designed for subclassing (private fields, no protected hooks), and niimbluelib itself, despite
 * using {@code extends}, overrides every field-touching method anyway - the two frame layouts share
 * only the head/tail bytes, nothing else. A standalone class avoids putting any subclassing burden
 * on already hardware-confirmed, heavily-used code for the sake of this one feature.
 */
public class NiimbotCrc32Packet {

	private final int command;
	private final int chunkNumber;
	private final byte[] data;

	public NiimbotCrc32Packet(int command, int chunkNumber, byte[] data) {
		this.command = command & 0xff;
		this.chunkNumber = chunkNumber & 0xffff;
		this.data = data;
	}

	public int getCommand() {
		return command;
	}

	public int getChunkNumber() {
		return chunkNumber;
	}

	public byte[] getData() {
		return data;
	}

	public int getLength() {
		return NiimbotPacket.HEAD.length + 1 + 2 + 1 + data.length + 4 + NiimbotPacket.TAIL.length;
	}

	/** CRC32 over {@code [command, chunkNumberHi, chunkNumberLo, dataLength, ...data]} - matches niimbluelib's own {@code checksum} getter exactly. */
	public long checksum() {
		byte[] chunkB = u16(chunkNumber);
		byte[] buf = new byte[4 + data.length];
		buf[0] = (byte) command;
		buf[1] = chunkB[0];
		buf[2] = chunkB[1];
		buf[3] = (byte) data.length;
		System.arraycopy(data, 0, buf, 4, data.length);

		CRC32 crc = new CRC32();
		crc.update(buf);
		return crc.getValue();
	}

	/** {@code [0x55, 0x55, CMD, CHUNK_HI, CHUNK_LO, DATA_LEN, DATA..., CRC32(4 bytes), 0xAA, 0xAA]}. */
	public byte[] toBytes() {
		byte[] out = new byte[getLength()];
		int pos = 0;

		System.arraycopy(NiimbotPacket.HEAD, 0, out, pos, NiimbotPacket.HEAD.length);
		pos += NiimbotPacket.HEAD.length;

		out[pos++] = (byte) command;

		byte[] chunkB = u16(chunkNumber);
		out[pos++] = chunkB[0];
		out[pos++] = chunkB[1];

		out[pos++] = (byte) data.length;

		System.arraycopy(data, 0, out, pos, data.length);
		pos += data.length;

		long crc = checksum();
		out[pos++] = (byte) ((crc >> 24) & 0xff);
		out[pos++] = (byte) ((crc >> 16) & 0xff);
		out[pos++] = (byte) ((crc >> 8) & 0xff);
		out[pos++] = (byte) (crc & 0xff);

		System.arraycopy(NiimbotPacket.TAIL, 0, out, pos, NiimbotPacket.TAIL.length);

		return out;
	}

	public static NiimbotCrc32Packet fromBytes(byte[] buf) {
		int minPacketSize = NiimbotPacket.HEAD.length + 1 + 2 + 1 + 4 + NiimbotPacket.TAIL.length;

		if (buf.length < minPacketSize) {
			throw new NiimbotProtocolException("Packet is too small (" + buf.length + " < " + minPacketSize + ")");
		}
		if (!NiimbotPacket.hasSubarrayAtPos(buf, NiimbotPacket.HEAD, 0)) {
			throw new NiimbotProtocolException("Invalid packet head");
		}
		if (!NiimbotPacket.hasSubarrayAtPos(buf, NiimbotPacket.TAIL, buf.length - NiimbotPacket.TAIL.length)) {
			throw new NiimbotProtocolException("Invalid packet tail");
		}

		int cmd = buf[2] & 0xff;
		int chunkNumber = ((buf[3] & 0xff) << 8) | (buf[4] & 0xff);
		int dataLen = buf[5] & 0xff;

		if (buf.length != minPacketSize + dataLen) {
			throw new NiimbotProtocolException("Invalid packet size (" + buf.length + " != " + (minPacketSize + dataLen) + ")");
		}

		byte[] data = Arrays.copyOfRange(buf, 6, 6 + dataLen);
		long checksum = ((long) (buf[6 + dataLen] & 0xff) << 24) | ((buf[7 + dataLen] & 0xff) << 16)
				| ((buf[8 + dataLen] & 0xff) << 8) | (buf[9 + dataLen] & 0xff);

		NiimbotCrc32Packet packet = new NiimbotCrc32Packet(cmd, chunkNumber, data);

		if (packet.checksum() != checksum) {
			throw new NiimbotProtocolException("Invalid packet checksum (" + packet.checksum() + " != " + checksum + ")");
		}

		return packet;
	}

	private static byte[] u16(int n) {
		return new byte[] {(byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
	}
}
