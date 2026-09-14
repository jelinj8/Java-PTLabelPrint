package cz.bliksoft.ptlabelprint.protocol.niimbot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Model → print-task dispatch, ported from niimbluelib's {@code modelPrintTasks}/
 * {@code findPrintTask} (src/print_tasks/index.ts). An exact {@code {model, protocolVersion}}
 * match wins over a bare-{@code model} match - mirrors niimbluelib's own two-level lookup, since a
 * model can switch print tasks across a protocol-version bump (e.g. D110_M uses {@link B1PrintTask}
 * below protocol v4, {@link D110V4PrintTask} at v4).
 *
 * <p>
 * {@link #findPrintTask} returns empty for any model not listed here - most of
 * {@link PrinterModel}'s 77 entries aren't, since niimbluelib itself has no dedicated print task
 * for them either (this project doesn't invent one). Callers must handle that explicitly rather
 * than falling back to a guessed task.
 */
public final class NiimbotPrintTasks {

	/** Builds a print task from a connected device and print options - one per concrete {@link AbstractNiimbotPrintTask} subclass. */
	@FunctionalInterface
	public interface TaskFactory {
		AbstractNiimbotPrintTask create(NiimbotDevice device, PrintOptions options);
	}

	private static final class ModelAndVersion {
		private final PrinterModel model;
		private final int protocolVersion;

		private ModelAndVersion(PrinterModel model, int protocolVersion) {
			this.model = model;
			this.protocolVersion = protocolVersion;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ModelAndVersion)) {
				return false;
			}
			ModelAndVersion other = (ModelAndVersion) o;
			return protocolVersion == other.protocolVersion && model == other.model;
		}

		@Override
		public int hashCode() {
			return Objects.hash(model, protocolVersion);
		}
	}

	private static final Map<PrinterModel, TaskFactory> BY_MODEL = new EnumMap<>(PrinterModel.class);
	private static final Map<ModelAndVersion, TaskFactory> BY_MODEL_AND_VERSION = new HashMap<>();

	static {
		// D11_V1 (OldD11PrintTask)
		registerBareModel(OldD11PrintTask::new, PrinterModel.D11, PrinterModel.D11S);

		// B21_V1 (B21V1PrintTask)
		registerBareModel(B21V1PrintTask::new, PrinterModel.B21);

		// B21_L2B (B21L2BPrintTask)
		registerBareModel(B21L2BPrintTask::new, PrinterModel.B21_L2B);

		// D110 (D110PrintTask) - plus versioned overrides for D11 at protocol v1/v2
		registerBareModel(D110PrintTask::new, PrinterModel.B21S, PrinterModel.B21S_C2B, PrinterModel.D110);
		registerVersioned(D110PrintTask::new, PrinterModel.D11, 1);
		registerVersioned(D110PrintTask::new, PrinterModel.D11, 2);

		// B1 (B1PrintTask)
		registerBareModel(B1PrintTask::new, PrinterModel.D110_M, PrinterModel.B1, PrinterModel.B21_C2B,
				PrinterModel.M2_H, PrinterModel.N1, PrinterModel.D101);

		// D110M_V4 (D110V4PrintTask) - plus a versioned override for D110_M at protocol v4
		registerBareModel(D110V4PrintTask::new, PrinterModel.D11_H, PrinterModel.B21_PRO, PrinterModel.B1_PRO,
				PrinterModel.C1, PrinterModel.EP1C);
		registerVersioned(D110V4PrintTask::new, PrinterModel.D110_M, 4);

		// H1S (H1SPrintTask)
		registerBareModel(H1SPrintTask::new, PrinterModel.H1S);
	}

	private NiimbotPrintTasks() {
	}

	private static void registerBareModel(TaskFactory factory, PrinterModel... models) {
		for (PrinterModel model : models) {
			BY_MODEL.put(model, factory);
		}
	}

	private static void registerVersioned(TaskFactory factory, PrinterModel model, int protocolVersion) {
		BY_MODEL_AND_VERSION.put(new ModelAndVersion(model, protocolVersion), factory);
	}

	/**
	 * The print-task factory for {@code model} at {@code protocolVersion} - an exact
	 * {@code {model, protocolVersion}} match wins over a bare-{@code model} match. Empty if this
	 * model has no ported print task at all.
	 */
	public static Optional<TaskFactory> findPrintTask(PrinterModel model, int protocolVersion) {
		if (model == null) {
			return Optional.empty();
		}

		TaskFactory versioned = BY_MODEL_AND_VERSION.get(new ModelAndVersion(model, protocolVersion));
		if (versioned != null) {
			return Optional.of(versioned);
		}

		return Optional.ofNullable(BY_MODEL.get(model));
	}
}
