package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Which sound to query/toggle - sent with {@link RequestCommandId#SOUND_SETTINGS}. */
public enum SoundSettingsItemType {

	BLUETOOTH_CONNECTION_SOUND(1),
	POWER_SOUND(2);

	private final int code;

	SoundSettingsItemType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
