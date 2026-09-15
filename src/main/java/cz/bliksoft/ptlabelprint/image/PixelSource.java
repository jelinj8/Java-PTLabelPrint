package cz.bliksoft.ptlabelprint.image;

/**
 * A monochrome (plus optional red for double-color printers) pixel source - the cross-family image
 * abstraction every protocol family's own converter builds from (see
 * {@code protocol.niimbot.NiimbotImageEncoder#encode} and
 * {@code protocol.phomemo.RasterImage#fromPixelSource}). Moved here from {@code protocol.niimbot}
 * (where it was ported from niimbluelib's {@code ImageSource}) once {@code protocol.phomemo} needed
 * it too - the same move this project already made for {@code Transport}/{@code BleTransport}.
 *
 * <p>
 * No built-in rotation here - callers/converters rotate as needed themselves (see
 * {@link ImageRotation}), since exactly what rotation is required is a per-family/per-model
 * decision (e.g. a Niimbot model's {@code PrintDirection}), not something this interface can know.
 */
public interface PixelSource {

	int getWidth();

	int getHeight();

	boolean isBlack(int x, int y);

	/** Only meaningful for double-color printers; return false for single-color use. */
	default boolean isRed(int x, int y) {
		return false;
	}
}
