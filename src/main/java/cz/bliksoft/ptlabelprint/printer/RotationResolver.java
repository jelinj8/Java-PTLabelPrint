package cz.bliksoft.ptlabelprint.printer;

import cz.bliksoft.ptlabelprint.image.ImageRotation;
import cz.bliksoft.ptlabelprint.image.PixelSource;

/**
 * Shared rotation-decision logic both {@code NiimbotLabelPrinter}/{@code PhomemoDSeriesLabelPrinter}
 * use - see {@link LabelPrinter#print}'s own javadoc for the full algorithm this implements.
 * Returns the image <b>before</b> the caller's own family/model-mandatory orientation transform -
 * the caller always applies that separately, on top of whatever this returns (0 or 1 extra 90-degree
 * pre-rotation for {@code AUTO}, or the caller's exact explicit choice).
 */
public final class RotationResolver {

	private RotationResolver() {
	}

	/**
	 * @param source            the original, unrotated source image
	 * @param mandatoryRotate90 whether the connected family/model's own mandatory orientation
	 *                          transform (applied separately, by the caller, after this method
	 *                          returns) is a 90-degree clockwise rotation
	 * @param printheadPixels   the connected printer's physical printhead width - only consulted for
	 *                          {@link Rotation#AUTO}
	 * @param rotation          the caller's requested {@link Rotation}
	 */
	public static PixelSource resolve(PixelSource source, boolean mandatoryRotate90, int printheadPixels, Rotation rotation) {
		switch (rotation) {
		case NONE:
			return source;
		case CW_90:
			return ImageRotation.rotate90Clockwise(source);
		case CW_180:
			return rotateNTimes(source, 2);
		case CW_270:
			return rotateNTimes(source, 3);
		case AUTO:
		default:
			return resolveAuto(source, mandatoryRotate90, printheadPixels);
		}
	}

	private static PixelSource resolveAuto(PixelSource source, boolean mandatoryRotate90, int printheadPixels) {
		if (effectivePrintheadAxis(source, mandatoryRotate90) <= printheadPixels) {
			return source; // candidate A: no extra pre-rotation needed
		}

		PixelSource candidateB = ImageRotation.rotate90Clockwise(source);
		if (effectivePrintheadAxis(candidateB, mandatoryRotate90) <= printheadPixels) {
			return candidateB;
		}

		return source; // neither orientation fits - give up rotating, let canvas-resize/crop handle it
	}

	/** What the printhead-axis dimension of {@code source} will be once the mandatory transform is applied on top. */
	private static int effectivePrintheadAxis(PixelSource source, boolean mandatoryRotate90) {
		return mandatoryRotate90 ? source.getHeight() : source.getWidth();
	}

	private static PixelSource rotateNTimes(PixelSource source, int n) {
		PixelSource result = source;
		for (int i = 0; i < n; i++) {
			result = ImageRotation.rotate90Clockwise(result);
		}
		return result;
	}
}
