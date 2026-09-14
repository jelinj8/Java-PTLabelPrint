package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A single Niimbot wire frame: {@code [0x55, 0x55, CMD, DATA_LEN, DATA..., XOR_CHECKSUM, 0xAA, 0xAA]}.
 * Ported from niimbluelib's {@code NiimbotPacket} (src/packets/packet.ts). The CRC32-checksummed
 * firmware-upgrade variant ({@code NiimbotCrc32Packet}) is not ported - firmware upgrade is out of
 * scope for this project.
 */
public class NiimbotPacket {

	public static final byte[] HEAD = {0x55, 0x55};
	public static final byte[] TAIL = {(byte) 0xaa, (byte) 0xaa};

	/** Raw wire command byte (0-255) - either a {@link RequestCommandId#getCode()} or a {@link ResponseCommandId#getCode()}. */
	private final int command;
	private final byte[] data;

	/** Response command IDs this packet's send should wait for; empty for a one-way packet. */
	private List<ResponseCommandId> validResponseIds = Collections.emptyList();

	/** There can be no response after this request. */
	private boolean oneWay = false;

	public NiimbotPacket(int command, byte[] data) {
		this.command = command & 0xff;
		this.data = data;
	}

	public NiimbotPacket(RequestCommandId command, byte[] data) {
		this(command.getCode(), data);
	}

	public int getCommand() {
		return command;
	}

	public byte[] getData() {
		return data;
	}

	/** Data length (header, command, dataLen, checksum, tail are excluded). */
	public int getDataLength() {
		return data.length;
	}

	public int getLength() {
		return HEAD.length + 1 + 1 + data.length + 1 + TAIL.length;
	}

	public boolean isOneWay() {
		return oneWay;
	}

	public void setOneWay(boolean oneWay) {
		this.oneWay = oneWay;
	}

	public List<ResponseCommandId> getValidResponseIds() {
		return validResponseIds;
	}

	public void setValidResponseIds(List<ResponseCommandId> validResponseIds) {
		this.validResponseIds = validResponseIds;
	}

	public int checksum() {
		int c = command;
		c ^= (data.length & 0xff);
		for (byte b : data) {
			c ^= (b & 0xff);
		}
		return c & 0xff;
	}

	/** {@code [0x55, 0x55, CMD, DATA_LEN, DATA..., CHECKSUM, 0xAA, 0xAA]}. */
	public byte[] toBytes() {
		byte[] out = new byte[getLength()];
		int pos = 0;

		System.arraycopy(HEAD, 0, out, pos, HEAD.length);
		pos += HEAD.length;

		out[pos++] = (byte) command;
		out[pos++] = (byte) data.length;

		System.arraycopy(data, 0, out, pos, data.length);
		pos += data.length;

		out[pos++] = (byte) checksum();

		System.arraycopy(TAIL, 0, out, pos, TAIL.length);

		if (command == RequestCommandId.CONNECT.getCode()) {
			byte[] prefixed = new byte[out.length + 1];
			prefixed[0] = 0x03;
			System.arraycopy(out, 0, prefixed, 1, out.length);
			return prefixed;
		}

		return out;
	}

	public static NiimbotPacket fromBytes(byte[] buf) {
		int minPacketSize = HEAD.length + 1 + 1 + 1 + TAIL.length;

		if (buf.length < minPacketSize) {
			throw new NiimbotProtocolException("Packet is too small (" + buf.length + " < " + minPacketSize + ")");
		}

		if (!hasSubarrayAtPos(buf, HEAD, 0)) {
			throw new NiimbotProtocolException("Invalid packet head");
		}
		if (!hasSubarrayAtPos(buf, TAIL, buf.length - TAIL.length)) {
			throw new NiimbotProtocolException("Invalid packet tail");
		}

		int cmd = buf[2] & 0xff;
		int dataLen = buf[3] & 0xff;

		if (buf.length != minPacketSize + dataLen) {
			throw new NiimbotProtocolException("Invalid packet size (" + buf.length + " != " + (minPacketSize + dataLen) + ")");
		}

		byte[] data = Arrays.copyOfRange(buf, 4, 4 + dataLen);
		int checksum = buf[4 + dataLen] & 0xff;

		NiimbotPacket packet = new NiimbotPacket(cmd, data);

		if (packet.checksum() != checksum) {
			throw new NiimbotProtocolException("Invalid packet checksum (" + packet.checksum() + " != " + checksum + ")");
		}

		return packet;
	}

	static boolean hasSubarrayAtPos(byte[] arr, byte[] sub, int pos) {
		if (pos < 0 || pos > arr.length - sub.length) {
			return false;
		}
		for (int i = 0; i < sub.length; i++) {
			if (arr[pos + i] != sub[i]) {
				return false;
			}
		}
		return true;
	}
}
