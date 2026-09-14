package cz.bliksoft.ptlabelprint.printer;

import java.io.IOException;
import java.util.function.IntConsumer;

import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.ptlabelprint.protocol.phomemo.DSeriesPrinter;
import cz.bliksoft.ptlabelprint.protocol.phomemo.RasterImage;

/**
 * {@link LabelPrinter} for {@link PrinterFamily#PHOMEMO_D_SERIES}. Connection lifecycle is entirely
 * inherited from {@link PhomemoLabelPrinter} (this protocol family has no connect-handshake/
 * info-query concept at all - see {@link DSeriesPrinter}'s javadoc) - {@link #print} is the one
 * real capability this class adds, a direct pass-through to {@link DSeriesPrinter#print}.
 */
public class PhomemoDSeriesLabelPrinter extends PhomemoLabelPrinter {

	public PhomemoDSeriesLabelPrinter(PrinterDefinition definition, BlePeripheral peripheral) {
		super(definition, PrinterFamily.PHOMEMO_D_SERIES, peripheral);
	}

	/** See {@link DSeriesPrinter#print} - the image height constraint documented there still applies. */
	public void print(RasterImage image, int density, boolean continuous, int feedDots, IntConsumer onProgress) throws IOException {
		DSeriesPrinter.print(transport, image, density, continuous, feedDots, onProgress);
	}
}
