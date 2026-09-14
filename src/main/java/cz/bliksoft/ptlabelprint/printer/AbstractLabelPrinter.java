package cz.bliksoft.ptlabelprint.printer;

/**
 * Cross-family {@link LabelPrinter} plumbing: just the {@link PrinterDefinition} field/accessor and
 * the family-validation-in-constructor pattern every concrete subclass needs. Deliberately does
 * <b>not</b> implement {@code connect()}/{@code close()}/{@code isConnected()} - those delegate to
 * genuinely different underlying objects per family ({@link cz.bliksoft.ptlabelprint.protocol.niimbot.NiimbotDevice}
 * vs. a bare {@link cz.bliksoft.ptlabelprint.protocol.BleTransport}), so only the *shape* (a single
 * delegate call) is shareable at this cross-family level, not a body - see {@link PhomemoLabelPrinter}
 * for where a real shared body lives, once a family actually has more than one concrete subclass.
 */
public abstract class AbstractLabelPrinter implements LabelPrinter {

	private final PrinterDefinition definition;

	protected AbstractLabelPrinter(PrinterDefinition definition, PrinterFamily expectedFamily) {
		if (definition.getFamily() != expectedFamily) {
			throw new IllegalArgumentException("Definition " + definition + " is not a " + expectedFamily + " printer");
		}
		this.definition = definition;
	}

	@Override
	public PrinterDefinition getDefinition() {
		return definition;
	}
}
