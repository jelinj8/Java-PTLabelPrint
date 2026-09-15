/**
 * The cross-family image abstraction and staging pipeline: {@link cz.bliksoft.ptlabelprint.image.PixelSource}
 * (the shared "read pixels" interface both {@code protocol.niimbot}/{@code protocol.phomemo}
 * converters build from), {@link cz.bliksoft.ptlabelprint.image.BufferedImagePixelSource} (basic,
 * fixed-threshold B/W conversion from a standard {@code java.awt.image.BufferedImage} - ordered/
 * error-diffusion dithering is a documented future improvement, not implemented here),
 * {@link cz.bliksoft.ptlabelprint.image.ImageRotation} and {@link cz.bliksoft.ptlabelprint.image.CanvasResize}
 * (pre-encoding transforms used by {@code printer.LabelPrinter#print}'s rotation/fit algorithm),
 * and {@link cz.bliksoft.ptlabelprint.image.PrinterCapabilities} (the common DPI/printhead-width/
 * density-range value object {@code LabelPrinter#getCapabilities()} returns). A bridge to
 * BSToolbox's {@code IconSpecEngine} for the CLI's iconspec-to-print mode is still not implemented.
 */
package cz.bliksoft.ptlabelprint.image;
