package cz.bliksoft.ptlabelprint;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CliTest {

	@Test
	void executesWithoutArgs() {
		int exitCode = new CommandLine(new Cli()).execute();
		assertEquals(0, exitCode);
	}

	@Test
	void reportsVersion() {
		int exitCode = new CommandLine(new Cli()).execute("--version");
		assertEquals(0, exitCode);
	}

	@Test
	void parsesPatternSize() {
		assertArrayEquals(new double[] {40, 20}, Cli.PrintTestCommand.parseSizeMm("40x20"));
		assertArrayEquals(new double[] {100, 150.5}, Cli.PrintTestCommand.parseSizeMm("100X150.5"));
		assertNull(Cli.PrintTestCommand.parseSizeMm("40"));
		assertNull(Cli.PrintTestCommand.parseSizeMm("40xabc"));
		assertNull(Cli.PrintTestCommand.parseSizeMm("0x20"));
	}

	@Test
	void alignmentPatternHasRequestedSizeAndAsymmetricMarker() {
		java.awt.image.BufferedImage img = Cli.buildAlignmentPattern(320, 160, 8);
		assertEquals(320, img.getWidth());
		assertEquals(160, img.getHeight());
		// square near the top-right corner is black, the mirrored spot near the top-left bar's right is white
		assertEquals(0xff000000, img.getRGB(320 - 24, 24));
		assertEquals(0xffffffff, img.getRGB(40, 24));
	}
}
