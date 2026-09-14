package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import cz.bliksoft.ptlabelprint.protocol.Transport;

/**
 * High-level, typed API for a Niimbot-branded printer (D11_H, M2 - <b>not</b> Phomemo's D/Q-series,
 * which speak an unrelated protocol; see this package's own package-info). Combines what
 * niimbluelib splits across {@code NiimbotAbstractClient} and {@code NiimbotProtocol} - a single
 * concrete class here since this port doesn't need client-type polymorphism at this layer (that's
 * {@link Transport}'s job instead).
 *
 * <p>
 * Connect/negotiate/info/heartbeat/misc-config commands, plus the low-level page/print
 * start-end and bitmap-row primitives {@link D110V4PrintTask} builds the one print flow this
 * project has ported on top of. Not thread-safe for concurrent calls from multiple threads
 * (mirrors niimbluelib's single in-flight request assumption, enforced here with a
 * {@link ReentrantLock}).
 */
public class NiimbotDevice {

	private static final long DEFAULT_PACKET_TIMEOUT_MS = 1000;

	private final Transport transport;
	private final PrinterInfo info = new PrinterInfo();
	private final ReentrantLock lock = new ReentrantLock();
	private final BlockingQueue<NiimbotPacket> resultQueue = new ArrayBlockingQueue<>(1);

	private volatile List<ResponseCommandId> awaitedIds;
	private byte[] packetBuf = new byte[0];
	private long packetTimeoutMs = DEFAULT_PACKET_TIMEOUT_MS;
	private volatile boolean debug;

	public NiimbotDevice(Transport transport) {
		this.transport = transport;
	}

	public long getPacketTimeout() {
		return packetTimeoutMs;
	}

	public void setPacketTimeout(long packetTimeoutMs) {
		this.packetTimeoutMs = packetTimeoutMs;
	}

	/** When enabled, prints every raw TX/RX byte sequence (and dropped unsolicited packets) to stderr - mirrors niimbluelib's own {@code setDebug}. */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	// --- connection lifecycle ---------------------------------------------------------------

	/**
	 * Opens the transport, performs the connect handshake, and fetches printer info. Ported from
	 * niimbluelib's {@code NiimbotBluetoothClient.connect()} + {@code negotiateAndGetPrinterInfo}.
	 * On failure, the transport is disconnected before the exception propagates.
	 */
	public PrinterInfo connect() throws IOException, TimeoutException {
		transport.setRawDataListener(this::onRawData);
		transport.connect();

		try {
			ConnectNegotiateResult negotiated = connectNegotiate();
			info.setConnectResult(negotiated.getConnectResult());
			info.setProtocolVersion(negotiated.getProtocolVersion());
			info.setSupportColor(negotiated.isSupportColor());
		} catch (IOException | TimeoutException | RuntimeException e) {
			closeQuietly();
			throw e;
		}

		try {
			fetchPrinterInfo();
		} catch (IOException | TimeoutException | RuntimeException e) {
			closeQuietly();
			throw e;
		}

		return info;
	}

	public void disconnect() throws IOException {
		transport.disconnect();
	}

	private void closeQuietly() {
		try {
			transport.disconnect();
		} catch (IOException ignored) {
			// best-effort cleanup after a failed connect
		}
	}

	public boolean isConnected() {
		return transport.isConnected();
	}

	public PrinterInfo getPrinterInfo() {
		return info;
	}

	/** Looks up this printer's capabilities in {@link PrinterModels}, once {@link #connect()} has populated the model ID. */
	public Optional<PrinterModelMeta> getModelMetadata() {
		Integer modelId = info.getModelId();
		return modelId == null ? Optional.empty() : PrinterModels.findById(modelId);
	}

	// --- connect negotiate / printer info ----------------------------------------------------

	/** Sends the connect packet and, on protocol v3, fetches the protocol version/color support. */
	public ConnectNegotiateResult connectNegotiate() throws IOException, TimeoutException {
		ConnectResult connectResult = sendConnect();
		int protocolVersion = 0;
		boolean supportColor = false;

		if (connectResult == ConnectResult.CONNECTED_NEW) {
			protocolVersion = 1;
		} else if (connectResult == ConnectResult.CONNECTED_V3) {
			PrinterStatusData statusData = getPrinterStatusData();
			protocolVersion = statusData.getProtocolVersion();
			supportColor = statusData.isSupportColor();
		}

		return new ConnectNegotiateResult(connectResult, protocolVersion, supportColor);
	}

	/**
	 * Fetches printer info and stores it. The model ID is required (an exception propagates if it
	 * can't be read); every other field is best-effort - a failure just leaves that field unset,
	 * matching niimbluelib's own {@code fetchPrinterInfo} (which logs a console warning per field
	 * instead; this port has no logging facade yet, so failures are silently swallowed here).
	 */
	public PrinterInfo fetchPrinterInfo() throws IOException, TimeoutException {
		info.setModelId(getPrinterModel());

		try {
			info.setSerial(getPrinterSerialNumber());
		} catch (IOException | TimeoutException | RuntimeException ignored) {
		}
		try {
			info.setMac(getPrinterBluetoothMacAddress());
		} catch (IOException | TimeoutException | RuntimeException ignored) {
		}
		try {
			info.setBatteryPercents(getBatteryChargeLevel());
		} catch (IOException | TimeoutException | RuntimeException ignored) {
		}
		try {
			info.setAutoShutdownTime(getAutoShutDownTime());
		} catch (IOException | TimeoutException | RuntimeException ignored) {
		}
		try {
			info.setLabelType(getLabelType());
		} catch (IOException | TimeoutException | RuntimeException ignored) {
		}

		try {
			HeartbeatPrinterInfoData hi = heartbeatPrinterInfo();
			info.setPrintheadWidth(hi.getPrintheadWidth());
			info.setHardwareVersion(hi.getHardwareVersion());
			info.setSoftwareVersion(hi.getSoftwareVersion());
			info.setResolutionClass(hi.getResolutionClass());
		} catch (IOException | TimeoutException | RuntimeException e) {
			try {
				info.setHardwareVersion(getHardwareVersion());
			} catch (IOException | TimeoutException | RuntimeException ignored) {
			}
			try {
				info.setSoftwareVersion(getSoftwareVersion());
			} catch (IOException | TimeoutException | RuntimeException ignored) {
			}
		}

		return info;
	}

	// --- typed commands -----------------------------------------------------------------------

	public ConnectResult sendConnect() throws IOException, TimeoutException {
		return PacketParser.parseConnectResponse(send(PacketGenerator.connect()));
	}

	public PrinterStatusData getPrinterStatusData() throws IOException, TimeoutException {
		return PacketParser.parsePrinterStatusDataResponse(send(PacketGenerator.getPrinterStatusData()));
	}

	public int getPrinterModel() throws IOException, TimeoutException {
		return PacketParser.parsePrinterInfoModelIdResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.PRINTER_MODEL_ID)));
	}

	/** Read paper NFC tag info - see {@link RfidInfo}'s javadoc for how Niimbot's consumable lock shows up here. */
	public RfidInfo rfidInfo() throws IOException, TimeoutException {
		return PacketParser.parseRfidInfoResponse(send(PacketGenerator.rfidInfo()));
	}

	/** Read ribbon NFC tag info. */
	public RfidInfo rfidInfo2() throws IOException, TimeoutException {
		return PacketParser.parseRfidInfoResponse(send(PacketGenerator.rfidInfo2()));
	}

	public HeartbeatPrinterInfoData heartbeatPrinterInfo() throws IOException, TimeoutException {
		return PacketParser.parseHeartbeatPrinterInfoResponse(send(PacketGenerator.heartbeat(HeartbeatType.PRINTER_INFO)));
	}

	/** Sends a single heartbeat (no repeating timer - call this periodically yourself if desired). */
	public HeartbeatData heartbeat() throws IOException, TimeoutException {
		HeartbeatType type = info.getProtocolVersion() >= 3 ? HeartbeatType.ADVANCED_2 : HeartbeatType.ADVANCED_1;
		NiimbotPacket packet = sendPacketWaitResponse(PacketGenerator.heartbeat(type), 500);
		ResponseCommandId cmd = ResponseCommandId.fromCode(packet.getCommand()).orElse(null);

		HeartbeatData data;
		if (cmd == ResponseCommandId.IN_HEARTBEAT_ADVANCED_1) {
			data = PacketParser.parseHeartbeatAdvanced1Response(packet, info.getModelId());
		} else if (cmd == ResponseCommandId.IN_HEARTBEAT_ADVANCED_2) {
			data = PacketParser.parseHeartbeatAdvanced2Response(packet);
		} else {
			throw new IOException("Unsupported heartbeat response " + cmd);
		}

		if (data.getBatteryPercents() != null) {
			info.setBatteryPercents(data.getBatteryPercents());
		}
		return data;
	}

	public int getBatteryChargeLevel() throws IOException, TimeoutException {
		return PacketParser.parseBatteryChargeLevelResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.BATTERY_CHARGE_LEVEL)));
	}

	public AutoShutdownTime getAutoShutDownTime() throws IOException, TimeoutException {
		int code = PacketParser.parseAutoShutdownTimeResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.AUTO_SHUTDOWN_TIME)));
		return AutoShutdownTime.fromCode(code)
				.orElseThrow(() -> new NiimbotProtocolException("Unknown auto shutdown time " + code));
	}

	public void setAutoShutDownTime(AutoShutdownTime time) throws IOException, TimeoutException {
		send(PacketGenerator.setAutoShutDownTime(time));
	}

	public String getSoftwareVersion() throws IOException, TimeoutException {
		return PacketParser.parsePrinterVersionResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.SOFTWARE_VERSION)));
	}

	public String getHardwareVersion() throws IOException, TimeoutException {
		return PacketParser.parsePrinterVersionResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.HARDWARE_VERSION)));
	}

	public LabelType getLabelType() throws IOException, TimeoutException {
		return PacketParser.parseLabelTypeResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.LABEL_TYPE)));
	}

	public String getPrinterSerialNumber() throws IOException, TimeoutException {
		return PacketParser.parsePrinterSerialNumberResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.SERIAL_NUMBER)));
	}

	public String getPrinterBluetoothMacAddress() throws IOException, TimeoutException {
		return PacketParser.parsePrinterBluetoothMacAddressResponse(send(PacketGenerator.getPrinterInfo(PrinterInfoType.BLUETOOTH_ADDRESS)));
	}

	/** Clears printer settings (sound and maybe some others - matches niimbluelib's own uncertainty about the full scope). */
	public void printerReset() throws IOException, TimeoutException {
		send(PacketGenerator.printerReset());
	}

	public PrintStatus getPrintStatus() throws IOException, TimeoutException {
		PrintStatus status = PacketParser.parsePrintStatusResponse(send(PacketGenerator.printStatus()));
		if (status.getError() != 0) {
			throw new NiimbotPrintException("Print error " + status.getError(), status.getError());
		}
		return status;
	}

	// --- print flow primitives (used by D110V4PrintTask) --------------------------------------

	/** False returned when pageStart is refused. */
	public boolean pageStart() throws IOException, TimeoutException {
		return PacketParser.parseBooleanResponse(send(PacketGenerator.pageStart()));
	}

	/** False returned when pageEnd is refused. */
	public boolean pageEnd() throws IOException, TimeoutException {
		return PacketParser.parseBooleanResponse(send(PacketGenerator.pageEnd()));
	}

	/** False returned when printEnd is refused. */
	public boolean printEnd() throws IOException, TimeoutException {
		return PacketParser.parseBooleanResponse(send(PacketGenerator.printEnd()));
	}

	/**
	 * Sends an arbitrary pre-built packet and waits for its response (unless one-way) - the
	 * generic escape hatch print-task classes use for packets ({@code printStart9b},
	 * {@code setPageSize13b}, bitmap rows, ...) that don't warrant their own typed method here.
	 */
	public NiimbotPacket sendRaw(NiimbotPacket packet) throws IOException, TimeoutException {
		return send(packet);
	}

	/** Like {@link #sendRaw(NiimbotPacket)}, but with an explicit per-packet timeout instead of {@link #getPacketTimeout()}. */
	public NiimbotPacket sendRaw(NiimbotPacket packet, long timeoutMs) throws IOException, TimeoutException {
		return sendPacketWaitResponse(packet, timeoutMs);
	}

	/** Like {@link #sendRaw(NiimbotPacket)}, but for a whole sequence, sent one at a time in order. */
	public void sendAllRaw(List<NiimbotPacket> packets) throws IOException, TimeoutException {
		for (NiimbotPacket p : packets) {
			send(p);
		}
	}

	/** Like {@link #sendAllRaw(List)}, but with an explicit per-packet timeout. */
	public void sendAllRaw(List<NiimbotPacket> packets, long timeoutMs) throws IOException, TimeoutException {
		for (NiimbotPacket p : packets) {
			sendPacketWaitResponse(p, timeoutMs);
		}
	}

	/**
	 * Polls {@link #getPrintStatus()} every {@code pollIntervalMs} until {@code page == pagesToPrint}.
	 * Ported from niimbluelib's {@code waitUntilPrintFinishedByStatusPoll} - simplified to plain
	 * blocking (this is a synchronous library), so unlike niimbluelib this has no
	 * {@code printprogress} event; the caller can inspect {@link PrintStatus} itself each poll if
	 * it wants progress.
	 */
	public void waitUntilPrintFinishedByStatusPoll(int pagesToPrint, long pollIntervalMs, long overallTimeoutMs)
			throws IOException, TimeoutException {
		long deadline = System.currentTimeMillis() + overallTimeoutMs;

		while (true) {
			PrintStatus status = getPrintStatus();
			if (status.getPage() == pagesToPrint) {
				return;
			}
			if (System.currentTimeMillis() > deadline) {
				throw new TimeoutException("Timeout waiting for print to finish (last status: page="
						+ status.getPage() + "/" + pagesToPrint + ")");
			}
			sleep(pollIntervalMs);
		}
	}

	/**
	 * Like {@link #waitUntilPrintFinishedByStatusPoll}, but for tasks (e.g. {@code B21V1PrintTask})
	 * whose printer reports completion only via {@link #printEnd()} returning {@code true} on a
	 * retry, not via {@link #getPrintStatus()}. Ported from niimbluelib's
	 * {@code waitUntilPrintFinishedByPrintEndPoll}.
	 */
	public void waitUntilPrintFinishedByPrintEndPoll(long pollIntervalMs, long overallTimeoutMs) throws IOException, TimeoutException {
		long deadline = System.currentTimeMillis() + overallTimeoutMs;

		while (true) {
			if (printEnd()) {
				return;
			}
			if (System.currentTimeMillis() > deadline) {
				throw new TimeoutException("Timeout waiting for print end poll");
			}
			sleep(pollIntervalMs);
		}
	}

	/**
	 * Blocks until an <i>unsolicited</i> {@code IN_PRINTER_PAGE_INDEX} packet reports
	 * {@code pagesToPrint} - unlike every other {@code waitUntilPrintFinished*} method, the printer
	 * pushes these on its own rather than responding to a request, so this listens passively
	 * instead of sending anything. {@code timeoutMs} resets on every page-index packet received
	 * (matches niimbluelib's own {@code waitUntilPrintFinishedByPageIndex}). Used by
	 * {@code OldD11PrintTask}.
	 */
	public void waitForPageIndex(int pagesToPrint, long timeoutMs) throws IOException, TimeoutException {
		lock.lock();
		try {
			resultQueue.clear();
			awaitedIds = Collections.singletonList(ResponseCommandId.IN_PRINTER_PAGE_INDEX);
			long deadline = System.currentTimeMillis() + timeoutMs;

			while (true) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					throw new TimeoutException("Timeout waiting for page index " + pagesToPrint);
				}

				NiimbotPacket packet;
				try {
					packet = resultQueue.poll(remaining, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for page index", e);
				}
				if (packet == null) {
					throw new TimeoutException("Timeout waiting for page index " + pagesToPrint);
				}

				byte[] data = packet.getData();
				int page = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
				deadline = System.currentTimeMillis() + timeoutMs;
				if (page == pagesToPrint) {
					return;
				}
			}
		} finally {
			awaitedIds = null;
			lock.unlock();
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for print to finish", e);
		}
	}

	// --- packet send/receive plumbing ---------------------------------------------------------

	private NiimbotPacket send(NiimbotPacket packet) throws IOException, TimeoutException {
		return sendPacketWaitResponse(packet, packetTimeoutMs);
	}

	/**
	 * Sends a packet and, unless it's one-way, blocks for up to {@code timeoutMs} for a response
	 * matching its {@link NiimbotPacket#getValidResponseIds()}. Ported from niimbluelib's
	 * {@code sendPacketWaitResponse}/{@code waitForPacket}, simplified to plain blocking (this is a
	 * synchronous library, not an event-emitter-based one) and serialized with a lock instead of an
	 * async mutex.
	 */
	private NiimbotPacket sendPacketWaitResponse(NiimbotPacket packet, long timeoutMs) throws IOException, TimeoutException {
		lock.lock();
		try {
			resultQueue.clear();
			awaitedIds = packet.getValidResponseIds();

			byte[] bytes = packet.toBytes();
			if (debug) {
				System.err.println("TX " + toHex(bytes));
			}
			transport.write(bytes);

			if (packet.isOneWay()) {
				return null;
			}

			NiimbotPacket response;
			try {
				response = resultQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while waiting for response", e);
			}

			if (response == null) {
				throw new TimeoutException("Timeout waiting response for " + packet.getValidResponseIds());
			}

			ResponseCommandId cmd = ResponseCommandId.fromCode(response.getCommand()).orElse(null);

			if (cmd == ResponseCommandId.IN_PRINT_ERROR) {
				int reasonId = response.getData().length > 0 ? (response.getData()[0] & 0xff) : 0;
				String name = PrinterErrorCode.fromCode(reasonId).map(Enum::name).orElse("UNKNOWN");
				throw new NiimbotPrintException("Print error " + reasonId + ": " + name, reasonId);
			}
			if (cmd == ResponseCommandId.NOT_SUPPORTED) {
				throw new NiimbotPrintException("Feature not supported", 0);
			}

			return response;
		} finally {
			awaitedIds = null;
			lock.unlock();
		}
	}

	/**
	 * Feeds raw bytes from the transport, reassembling and dispatching complete packets. Ported
	 * from niimbluelib's {@code processRawPacket} - an incomplete trailing frame is buffered and
	 * completed by a later call, rather than treated as an error.
	 */
	private void onRawData(byte[] data) {
		if (data.length == 0) {
			return;
		}

		if (debug) {
			System.err.println("RX raw " + toHex(data));
		}

		byte[] combined = new byte[packetBuf.length + data.length];
		System.arraycopy(packetBuf, 0, combined, 0, packetBuf.length);
		System.arraycopy(data, 0, combined, packetBuf.length, data.length);
		packetBuf = combined;

		if (packetBuf.length > 1 && !NiimbotPacket.hasSubarrayAtPos(packetBuf, NiimbotPacket.HEAD, 0)) {
			packetBuf = new byte[0];
			return;
		}

		List<NiimbotPacket> packets;
		try {
			packets = PacketParser.parsePacketBundle(packetBuf);
		} catch (NiimbotProtocolException e) {
			// buffer doesn't yet contain a whole frame - wait for more data
			return;
		}

		if (!packets.isEmpty()) {
			packetBuf = new byte[0];
			for (NiimbotPacket p : packets) {
				dispatch(p);
			}
		}
	}

	private void dispatch(NiimbotPacket p) {
		List<ResponseCommandId> ids = awaitedIds;
		if (ids == null) {
			return; // nobody waiting - drop the unsolicited packet
		}

		ResponseCommandId cmd = ResponseCommandId.fromCode(p.getCommand()).orElse(null);
		boolean matches = ids.isEmpty() || ids.contains(cmd) || cmd == ResponseCommandId.IN_PRINT_ERROR
				|| cmd == ResponseCommandId.NOT_SUPPORTED;

		if (matches) {
			resultQueue.offer(p);
		} else if (debug) {
			System.err.println("RX dropped (unsolicited, awaiting " + ids + "): cmd=0x" + Integer.toHexString(p.getCommand()));
		}
	}

	private static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02x ", b & 0xff));
		}
		return sb.toString().trim();
	}
}
