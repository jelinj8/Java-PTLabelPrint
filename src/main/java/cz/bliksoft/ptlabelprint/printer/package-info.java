/**
 * The printer abstraction layer: declarative per-model definitions (label width/DPI, rotation
 * quirks, which {@code cz.bliksoft.ptlabelprint.protocol.*} family it speaks) plus auto-detection
 * from the BLE advertised name, so callers work against one typed API regardless of manufacturer -
 * mirrors <a href="https://github.com/transcriptionstream/phomymo">phomymo</a>'s
 * {@code printers.json} + auto-detect approach (and niimbluelib's own per-device configmaps) but
 * generalized to dispatch across protocol families rather than just per-model parameters within one
 * family. Not yet implemented.
 */
package cz.bliksoft.ptlabelprint.printer;
