package cz.bliksoft.ptlabelprint.protocol.niimbot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotPrintTasks.TaskFactory;

class NiimbotPrintTasksTest {

	@Test
	void d11HResolvesToD110V4PrintTask() {
		Optional<TaskFactory> factory = NiimbotPrintTasks.findPrintTask(PrinterModel.D11_H, 5);
		assertTrue(factory.isPresent());
		AbstractNiimbotPrintTask task = factory.get().create(null, new PrintOptions());
		assertTrue(task instanceof D110V4PrintTask);
	}

	@Test
	void m2HResolvesToB1PrintTask() {
		Optional<TaskFactory> factory = NiimbotPrintTasks.findPrintTask(PrinterModel.M2_H, 4);
		assertTrue(factory.isPresent());
		AbstractNiimbotPrintTask task = factory.get().create(null, new PrintOptions());
		assertTrue(task instanceof B1PrintTask);
	}

	@Test
	void versionedOverrideBeatsBareModelMatch() {
		// D110_M is bare-mapped to B1PrintTask, but protocol v4 specifically overrides to D110V4PrintTask.
		Optional<TaskFactory> v4 = NiimbotPrintTasks.findPrintTask(PrinterModel.D110_M, 4);
		assertTrue(v4.get().create(null, new PrintOptions()) instanceof D110V4PrintTask);

		Optional<TaskFactory> v3 = NiimbotPrintTasks.findPrintTask(PrinterModel.D110_M, 3);
		assertTrue(v3.get().create(null, new PrintOptions()) instanceof B1PrintTask);
	}

	@Test
	void modelWithNoPortedTaskResolvesEmpty() {
		// A63 isn't in niimbluelib's own modelPrintTasks dispatch table either - no task to invent.
		assertFalse(NiimbotPrintTasks.findPrintTask(PrinterModel.A63, 1).isPresent());
	}
}
