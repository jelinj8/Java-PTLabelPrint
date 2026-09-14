package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Accumulated printer information, populated by {@link NiimbotDevice#connect()}/{@code fetchPrinterInfo()}. */
public class PrinterInfo {

	private ConnectResult connectResult;
	private int protocolVersion;
	private Integer modelId;
	private String serial;
	private String mac;
	private Integer batteryPercents;
	private AutoShutdownTime autoShutdownTime;
	private LabelType labelType;
	private Integer printheadWidth;
	private boolean supportColor;
	private String softwareVersion;
	private String hardwareVersion;
	private ResolutionClass resolutionClass;

	public ConnectResult getConnectResult() {
		return connectResult;
	}

	public void setConnectResult(ConnectResult connectResult) {
		this.connectResult = connectResult;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public Integer getModelId() {
		return modelId;
	}

	public void setModelId(Integer modelId) {
		this.modelId = modelId;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public Integer getBatteryPercents() {
		return batteryPercents;
	}

	public void setBatteryPercents(Integer batteryPercents) {
		this.batteryPercents = batteryPercents;
	}

	public AutoShutdownTime getAutoShutdownTime() {
		return autoShutdownTime;
	}

	public void setAutoShutdownTime(AutoShutdownTime autoShutdownTime) {
		this.autoShutdownTime = autoShutdownTime;
	}

	public LabelType getLabelType() {
		return labelType;
	}

	public void setLabelType(LabelType labelType) {
		this.labelType = labelType;
	}

	public Integer getPrintheadWidth() {
		return printheadWidth;
	}

	public void setPrintheadWidth(Integer printheadWidth) {
		this.printheadWidth = printheadWidth;
	}

	public boolean isSupportColor() {
		return supportColor;
	}

	public void setSupportColor(boolean supportColor) {
		this.supportColor = supportColor;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public void setSoftwareVersion(String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}

	public String getHardwareVersion() {
		return hardwareVersion;
	}

	public void setHardwareVersion(String hardwareVersion) {
		this.hardwareVersion = hardwareVersion;
	}

	public ResolutionClass getResolutionClass() {
		return resolutionClass;
	}

	public void setResolutionClass(ResolutionClass resolutionClass) {
		this.resolutionClass = resolutionClass;
	}

	@Override
	public String toString() {
		return "PrinterInfo{" + "connectResult=" + connectResult + ", protocolVersion=" + protocolVersion
				+ ", modelId=" + modelId + ", serial=" + serial + ", mac=" + mac + ", batteryPercents="
				+ batteryPercents + ", autoShutdownTime=" + autoShutdownTime + ", labelType=" + labelType
				+ ", printheadWidth=" + printheadWidth + ", supportColor=" + supportColor + ", softwareVersion="
				+ softwareVersion + ", hardwareVersion=" + hardwareVersion + ", resolutionClass=" + resolutionClass
				+ '}';
	}
}
