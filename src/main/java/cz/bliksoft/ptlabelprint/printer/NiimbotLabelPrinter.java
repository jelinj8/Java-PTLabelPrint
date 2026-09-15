package cz.bliksoft.ptlabelprint.printer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import cz.bliksoft.ptlabelprint.image.BufferedImagePixelSource;
import cz.bliksoft.ptlabelprint.image.ImageRotation;
import cz.bliksoft.ptlabelprint.image.PixelSource;
import cz.bliksoft.ptlabelprint.image.PrinterCapabilities;
import cz.bliksoft.ptlabelprint.protocol.Transport;
import cz.bliksoft.ptlabelprint.protocol.niimbot.AbstractNiimbotPrintTask;
import cz.bliksoft.ptlabelprint.protocol.niimbot.EncodedImage;
import cz.bliksoft.ptlabelprint.protocol.niimbot.LabelType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotImageEncoder;
import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotPrintTasks;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PageColorType;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrintDirection;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrintOptions;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterInfo;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterModel;
import cz.bliksoft.ptlabelprint.protocol.niimbot.PrinterModelMeta;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#NIIMBOT}. Thin wrapper around
 * {@link NiimbotDevice} - {@link #getDevice()} is the real, full-capability API (print flow,
 * heartbeat, RFID, etc.); this class adds the family/definition bookkeeping the abstraction layer
 * needs, plus the common {@link #print}/{@link #getCapabilities()} entry points.
 *
 * <p>
 * Extends {@link AbstractLabelPrinter} directly, not through an intermediate
 * {@code NiimbotLabelPrinter}-family class the way {@link PhomemoDSeriesLabelPrinter} extends
 * {@link PhomemoLabelPrinter}: there is exactly one concrete Niimbot {@link LabelPrinter} subclass
 * and no foreseeable sibling today. If/when a second one is ever needed, extract an intermediate
 * class following {@link PhomemoLabelPrinter}'s exact pattern rather than duplicating its
 * connect/close/isConnected bodies here first.
 */
public class NiimbotLabelPrinter extends AbstractLabelPrinter {

	private final NiimbotDevice device;

	public NiimbotLabelPrinter(PrinterDefinition definition, Transport transport) {
		super(definition, PrinterFamily.NIIMBOT);
		this.device = new NiimbotDevice(transport);
	}

	/** The full Niimbot device API - connect handshake result, printer info, print flow, etc. */
	public NiimbotDevice getDevice() {
		return device;
	}

	@Override
	public void connect() throws IOException, TimeoutException {
		device.connect();
	}

	/** Convenience for {@code getDevice().getPrinterInfo()}, valid after {@link #connect()}. */
	public PrinterInfo getPrinterInfo() {
		return device.getPrinterInfo();
	}

	@Override
	public void close() throws IOException {
		device.disconnect();
	}

	@Override
	public boolean isConnected() {
		return device.isConnected();
	}

	@Override
	public PrinterCapabilities getCapabilities() {
		PrinterModelMeta meta = requireModelMeta();
		return new PrinterCapabilities(meta.getDpi(), meta.getPrintheadPixels(), meta.getDensityMin(), meta.getDensityMax());
	}

	/**
	 * See {@link LabelPrinter}'s own javadoc for the full rotation/density/copies algorithm. This is
	 * a direct generalization of what {@code Cli.NiimbotPrintTestCommand} already does - model/task
	 * lookup, {@link PrintOptions} construction, encode, print - plus the mandatory
	 * {@link PrintDirection} rotation that command never applied.
	 */
	@Override
	public void print(BufferedImage image, PrintJob job) throws IOException, TimeoutException {
		PrinterModelMeta meta = requireModelMeta();
		PrinterModel model = meta.getModel();
		int protocolVersion = device.getPrinterInfo().getProtocolVersion();

		AbstractNiimbotPrintTask task = NiimbotPrintTasks.findPrintTask(model, protocolVersion)
				.map(factory -> factory.create(device, buildPrintOptions(job, meta)))
				.orElseThrow(() -> new IllegalStateException("No print task implemented for model " + model + " yet"));

		boolean mandatoryRotate90 = meta.getPrintDirection() == PrintDirection.LEFT;
		PixelSource source = new BufferedImagePixelSource(image);
		PixelSource rotated = RotationResolver.resolve(source, mandatoryRotate90, meta.getPrintheadPixels(), job.getRotation());
		if (mandatoryRotate90) {
			rotated = ImageRotation.rotate90Clockwise(rotated);
		}

		EncodedImage encoded = NiimbotImageEncoder.encode(rotated, PageColorType.SINGLE_COLOR);

		task.printInit();
		task.printPage(encoded, job.getCopies());
		task.waitForFinished();
		task.printEnd();
	}

	private PrintOptions buildPrintOptions(PrintJob job, PrinterModelMeta meta) {
		int density = job.getDensity() != null ? job.getDensity() : meta.getDensityDefault();
		if (density < meta.getDensityMin() || density > meta.getDensityMax()) {
			throw new IllegalArgumentException("Density " + density + " out of range [" + meta.getDensityMin() + ", "
					+ meta.getDensityMax() + "] for " + meta.getModel());
		}

		return new PrintOptions()
				.setLabelType(job.isContinuousMedia() ? LabelType.CONTINUOUS : LabelType.WITH_GAPS)
				.setDensity(density)
				.setTotalPages(job.getCopies());
	}

	private PrinterModelMeta requireModelMeta() {
		return device.getModelMetadata()
				.orElseThrow(() -> new IllegalStateException("Not connected, or connected model isn't recognized - call connect() first"));
	}
}
