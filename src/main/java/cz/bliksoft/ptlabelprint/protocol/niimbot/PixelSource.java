package cz.bliksoft.ptlabelprint.protocol.niimbot;

/**
 * A monochrome (plus optional red for double-color printers) pixel source for
 * {@link NiimbotImageEncoder#encode}. Simplified from niimbluelib's {@code ImageSource}: no
 * built-in {@code printDirection} rotation here - callers pre-rotate/pre-orient pixels themselves
 * if a model's {@code PrintDirection} requires it (see {@link PrinterModelMeta#getPrintDirection()}).
 */
public interface PixelSource {

	int getWidth();

	int getHeight();

	boolean isBlack(int x, int y);

	/** Only meaningful for {@link PageColorType#DOUBLE_COLOR} printers; return false for single-color use. */
	default boolean isRed(int x, int y) {
		return false;
	}
}
