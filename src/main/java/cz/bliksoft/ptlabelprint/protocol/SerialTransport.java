package cz.bliksoft.ptlabelprint.protocol;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import com.fazecast.jSerialComm.SerialPort;

/**
 * {@link Transport} over a serial port (jSerialComm) - a real one, a USB CDC one, or, the case this
 * was built for, the COM port an OS creates for a paired classic-Bluetooth device's Serial Port
 * Profile. Phomemo's own app talks to the M421 that way rather than over BLE, at roughly 2.5x the
 * throughput the printer accepts over BLE (see CLAUDE.md, "M421: what the official app does").
 *
 * <p>
 * The link does its own flow control, so {@link #writeStream} ignores the chunking/delay a BLE
 * caller asks for and just writes the data - that pacing exists to stand in for flow control BLE
 * write-without-response doesn't have.
 *
 * <p>
 * jSerialComm is a {@code provided} dependency: only an application that actually uses this class
 * needs it on its classpath.
 */
public class SerialTransport implements Transport {

	private static final int READ_TIMEOUT_MS = 100;

	private final String portName;
	private final int baudRate;
	private SerialPort port;
	private Thread reader;
	private volatile Consumer<byte[]> rawDataListener;

	/**
	 * @param portName  e.g. {@code COM7} or {@code /dev/rfcomm0}
	 * @param baudRate  ignored by Bluetooth and USB CDC ports, which have no real line speed
	 */
	public SerialTransport(String portName, int baudRate) {
		this.portName = portName;
		this.baudRate = baudRate;
	}

	@Override
	public void connect() throws IOException {
		SerialPort p = SerialPort.getCommPort(portName);
		p.setBaudRate(baudRate);
		p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, READ_TIMEOUT_MS, 0);
		if (!p.openPort()) {
			throw new IOException("Could not open serial port " + portName);
		}
		port = p;

		reader = new Thread(this::readLoop, "serial-transport-reader");
		reader.setDaemon(true);
		reader.start();
	}

	private void readLoop() {
		byte[] buf = new byte[1024];
		SerialPort p = port;
		while (p != null && p.isOpen()) {
			int n = p.readBytes(buf, buf.length);
			if (n < 0) {
				return;
			}
			Consumer<byte[]> listener = rawDataListener;
			if (n > 0 && listener != null) {
				listener.accept(Arrays.copyOf(buf, n));
			}
		}
	}

	@Override
	public void disconnect() {
		SerialPort p = port;
		port = null;
		if (p != null) {
			p.closePort();
		}
	}

	@Override
	public boolean isConnected() {
		SerialPort p = port;
		return p != null && p.isOpen();
	}

	@Override
	public void write(byte[] data) throws IOException {
		SerialPort p = port;
		if (p == null) {
			throw new IOException("Serial port " + portName + " is not open");
		}
		int offset = 0;
		while (offset < data.length) {
			int n = p.writeBytes(data, data.length - offset, offset);
			if (n < 0) {
				throw new IOException("Write to serial port " + portName + " failed");
			}
			offset += n;
		}
	}

	@Override
	public void writeStream(byte[] data, int chunkSize, long chunkDelayMs) throws IOException {
		write(data);
	}

	@Override
	public void setRawDataListener(Consumer<byte[]> listener) {
		this.rawDataListener = listener;
	}
}
