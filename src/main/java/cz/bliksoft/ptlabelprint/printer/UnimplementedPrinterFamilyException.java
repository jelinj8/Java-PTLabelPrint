package cz.bliksoft.ptlabelprint.printer;

/**
 * Thrown by {@link PrinterFactory#create} when a {@link PrinterDefinition} was correctly
 * <i>identified</i> (its {@link PrinterFamily} is a real, cataloged family) but that family has no
 * {@link LabelPrinter} implementation yet - distinct from "not recognized at all"
 * ({@link PrinterCatalog#detect} returning no match, which never reaches {@link PrinterFactory}).
 * Extends {@link IllegalArgumentException} so existing {@code catch (IllegalArgumentException)}
 * callers keep working unchanged; catch this specifically to distinguish the two outcomes, e.g. to
 * show the user "detected as X, but X isn't implemented yet" instead of a generic factory error.
 */
public class UnimplementedPrinterFamilyException extends IllegalArgumentException {

	private final PrinterDefinition definition;

	public UnimplementedPrinterFamilyException(PrinterDefinition definition) {
		super("Definition " + definition + " was recognized as family " + definition.getFamily()
				+ ", but that family has no LabelPrinter implementation yet (cataloged, not printable) - see CLAUDE.md's \"Protocol families\" section.");
		this.definition = definition;
	}

	public PrinterDefinition getDefinition() {
		return definition;
	}
}
