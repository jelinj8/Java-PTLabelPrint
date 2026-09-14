package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Whether a {@link RequestCommandId#SOUND_SETTINGS} packet sets or reads a sound's on/off state. */
public enum SoundSettingsType {

	SET_SOUND(0x01),
	GET_SOUND_STATE(0x02);

	private final int code;

	SoundSettingsType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
