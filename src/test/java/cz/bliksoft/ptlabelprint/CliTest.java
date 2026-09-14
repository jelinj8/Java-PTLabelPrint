package cz.bliksoft.ptlabelprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
