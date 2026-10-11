package cz.bliksoft.ptlabelprint.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import cz.bliksoft.javautils.ble.BleCharacteristic;
import cz.bliksoft.javautils.ble.BleService;

class BleTransportTest {

	private static BleService service(String uuid) {
		return new BleService(uuid,
				Arrays.asList(new BleCharacteristic("c-" + uuid, Arrays.asList("NOTIFY", "WRITE_WITHOUT_RESPONSE"))));
	}

	/**
	 * Niimbot M2's GATT layout as BlueZ reports it (by handle) - the Niimbot
	 * service last.
	 */
	@Test
	void knownServicesComeFirst() {
		List<BleService> bluezOrder = Arrays.asList(service(BleTransport.PHOMEMO_SERVICE_UUID),
				service("0000ff10-0000-1000-8000-00805f9b34fb"), service("49535343-fe7d-4ae5-8fa9-9fafd205e455"),
				service(BleTransport.SERVICE_UUID.toUpperCase()));

		List<String> ordered = BleTransport.preferKnownChannelServices(bluezOrder).stream().map(BleService::getUuid)
				.collect(Collectors.toList());

		assertEquals(Arrays.asList(BleTransport.SERVICE_UUID.toUpperCase(), BleTransport.PHOMEMO_SERVICE_UUID,
				"0000ff10-0000-1000-8000-00805f9b34fb", "49535343-fe7d-4ae5-8fa9-9fafd205e455"), ordered);
	}
}
