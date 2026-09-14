package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Thrown for malformed packets (bad head/tail/checksum, wrong size) and for a buffered chunk that
 * doesn't yet form a complete frame. {@link NiimbotDevice} treats the latter as "wait for more
 * data" the same way niimbluelib's {@code processRawPacket} does - see its javadoc.
 */
public class NiimbotProtocolException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NiimbotProtocolException(String message) {
		super(message);
	}
}
