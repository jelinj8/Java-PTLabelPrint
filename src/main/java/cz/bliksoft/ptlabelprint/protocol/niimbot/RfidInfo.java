package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * Consumable (paper/ribbon) NFC tag info, read via {@link RequestCommandId#RFID_INFO} /
 * {@link RequestCommandId#RFID_INFO_2}. This is where Niimbot's RFID-based consumable lock
 * surfaces in the protocol - a compatible clone printer (e.g. Phomemo's D/Q-series) that skips
 * that lock is expected to report {@link #isTagPresent()} false, or simply not enforce
 * {@link RequestCommandId#RFID_SUCCESS_TIMES}/paper-count checks the way a genuine Niimbot does;
 * confirm against real hardware rather than assuming.
 */
public class RfidInfo {

	private boolean tagPresent;
	private String uuid = "";
	private String uuid2;
	private String barCode = "";
	private String serialNumber = "";
	private int allPaper = -1;
	private int usedPaper = -1;
	private LabelType consumablesType = LabelType.INVALID;
	private Integer capacity;

	public boolean isTagPresent() {
		return tagPresent;
	}

	public void setTagPresent(boolean tagPresent) {
		this.tagPresent = tagPresent;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid2() {
		return uuid2;
	}

	public void setUuid2(String uuid2) {
		this.uuid2 = uuid2;
	}

	public String getBarCode() {
		return barCode;
	}

	public void setBarCode(String barCode) {
		this.barCode = barCode;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public int getAllPaper() {
		return allPaper;
	}

	public void setAllPaper(int allPaper) {
		this.allPaper = allPaper;
	}

	public int getUsedPaper() {
		return usedPaper;
	}

	public void setUsedPaper(int usedPaper) {
		this.usedPaper = usedPaper;
	}

	public LabelType getConsumablesType() {
		return consumablesType;
	}

	public void setConsumablesType(LabelType consumablesType) {
		this.consumablesType = consumablesType;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		return "RfidInfo{" + "tagPresent=" + tagPresent + ", uuid=" + uuid + ", uuid2=" + uuid2 + ", barCode="
				+ barCode + ", serialNumber=" + serialNumber + ", allPaper=" + allPaper + ", usedPaper=" + usedPaper
				+ ", consumablesType=" + consumablesType + ", capacity=" + capacity + '}';
	}
}
