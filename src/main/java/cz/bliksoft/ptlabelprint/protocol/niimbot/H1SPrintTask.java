package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Print flow for the {@code H1S} print task - ported from niimbluelib's {@code H1SPrintTask}
 * (src/print_tasks/H1SPrintTask.ts). Per {@link NiimbotPrintTasks}' dispatch table, this is the
 * print task the H1S uses. The simplest task in this package: a 2-byte {@code PrintStart}
 * ({@link PacketGenerator#printStart2b}), a 9-byte page size
 * ({@link PacketGenerator#setPageSize9b}, cut height/type always 0 - niimbluelib doesn't expose
 * them for this task either), and otherwise identical to {@link AbstractNiimbotPrintTask}'s shared
 * defaults for {@code waitForFinished}/{@code printEnd}/{@code isSupportColor} - no overrides
 * needed for any of them.
 */
public class H1SPrintTask extends AbstractNiimbotPrintTask {

	public H1SPrintTask(NiimbotDevice device, PrintOptions options) {
		super(device, options);
	}

	@Override
	public void printInit() throws IOException, TimeoutException {
		device.sendAllRaw(Arrays.asList(PacketGenerator.setDensity(options.getDensity()),
				PacketGenerator.setLabelType(options.getLabelType().getCode()),
				PacketGenerator.printStart2b(options.getTotalPages())));
	}

	@Override
	public void printPage(EncodedImage image, int quantity) throws IOException, TimeoutException {
		validatePage(image, quantity);

		List<NiimbotPacket> pkts = new ArrayList<>();
		pkts.add(PacketGenerator.pageStart());
		pkts.add(PacketGenerator.setPageSize9b(image.getRows(), image.getCols(), quantity, 0, 0));
		pkts.addAll(PacketGenerator.writeImageDataSingleColor(image, printheadPixels()));
		pkts.add(PacketGenerator.pageEnd());

		device.sendAllRaw(pkts, options.getPageTimeoutMs());
	}
}
