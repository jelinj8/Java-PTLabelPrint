package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Sequentially reads fields out of a packet's data payload, with EOF checks. Ported from
 * niimbluelib's {@code SequentialDataReader} (src/packets/data_reader.ts).
 */
public class SequentialDataReader {

	private final byte[] bytes;
	private int offset;

	public SequentialDataReader(byte[] bytes) {
		this.bytes = bytes;
		this.offset = 0;
	}

	/** Check available bytes. */
	public boolean canRead(int count) {
		return offset + count <= bytes.length;
	}

	private void willRead(int count) {
		if (!canRead(count)) {
			throw new NiimbotProtocolException("Tried to read too much data");
		}
	}

	public void skip(int len) {
		willRead(len);
		offset += len;
	}

	public byte[] readBytes(int len) {
		willRead(len);
		byte[] part = Arrays.copyOfRange(bytes, offset, offset + len);
		offset += len;
		return part;
	}

	public byte[] readVBytes() {
		int len = readI8();
		return readBytes(len);
	}

	public String readVString() {
		return new String(readVBytes(), StandardCharsets.UTF_8);
	}

	/** Read 8 bit unsigned int. */
	public int readI8() {
		willRead(1);
		int result = bytes[offset] & 0xff;
		offset += 1;
		return result;
	}

	public boolean readBool() {
		return readI8() > 0;
	}

	/** Read 16 bit signed int (big endian). */
	public int readI16() {
		willRead(2);
		int result = ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
		offset += 2;
		return (short) result;
	}

	/** Check EOF condition. */
	public void end() {
		if (offset != bytes.length) {
			throw new NiimbotProtocolException("Extra data left");
		}
	}
}
