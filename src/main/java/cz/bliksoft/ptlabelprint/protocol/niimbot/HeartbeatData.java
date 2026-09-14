package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Available fields depend on model - most are {@code null} when a given heartbeat variant doesn't report them. */
public class HeartbeatData {

	private Boolean paperInserted;
	private Boolean paperRfidSuccess;
	private Boolean lidClosed;
	private Integer batteryPercents;

	private Integer temp;
	private Boolean ribbonInserted;
	private Boolean ribbonRfidSuccess;

	private Integer wifiRssi;
	private Integer lightingErrorCode;
	private Integer voltageState;

	public Boolean getPaperInserted() {
		return paperInserted;
	}

	public void setPaperInserted(Boolean paperInserted) {
		this.paperInserted = paperInserted;
	}

	public Boolean getPaperRfidSuccess() {
		return paperRfidSuccess;
	}

	public void setPaperRfidSuccess(Boolean paperRfidSuccess) {
		this.paperRfidSuccess = paperRfidSuccess;
	}

	public Boolean getLidClosed() {
		return lidClosed;
	}

	public void setLidClosed(Boolean lidClosed) {
		this.lidClosed = lidClosed;
	}

	public Integer getBatteryPercents() {
		return batteryPercents;
	}

	public void setBatteryPercents(Integer batteryPercents) {
		this.batteryPercents = batteryPercents;
	}

	public Integer getTemp() {
		return temp;
	}

	public void setTemp(Integer temp) {
		this.temp = temp;
	}

	public Boolean getRibbonInserted() {
		return ribbonInserted;
	}

	public void setRibbonInserted(Boolean ribbonInserted) {
		this.ribbonInserted = ribbonInserted;
	}

	public Boolean getRibbonRfidSuccess() {
		return ribbonRfidSuccess;
	}

	public void setRibbonRfidSuccess(Boolean ribbonRfidSuccess) {
		this.ribbonRfidSuccess = ribbonRfidSuccess;
	}

	public Integer getWifiRssi() {
		return wifiRssi;
	}

	public void setWifiRssi(Integer wifiRssi) {
		this.wifiRssi = wifiRssi;
	}

	public Integer getLightingErrorCode() {
		return lightingErrorCode;
	}

	public void setLightingErrorCode(Integer lightingErrorCode) {
		this.lightingErrorCode = lightingErrorCode;
	}

	public Integer getVoltageState() {
		return voltageState;
	}

	public void setVoltageState(Integer voltageState) {
		this.voltageState = voltageState;
	}

	@Override
	public String toString() {
		return "HeartbeatData{" + "paperInserted=" + paperInserted + ", paperRfidSuccess=" + paperRfidSuccess
				+ ", lidClosed=" + lidClosed + ", batteryPercents=" + batteryPercents + ", temp=" + temp
				+ ", ribbonInserted=" + ribbonInserted + ", ribbonRfidSuccess=" + ribbonRfidSuccess + ", wifiRssi="
				+ wifiRssi + ", lightingErrorCode=" + lightingErrorCode + ", voltageState=" + voltageState + '}';
	}
}
