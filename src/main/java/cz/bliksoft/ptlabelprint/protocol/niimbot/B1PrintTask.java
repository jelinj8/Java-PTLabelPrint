package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code B1} print task - ported from niimbluelib's {@code B1PrintTask}
 * (src/print_tasks/B1PrintTask.ts). Per niimbluelib's own {@code modelPrintTasks} dispatch table
 * (see {@link NiimbotPrintTasks}), this is the print task Niimbot's <b>M2_H</b> uses (also B1,
 * D110_M protocol &lt; 4, B21_C2B, N1, D101) - despite the similarly-named {@link D110V4PrintTask}
 * (the {@code D110M_V4} task), which the M2_H does <b>not</b> use. Simpler than
 * {@link D110V4PrintTask}: no B21_PRO one-way-PrintStatus quirk before the image rows, no
 * one-way-Heartbeat quirk after {@code PrintEnd} (so it uses {@link AbstractNiimbotPrintTask}'s
 * plain default {@link #printEnd()} unmodified), and an explicit {@code PageStart} the D110M_V4
 * flow doesn't send.
 *
 * <p>
 * Usage mirrors {@link D110V4PrintTask}'s own javadoc example.
 */
public class B1PrintTask extends AbstractNiimbotPrintTask {

	public B1PrintTask(NiimbotDevice device, PrintOptions options) {
		super(device, options);
	}

	@Override
	public boolean isSupportColor(PageColorType pageColor) {
		return pageColor == PageColorType.SINGLE_COLOR || pageColor == PageColorType.DOUBLE_COLOR;
	}

	/** Sets density, label type, and sends the print-start command. */
	@Override
	public void printInit() throws IOException, TimeoutException {
		device.sendAllRaw(Arrays.asList(PacketGenerator.setDensity(options.getDensity()),
				PacketGenerator.setLabelType(options.getLabelType().getCode()),
				PacketGenerator.printStart7b(options.getTotalPages(), options.getPageColor())));
	}

	/** Prints one page. Ported from niimbluelib's own {@code printPage}. */
	@Override
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.pageStart());
		pkts.add(PacketGenerator.setPageSize6b(image.getRows(), image.getCols(), quantity));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels()));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}
}
