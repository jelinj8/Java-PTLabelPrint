package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Result of the initial connect handshake, before any printer info is fetched. */
public class ConnectNegotiateResult {

	private final ConnectResult connectResult;
	private final int protocolVersion;
	private final boolean supportColor;

	public ConnectNegotiateResult(ConnectResult connectResult, int protocolVersion, boolean supportColor) {
		this.connectResult = connectResult;
		this.protocolVersion = protocolVersion;
		this.supportColor = supportColor;
	}

	public ConnectResult getConnectResult() {
		return connectResult;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public boolean isSupportColor() {
		return supportColor;
	}
}
