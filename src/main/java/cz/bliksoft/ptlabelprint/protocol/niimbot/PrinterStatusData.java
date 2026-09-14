package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Response to {@link RequestCommandId#PRINTER_STATUS_DATA}, used during connect negotiation on protocol v3+. */
public class PrinterStatusData {

	private int protocolVersion;
	private boolean supportColor;

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public boolean isSupportColor() {
		return supportColor;
	}

	public void setSupportColor(boolean supportColor) {
		this.supportColor = supportColor;
	}
}
