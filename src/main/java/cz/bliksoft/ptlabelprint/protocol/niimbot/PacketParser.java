package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses response packets. Ported from niimbluelib's {@code PacketParser}
 * (src/packets/packet_parser.ts) - only the subset {@link NiimbotDevice} actually uses is
 * implemented; RFID/firmware/print-status-adjacent parsing beyond that is not ported yet.
 */
public class PacketParser {

	private PacketParser() {
	}

	/**
	 * Parse raw bytes containing one or more complete packets. Ported from niimbluelib's
	 * {@code parsePacketBundle} - the firmware CRC32-packet branch is not ported (out of scope), so
	 * this only recognizes plain {@link NiimbotPacket} frames.
	 *
	 * @throws NiimbotProtocolException if the buffer doesn't end exactly on a frame boundary - the
	 *         caller ({@link NiimbotDevice}) treats this as "incomplete, wait for more data", the
	 *         same way niimbluelib's {@code processRawPacket} does.
	 */
	public static List<NiimbotPacket> parsePacketBundle(byte[] buf) {
		List<byte[]> chunks = new ArrayList<>();
		int bufLength = buf.length;
		int pos = 0;

		while (buf.length - pos > 0) {
			if (!NiimbotPacket.hasSubarrayAtPos(buf, NiimbotPacket.HEAD, pos)) {
				break;
			}
			if (buf.length - pos < 3) {
				break;
			}

			int sizePos = pos + 3;

			if (buf.length <= sizePos) {
				break;
			}

			int size = buf[sizePos] & 0xff;
			int crcSize = 1;

			if (buf.length <= sizePos + size + crcSize + NiimbotPacket.TAIL.length) {
				break;
			}

			int tailPos = sizePos + size + crcSize + 1;

			if (!NiimbotPacket.hasSubarrayAtPos(buf, NiimbotPacket.TAIL, tailPos)) {
				break;
			}

			int tailEnd = tailPos + NiimbotPacket.TAIL.length;

			chunks.add(Arrays.copyOfRange(buf, pos, tailEnd));
			pos = tailEnd;
		}

		int chunksDataLen = 0;
		for (byte[] c : chunks) {
			chunksDataLen += c.length;
		}

		if (bufLength != chunksDataLen) {
			throw new NiimbotProtocolException("Split chunks data length not equal to buffer length (" + bufLength
					+ " != " + chunksDataLen + ")");
		}

		List<NiimbotPacket> packets = new ArrayList<>(chunks.size());
		for (byte[] c : chunks) {
			packets.add(NiimbotPacket.fromBytes(c));
		}
		return packets;
	}

	public static PrintStatus parsePrintStatusResponse(NiimbotPacket packet) {
		requireAtLeast(packet, 4);

		SequentialDataReader r = new SequentialDataReader(packet.getData());
		int page = r.readI16();
		int pagePrintProgress = r.readI8();
		int pageFeedProgress = r.readI8();
		int error = 0;

		if (packet.getDataLength() == 10) {
			r.skip(2);
			error = r.readI8();
		}

		return new PrintStatus(page, pagePrintProgress, pageFeedProgress, error);
	}

	public static ConnectResult parseConnectResponse(NiimbotPacket packet) {
		requireAtLeast(packet, 1);
		return ConnectResult.fromCode(packet.getData()[0] & 0xff)
				.orElseThrow(() -> new NiimbotProtocolException("Unknown connect result " + (packet.getData()[0] & 0xff)));
	}

	public static PrinterStatusData parsePrinterStatusDataResponse(NiimbotPacket packet) {
		PrinterStatusData result = new PrinterStatusData();

		if (packet.getDataLength() >= 13) {
			byte[] d = packet.getData();
			result.setSupportColor((d[10] & 0xff) > 0);

			int n = (d[11] & 0xff) * 100 + (d[12] & 0xff);

			if (n >= 204 && n < 300) {
				result.setProtocolVersion(3);
			} else if (n >= 300 && n < 302) {
				result.setProtocolVersion(4);
			} else if (n >= 302) {
				result.setProtocolVersion(5);
			}
		}

		return result;
	}

	public static int parsePrinterInfoModelIdResponse(NiimbotPacket packet) {
		requireAtLeast(packet, 1);

		if (packet.getDataLength() == 1) {
			return (packet.getData()[0] & 0xff) << 8;
		}

		requireExactly(packet, 2);
		byte[] d = packet.getData();
		return ((d[0] & 0xff) << 8) | (d[1] & 0xff);
	}

	public static RfidInfo parseRfidInfoResponse(NiimbotPacket packet) {
		RfidInfo info = new RfidInfo();

		if (packet.getDataLength() == 1) {
			return info;
		}

		SequentialDataReader r = new SequentialDataReader(packet.getData());
		info.setTagPresent(true);
		info.setUuid(bufToHex(r.readBytes(8), ""));
		info.setBarCode(r.readVString());
		info.setSerialNumber(r.readVString());
		info.setAllPaper(r.readI16());
		info.setUsedPaper(r.readI16());
		info.setConsumablesType(LabelType.fromCode(r.readI8()).orElse(LabelType.INVALID));

		if (r.canRead(2)) {
			info.setCapacity(r.readI16());
		}

		if (r.canRead(8)) {
			info.setUuid2(bufToHex(r.readBytes(8), ""));
		}

		r.end();

		return info;
	}

	public static HeartbeatPrinterInfoData parseHeartbeatPrinterInfoResponse(NiimbotPacket packet) {
		requireExactly(packet, 10);

		SequentialDataReader r = new SequentialDataReader(packet.getData());

		byte[] hw = r.readBytes(2);
		byte[] fw = r.readBytes(2);
		int hwH = hw[0] & 0xff;
		int hwL = hw[1] & 0xff;
		int fwH = fw[0] & 0xff;
		int fwL = fw[1] & 0xff;

		String softwareVersion = String.format("%.2f", fwL / 100.0 + fwH);
		String hardwareVersion = String.format("%.2f", hwL / 100.0 + hwH);
		int printheadWidth = r.readI16();
		ResolutionClass resolutionClass = ResolutionClass.fromCode(r.readI8());
		int printheadAlignment = r.readI8();
		boolean supportsRfid = r.readBool();
		boolean supportsWriteRfid = r.readBool();

		r.end();

		return new HeartbeatPrinterInfoData(softwareVersion, hardwareVersion, printheadWidth, resolutionClass,
				printheadAlignment, supportsRfid, supportsWriteRfid);
	}

	public static HeartbeatData parseHeartbeatAdvanced1Response(NiimbotPacket packet, Integer modelId) {
		int len = packet.getDataLength();
		SequentialDataReader r = new SequentialDataReader(packet.getData());
		HeartbeatData info = new HeartbeatData();

		if (len == 10) {
			// d110
			r.skip(8);
			info.setLidClosed(r.readI8() == 0);
			info.setBatteryPercents(r.readI8());
		} else if (len == 13) {
			// b1
			r.skip(9);
			info.setLidClosed(r.readI8() == 0);
			info.setBatteryPercents(r.readI8());
			info.setPaperInserted(r.readI8() == 0);
			info.setPaperRfidSuccess(r.readI8() != 0);
		} else if (len == 19) {
			r.skip(15);
			info.setLidClosed(r.readI8() == 0);
			info.setBatteryPercents(r.readI8());
			info.setPaperInserted(r.readI8() == 0);
			info.setPaperRfidSuccess(r.readI8() != 0);
		} else if (len == 20) {
			r.skip(18);
			info.setPaperInserted(r.readI8() == 0);
			info.setPaperRfidSuccess(r.readI8() != 0);
		} else {
			throw new NiimbotProtocolException("Invalid heartbeat length " + len);
		}
		r.end();

		int[] invertedLidModels = {512, 514, 513, 2304, 1792, 3584, 5120, 2560, 3840, 4352, 272, 273, 274};

		if (modelId != null && info.getLidClosed() != null) {
			for (int m : invertedLidModels) {
				if (m == modelId) {
					info.setLidClosed(!info.getLidClosed());
					break;
				}
			}
		}

		if (info.getBatteryPercents() != null && info.getBatteryPercents() <= 4) {
			info.setBatteryPercents(info.getBatteryPercents() * 25);
		}

		return info;
	}

	public static HeartbeatData parseHeartbeatAdvanced2Response(NiimbotPacket packet) {
		requireAtLeast(packet, 9);

		SequentialDataReader r = new SequentialDataReader(packet.getData());
		HeartbeatData info = new HeartbeatData();

		r.skip(2);
		info.setBatteryPercents(r.readI8());
		info.setTemp(r.readI8());
		info.setLidClosed(r.readI8() == 0);
		info.setPaperInserted(r.readI8() == 0);
		info.setPaperRfidSuccess(r.readI8() != 0);
		info.setRibbonRfidSuccess(r.readI8() != 0);
		info.setRibbonInserted(r.readI8() == 0);

		if (r.canRead(2)) {
			info.setWifiRssi(r.readI16());
		}

		if (r.canRead(2)) {
			r.skip(1);
			info.setLightingErrorCode(r.readI8());
		}

		if (r.canRead(1)) {
			info.setVoltageState(r.readI8());
		}

		r.end();

		if (info.getBatteryPercents() <= 4) {
			info.setBatteryPercents(info.getBatteryPercents() * 25);
		}

		return info;
	}

	public static String parsePrinterVersionResponse(NiimbotPacket packet) {
		requireExactly(packet, 2);
		byte[] d = packet.getData();
		double v1 = (d[1] & 0xff) / 100.0 + (d[0] & 0xff);
		double v2 = ((d[0] & 0xff) * 256 + (d[1] & 0xff)) / 100.0;
		return String.format("0x%s (%.2f or %.2f)", bufToHex(d, ""), v1, v2);
	}

	public static String parsePrinterSerialNumberResponse(NiimbotPacket packet) {
		requireAtLeast(packet, 1);
		byte[] d = packet.getData();

		if (d.length < 4) {
			return "-1";
		}

		if (d.length >= 8) {
			return new String(d, java.nio.charset.StandardCharsets.UTF_8);
		}

		return bufToHex(Arrays.copyOfRange(d, 0, 4), "").toUpperCase(java.util.Locale.ROOT);
	}

	public static String parsePrinterBluetoothMacAddressResponse(NiimbotPacket packet) {
		requireAtLeast(packet, 1);
		byte[] d = packet.getData();
		byte[] reversed = new byte[d.length];
		for (int i = 0; i < d.length; i++) {
			reversed[i] = d[d.length - 1 - i];
		}
		return bufToHex(reversed, ":");
	}

	public static boolean parseIsSoundEnabledResponse(NiimbotPacket packet) {
		requireExactly(packet, 3);
		return packet.getData()[2] != 0;
	}

	public static int parseBatteryChargeLevelResponse(NiimbotPacket packet) {
		requireExactly(packet, 1);
		int value = packet.getData()[0] & 0xff;
		return value <= 4 ? value * 25 : value;
	}

	public static int parseAutoShutdownTimeResponse(NiimbotPacket packet) {
		requireExactly(packet, 1);
		return packet.getData()[0] & 0xff;
	}

	public static LabelType parseLabelTypeResponse(NiimbotPacket packet) {
		requireExactly(packet, 1);
		return LabelType.fromCode(packet.getData()[0] & 0xff).orElse(LabelType.INVALID);
	}

	public static boolean parseBooleanResponse(NiimbotPacket packet) {
		requireExactly(packet, 1);
		return packet.getData()[0] == 1;
	}

	private static void requireExactly(NiimbotPacket packet, int len) {
		if (packet.getDataLength() != len) {
			throw new NiimbotProtocolException("Array length must be " + len);
		}
	}

	private static void requireAtLeast(NiimbotPacket packet, int len) {
		if (packet.getDataLength() < len) {
			throw new NiimbotProtocolException("Array length must be at least " + len);
		}
	}

	private static String bufToHex(byte[] buf, String separator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < buf.length; i++) {
			if (i > 0 && !separator.isEmpty()) {
				sb.append(separator);
			}
			sb.append(String.format("%02x", buf[i] & 0xff));
		}
		return sb.toString();
	}
}
