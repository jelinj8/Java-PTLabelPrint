package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Response to {@code Heartbeat(PRINTER_INFO)}. */
public class HeartbeatPrinterInfoData {

	private final String softwareVersion;
	private final String hardwareVersion;
	private final int printheadWidth;
	private final ResolutionClass resolutionClass;
	private final int printheadAlignment;
	private final boolean supportsRfid;
	private final boolean supportsWriteRfid;

	public HeartbeatPrinterInfoData(String softwareVersion, String hardwareVersion, int printheadWidth,
			ResolutionClass resolutionClass, int printheadAlignment, boolean supportsRfid, boolean supportsWriteRfid) {
		this.softwareVersion = softwareVersion;
		this.hardwareVersion = hardwareVersion;
		this.printheadWidth = printheadWidth;
		this.resolutionClass = resolutionClass;
		this.printheadAlignment = printheadAlignment;
		this.supportsRfid = supportsRfid;
		this.supportsWriteRfid = supportsWriteRfid;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public String getHardwareVersion() {
		return hardwareVersion;
	}

	public int getPrintheadWidth() {
		return printheadWidth;
	}

	public ResolutionClass getResolutionClass() {
		return resolutionClass;
	}

	public int getPrintheadAlignment() {
		return printheadAlignment;
	}

	public boolean isSupportsRfid() {
		return supportsRfid;
	}

	public boolean isSupportsWriteRfid() {
		return supportsWriteRfid;
	}
}
