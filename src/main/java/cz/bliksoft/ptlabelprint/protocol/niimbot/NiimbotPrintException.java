package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Thrown when the printer answers a request with {@link ResponseCommandId#IN_PRINT_ERROR} or
 * {@link ResponseCommandId#NOT_SUPPORTED}. Ported from niimbluelib's {@code PrintError}.
 */
public class NiimbotPrintException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int reasonId;

	public NiimbotPrintException(String message, int reasonId) {
		super(message);
		this.reasonId = reasonId;
	}

	/** Raw {@link PrinterErrorCode} value, or 0 for a plain "not supported" response. */
	public int getReasonId() {
		return reasonId;
	}
}
