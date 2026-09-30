# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: both protocol families confirmed printing on real hardware

`cz.bliksoft.ptlabelprint.protocol.niimbot` has a full port of niimbluelib's printer/print-task
catalog and a real (partial) port of its protocol: packet framing/checksum (`NiimbotPacket`), the
full `RequestCommandId`/`ResponseCommandId` catalog, `NiimbotDevice` (connect handshake, printer
info, heartbeat, RFID info, printer reset, plus the page/bitmap-row/print-start-end primitives),
`NiimbotImageEncoder` (ported from niimbluelib's `ImageEncoder` - row encoding, run-length row
collapsing, indexed/full bitmap row selection), `PrinterModel`/`PrinterModels` (all **77** models
niimbluelib itself ships metadata for - id(s), dpi, print direction, printhead pixels, paper types,
density range - a full verbatim port, not just the D-series/M2_H subset from earlier), and all
**7** of niimbluelib's print tasks, each `extends AbstractNiimbotPrintTask` (the shared plumbing -
`validatePage`, and concrete-but-overridable defaults for `waitForFinished`/`printEnd`/
`isSupportColor` - ported from niimbluelib's own `AbstractPrintTask`): `D110V4PrintTask` (D11_H,
also D110_M protocol v4/B21_PRO/B1_PRO/C1/EP1C), `B1PrintTask` (M2_H, also B1/D110_M below protocol
v4/B21_C2B/N1/D101 - **not** `D110V4PrintTask` despite the similar "D110M" naming),
`OldD11PrintTask` (D11/D11S), `D110PrintTask` (B21S/B21S_C2B/D110, also D11 at protocol v1/v2),
`B21V1PrintTask` (B21), `B21L2BPrintTask` (B21_L2B), and `H1SPrintTask` (H1S). `NiimbotPrintTasks`
is the model→task dispatch table (mirrors niimbluelib's own `modelPrintTasks`/`findPrintTask` -
an exact `{model, protocolVersion}` match beats a bare-model match); most of the 77 models aren't
in it at all, matching niimbluelib itself, which has no dedicated print task for them either - this
project doesn't invent one. `Cli`'s `niimbot-print-test` dispatches through
`NiimbotPrintTasks.findPrintTask` rather than a hardcoded per-model branch. All of this is built on
the shared `BleTransport`.

Beyond the print flow, `NiimbotDevice` now covers every method niimbluelib's own `NiimbotProtocol`
class wraps as a convenience API (not every raw command ID - see `PacketGenerator`'s own javadoc for
that distinction): `isSoundEnabled`/`setSoundEnabled` (`ptlabelprint-cli media` prints both),
`labelPositioningCalibration` (`ptlabelprint-cli niimbot-calibrate` - **uses real consumables**,
niimbluelib's own note: ejects ~15cm of paper), `setPrinterTime` (`ptlabelprint-cli
niimbot-set-time`), and `firmwareUpgrade` (`ptlabelprint-cli niimbot-firmware-upgrade`, requires
`--confirm-firmware-risk`). `D110V4PrintTask`/`PrintOptions` also gained the tube-type/width and
half-cut fields niimbluelib's own `D110MV4PrintTask.ts` conditionally sends - a `PrintOptions`-only
capability with no CLI exposure, since neither D11_H nor M2_H supports shrink-tube labels to test it
against.

**Firmware upgrade is a deliberate exception to "confirmed on real hardware" everywhere else in
this file**: it needed a genuinely different packet frame (`NiimbotCrc32Packet` - 2-byte chunk
number, 4-byte CRC32 checksum, standalone class, not a subclass of `NiimbotPacket`) and a dual-format
raw-data listener (`NiimbotDevice#onFirmwareRawData`, swapped in only for the duration of the call)
since the exchange mixes normal- and CRC32-framed responses. **It has never been run against real
hardware and there is no validated firmware file in this project to test with** - correctness rests
on matching niimbluelib's source, not on any real upload having succeeded; the one part that
*is* verified is `NiimbotCrc32Packet`'s frame encode/decode/checksum round-trip
(`NiimbotCrc32PacketTest`). A prior version of this file documented firmware upgrade as deliberately
out of scope - the user explicitly asked to include it anyway, accepting the brick risk; the CLI
command's required `--confirm-firmware-risk` flag is this project's only guardrail against
triggering it by accident.

**Confirmed against a real Niimbot D11_H**:
- `ptlabelprint-cli info <address>`: connect handshake (protocol v5), model ID (528, correctly
  resolved to `D11_H` via `PrinterModels`), serial number (matched the device's own advertised
  name), battery, label type, firmware versions, and DPI class (300, matching the table) all came
  back correct. No OS-level BLE pairing was needed. One cosmetic oddity: the parsed Bluetooth MAC
  address didn't match the OS-visible BLE address byte-for-byte (different byte order/
  representation) - not investigated further, doesn't block anything.
- `ptlabelprint-cli niimbot-print-test <address>`: **printed successfully** across three runs (a
  144x120px solid rectangle, full printhead width) - init/page/status-poll/end all completed
  without protocol errors. One run had a single missing line; a same-parameters retry showed it in
  a different position with an irregular (non-digital-looking) appearance - both runs' evidence
  points to a mechanical/printhead artifact (dust, debris, contact inconsistency), not a row-
  encoding bug, which would drop the *same* row every time with a clean edge. Unlike Phomemo's
  `d-series`, the Niimbot protocol did **not** require the image width to exactly match the
  printhead's physical capacity - the printer accepted the declared `cols` directly.
- `ptlabelprint-cli print-test <address> --image=<file>`: **printed successfully** with an
  asymmetric "L"-marker test pattern (not a solid rectangle - see "Unified image-print abstraction"
  below for why that distinction matters here), confirming `LabelPrinter#print(BufferedImage,
  PrintJob)`'s new `PrintDirection`-aware mandatory 90°CW rotation is correct in direction, not just
  dimensionally plausible - user-confirmed against the physical label.

**Confirmed against a real Niimbot M2 (M2_H)**: found via an unfiltered `discover` scan
(`M2_H-I814050044` → correctly detected as `M2_H(NIIMBOT)`, no OS-level pairing needed).
- `ptlabelprint-cli info <address>`: connect handshake (protocol v4, one below the D11_H's v5),
  model ID (4608, correctly resolved to `M2_H`), serial number (matched the advertised name),
  battery, label type (`WITH_GAPS`), printhead width (576, vs. the D11_H's narrower head), and DPI
  class (300) all came back correct.
- `ptlabelprint-cli media <address>` (new command, added for this test - queries
  `NiimbotDevice.heartbeat()` for live state and `rfidInfo()`/`rfidInfo2()` for the loaded
  consumables' NFC tag data, neither of which any prior CLI command exercised): heartbeat correctly
  reported `lidClosed`/`paperInserted`/`paperRfidSuccess`/`ribbonInserted`/`ribbonRfidSuccess` all
  `true`, plus battery and head temperature. `rfidInfo()` (paper) and `rfidInfo2()` (ribbon) each
  returned real, distinct tag data (barcode, serial, allPaper/usedPaper, consumables type) -
  correctly so: **unlike the D11_H, the M2 is thermal-transfer, not direct-thermal**, so it has a
  genuinely separate physical ribbon cartridge alongside the paper roll, each with its own NFC tag
  and its own independent paper-count (a ribbon is rated for multiple paper-roll refills - the user
  reports "usually 3" - so ribbon `allPaper`/`usedPaper` and paper `allPaper`/`usedPaper` are
  expected to run at different counts, not mirror each other; they can also drift out of sync with
  each other across a paper-format change mid-ribbon-life, per the user, so don't assume a fixed
  ratio between the two). The earlier draft of this note mistakenly treated the ribbon tag as a
  duplicate/oddity - corrected here.
- `ptlabelprint-cli niimbot-print-test <address>`: **printed successfully** (a 576x120px solid
  rectangle, full printhead width) - init/page/status-poll/end all completed without protocol
  errors, confirmed against the physical label. This required porting a *second* print task,
  `B1PrintTask` (niimbluelib's `B1PrintTask`, not `D110MV4PrintTask` - checked against niimbluelib's
  own `modelPrintTasks` dispatch table on GitHub rather than assumed, since the M2_H's model ID
  wasn't in `D110MV4PrintTask`'s own model list despite the "D110M"/M2 naming looking superficially
  related) plus its two packet variants, `PacketGenerator.printStart7b`/`setPageSize6b` - see
  `B1PrintTask`'s own javadoc for exactly how it differs from `D110V4PrintTask` (an explicit
  `PageStart`, no B21_PRO one-way-status/heartbeat quirks). `Cli`'s `niimbot-print-test` now
  dispatches between `D110V4PrintTask`/`B1PrintTask` by the connected printer's resolved
  `PrinterModel`.

`PrinterCatalog`'s `M2_H` entry is now marked `isConfirmedOnHardware() == true` to match.

`Cli` wires the family up with `scan`/`info`/`media`/`niimbot-calibrate`/`niimbot-set-time`/
`niimbot-firmware-upgrade`/`gatt`/`raw`/`niimbot-print-test` subcommands. **Not implemented yet**:
Serial transport. A real (if basic - fixed-threshold, no dithering) image pipeline now exists via
`.printer`'s `LabelPrinter#print(BufferedImage, PrintJob)` (see below); `niimbot-print-test` itself
still hand-builds a `PixelSource` directly and hasn't been refactored to delegate to it (deliberately
- see below). See each print-task class's javadoc for exactly which niimbluelib methods it ports vs.
omits - only `D110V4PrintTask` (D11_H) and `B1PrintTask` (M2_H) are hardware-confirmed; the other 5
are ported from niimbluelib's source but untested against real hardware.

`cz.bliksoft.ptlabelprint.protocol.phomemo` (`RasterImage`, `DSeriesCommands`, `DSeriesPrinter`)
ports phomymo's `d-series` protocol and **has successfully printed on a real Phomemo Q30** via
`ptlabelprint-cli phomemo-print-test <address>` - a 12mm x 12mm solid square, correctly sized,
positioned, and shaped. Getting there took three real-hardware iterations; see "Debugging history"
below for what each one actually was (useful precedent for the next device/model).

Re-reading `DSeriesCommands`/`DSeriesPrinter` confirms the print flow itself needs no per-model
code at all - every dimension (image size, density, continuous vs. gap mode) is a caller parameter,
nothing is hardcoded to the Q30 specifically, and the BLE channel is confirmed shared across the
whole family. So **D30/D35/D50/Q30S are expected to work via the exact same code path already
confirmed on the Q30** - though none of them is actually hardware-confirmed, only extrapolated from
the shared protocol (no such hardware is available to this project). `DSeriesLabelSizes` now ports
phomymo's own `D_SERIES_LABEL_SIZES` mm presets (8 entries: 40x12/30x12/22x12/12x12/30x14/22x14/
40x15/30x15) so `phomemo-print-test --label <key>` isn't limited to the one hardcoded 12x12mm size
- the default stays `12x12` (byte-for-byte the already-confirmed pattern). `D110` is **not** a real
phomymo d-series model (checked against its actual current `printers.json` - see the correction
below); an earlier version of this file wrongly listed it, most likely confused with Niimbot's own
real `D110`/`D110_M` - not re-added to `PrinterCatalog`, since that would tie against the real
Niimbot `D110` entry for every device actually named `D110...`, with no phomymo source to justify
it. The other 6
protocol tags (`m02`/`m04`/`m110`/generic `m-series`/`p12`/`tspl`) are now **cataloged, not yet
implemented**: `PhomemoPrinterModel`/`PhomemoPrinterModelMeta`/`PhomemoPrinterModels` hold all 17 of
phomymo's non-d-series `printers.json` rows verbatim (protocol tag, width, dpi, alignment,
rotation, tape), and `PrinterFamily`/`PrinterCatalog` know about them (see below) - but no
command-builder/print-flow code exists for any of the 6 yet, so a detected device from one of them
can't actually print (`PrinterFactory` throws `UnimplementedPrinterFamilyException`, not silently
misdispatching or defaulting). `d-series` now has a real (fixed-threshold, no dithering) image
pipeline from an arbitrary `BufferedImage` via `.printer`'s `LabelPrinter#print` (see below) -
`RasterImage.fromPixelSource` packs a `PixelSource` into this class's existing MSB-first format;
`phomemo-print-test` itself still hand-builds a trivial raster directly, not refactored to delegate
(deliberately - see below).

`cz.bliksoft.ptlabelprint.printer` - the printer abstraction layer - is now implemented, once there
were two real protocol families to actually generalize across: `PrinterCatalog` (BLE-advertised-
name → `PrinterDefinition`, longest-prefix-wins, mirroring phomymo's own `detectPrinterConfig`) and
`LabelPrinter` (connection lifecycle, plus a real, deliberately minimal common print entry point -
`print(BufferedImage, PrintJob)`/`getCapabilities()`, see "Unified image-print abstraction" below -
see `LabelPrinter`'s own javadoc for exactly what that surface covers vs. what still needs
`getDevice()`/`instanceof`-casting to each family's real API, e.g. Phomemo's `d-series` still has no
info-query capability at all, unlike Niimbot's rich `PrinterInfoType` catalog). Beneath that interface, a real (not
speculative) class hierarchy: `AbstractLabelPrinter` (cross-family - just the `PrinterDefinition`
field/family-validation pattern, since `connect`/`close`/`isConnected` delegate to genuinely
different underlying objects per family and aren't shareable further at that level) and
`PhomemoLabelPrinter` (Phomemo-specific - stores the caller-supplied `Transport` plus concrete
`connect`/`close`/`isConnected` bodies, since every Phomemo sub-protocol, implemented or merely
cataloged, shares the same transport shape). `NiimbotLabelPrinter` extends
`AbstractLabelPrinter` directly (exposes each family's real, different print API after connecting -
`getDevice()`/`getPrinterInfo()`); `PhomemoDSeriesLabelPrinter` extends `PhomemoLabelPrinter`
(exposes `print(...)`) - the only concrete `PhomemoLabelPrinter` subclass today, but the class is
real, shipping duplication-removal between two real files, not scaffolding for the 6 unimplemented
Phomemo families (those get no `LabelPrinter` subclass at all - see below). **Confirmed against
real hardware**: `ptlabelprint-cli discover` (an unfiltered scan cross-referenced against
`PrinterCatalog` - the fix for `scan`'s Niimbot-service-UUID filter finding neither real device
family, noted below) correctly identified a real D11_H's advertised name
(`D11_H-G412010570` → `D11_H(NIIMBOT)`) and a real M2_H's (`M2_H-I814050044` → `M2_H(NIIMBOT)`) live
over the air; `ptlabelprint-cli connect <address>` then auto-detected, dispatched via
`PrinterFactory`, and connected through `NiimbotLabelPrinter`, producing the same `PrinterInfo` as
the family-specific `info` command. The catalog now has an entry for every model
`PrinterModels`/`PhomemoPrinterModels` covers with a real BLE name pattern to match on (all 77
Niimbot models; 14 of Phomemo's 17 cataloged non-d-series rows - 3 are manual-select-only, no name
to match, see `PhomemoPrinterModels`' javadoc; plus `D30`/`D35`/`D50`/`Q30`/`Q30S` for
`PHOMEMO_D_SERIES`, deliberately excluding phomymo's own bare `"D"` wildcard pattern since it would
collide with every Niimbot D-series name) - see `PrinterDefinition#isConfirmedOnHardware()` for
which of those are actually hardware-tested (only D11_H, M2_H, and Q30 so far) vs. just
ported-and-untested, and `PrinterFamily`'s own javadoc for the 6 Phomemo sub-protocol families that
are cataloged but have no `LabelPrinter` implementation at all (`PrinterFactory` throws
`UnimplementedPrinterFamilyException` for those, distinct from "not recognized").

**Unified image-print abstraction** (`LabelPrinter#print(BufferedImage, PrintJob)`): takes a
standard `java.awt.image.BufferedImage` - no manufacturer-specific type (`PixelSource`/
`RasterImage`/`EncodedImage`) appears on this common surface; each family converts internally.
`.image` now has real content (previously just a package-info): `PixelSource` (moved here from
`.protocol.niimbot`, the shared width/height/`isBlack`/`isRed` pixel abstraction),
`BufferedImagePixelSource` (wraps a `BufferedImage`, fixed-threshold 128/255 luminance B/W
conversion - dithering is a documented future improvement, not attempted), `CanvasResize`
(center-anchored pad/crop to an exact target height - **not** a scale, since Phomemo `d-series`
needs an exact printhead-width match or it garbles the print, confirmed the hard way earlier this
session), `ImageRotation` (lazy 90°CW rotation, composable to any multiple of 90°), and
`PrinterCapabilities` (dpi/printheadPixels/densityMin/densityMax, returned by
`LabelPrinter#getCapabilities()`). `.printer` gained `PrintJob` (copies/continuousMedia/density/
`Rotation`, builder-style) and `RotationResolver`, which implements the exact rotation algorithm the
user specified: each family/model's own **mandatory** orientation is always applied first (Phomemo
`d-series`: unconditional 90°CW, unchanged, still inside `DSeriesPrinter.print`; Niimbot: 90°CW iff
the connected model's `PrinterModelMeta.getPrintDirection() == PrintDirection.LEFT`, newly
implemented here - see below for why this is a real behavior change for D11_H specifically) - then
`Rotation.NONE` (`PrintJob`'s **default**) uses that as final regardless of fit, an explicit
`CW_90`/`CW_180`/`CW_270` pre-rotates the *original* source by that much before the mandatory step
(an authoritative override, skips auto-fit), and `Rotation.AUTO` (must be requested explicitly, not
the default - see below) tries the mandatory-only result first and, only if it doesn't fit the
printhead axis, tries exactly one additional 90°CW pre-rotation before falling back to cropping
(`RotationResolverTest` verifies this math directly, including the not-yet-applied-mandatory-
rotation-aware fit check).

**`Rotation.NONE`, not `Rotation.AUTO`, is `PrintJob`'s default - changed after real-hardware use
in `StorageManagerServer` found the original default unsafe for gapped/die-cut media.** A Niimbot M2
(no mandatory rotation of its own - `PrintDirection.TOP`) printing a 50mm-wide label onto a
printhead whose real capacity is ~48.8mm (576px/300dpi) is a trivial, croppable overage - but with
`AUTO` as the default, `RotationResolver` "fixed" it by rotating 90°, which doesn't remove the
overage, it just moves it onto the *other* axis (the feed/length direction) - one that's equally
fixed by the die-cut label's own length on gapped media, so the fix just relocated the problem and
mis-oriented the print besides. Rotating to satisfy a printhead-width mismatch is only actually safe
for continuous media, where the feed axis is unconstrained - so `AUTO` now has to be requested
explicitly, per print, rather than assumed safe as a blanket default across both media kinds. See
`Rotation.NONE`'s own javadoc for the full reasoning. Phomemo also gained `DSeriesPrinterModel`/
`DSeriesPrinterModelMeta`/`DSeriesPrinterModels` (a per-model dpi/printheadPixels/density-range
table for `d-series` - all 5 entries identical, 203dpi/96px/1-8, since only the Q30's values are
actually confirmed and phomymo's own `printers.json` doesn't differentiate the family further - this
uniformity is a documented assumption, not a verified fact for D30/D35/D50/Q30S) and
`RasterImage.fromPixelSource` (packs a `PixelSource` into the class's existing MSB-first raster
format). `NiimbotLabelPrinter`/`PhomemoLabelPrinter`/`PhomemoDSeriesLabelPrinter`/`PrinterFactory`
now all construct from a `Transport`, not a `BlePeripheral` - what makes "regardless of connection
method" real today (a caller builds the `Transport` from whichever `BlePeripheral` they have, local-
or, once BSToolbox-BLE ships its currently-uncommitted local/remote `BleAdapter` split, remote-
backed - `BlePeripheral`'s own public shape is unchanged by that work, confirmed by reading the
sibling repo's in-progress diff, so `BleTransport` needs no change at all) and what a future
`SerialTransport` plugs into unchanged. New CLI command `print-test` (manufacturer-agnostic: detect
via `PrinterCatalog`, connect via `PrinterFactory`, print `getCapabilities()`, then one
`print(BufferedImage, PrintJob)` call against either an `--image` file or a synthetic default test
square) exercises the whole thing end to end; `niimbot-print-test`/`phomemo-print-test` are
deliberately left as-is (family-specific options this minimal common surface doesn't expose), not
refactored to delegate to the new path.

**D11_H's `PrintDirection`-aware rotation is now confirmed against real hardware**: implementing it
was a genuine behavior change to an already-hardware-confirmed path - niimbluelib rotates 90°CW
whenever `PrintDirection == LEFT` (D11_H's value; M2_H's is `TOP`, needing no rotation, unchanged),
which this project's own `NiimbotImageEncoder` never implemented before this change. The earlier
"confirmed working" D11_H print used a solid rectangle, which couldn't have revealed a
90°-orientation bug - so this was a real latent gap, not a regression risk introduced here.
Re-verified via `ptlabelprint-cli print-test <address> --image=<file>` with an asymmetric "L"-marker
pattern (a landscape rectangle with a bar on the left edge, a bar on the bottom edge, and a dot near
the top-right corner) at the default `--rotation=auto`, which for this image resolves to the
mandatory-orientation-only candidate (no additional auto-fit rotation needed): printed correctly,
confirmed by the user against the physical label - the rotation direction is genuinely correct, not
just dimensionally plausible.

**Neither family's real device advertises the service UUID `BleTransport.scanFilter()`/the CLI's
`scan` command filters on** - confirmed for both the Phomemo Q30 and a genuine Niimbot D11_H (found
instead via an unfiltered scan, by name: `D11_H-<serial>`). niimbluelib's own scan logic treats
that UUID as only one of several OR'd criteria, alongside model-name-prefix matching, which is
evidently the one that actually matters in practice on real hardware. **Fixed** by `discover`
(unfiltered scan + `PrinterCatalog` name matching, see above) - `scan`'s own UUID-only filter is
left as-is (still not very useful for either real family) since `discover` is the better tool now,
not because the underlying gap was left open.

**Die-cut/gapped labels on a real Q30 are silently ignored past ~80mm - continuous mode isn't
affected.** Confirmed via `phomemo-print-test --label 12x12 --length 150` (the `--length`/
`--feed-dots` options exist specifically for this): a 150mm die-cut job transferred without any
software-level error (`DSeriesPrinter.print`/`writeStream` completed normally) but printed
nothing at all, reproducing what the user had already hit outside this CLI; the same 150mm image
with `--continuous --feed-dots 64` (still a single job, no splitting) printed correctly as one
continuous black bar and fed as expected. Confirmed identical over both a local BLE connection and
this library's ESP32 remote bridge, which rules out a transport-reliability explanation (dropped
BLE writes were the first hypothesis here - wrong). Checked against phomymo's current GitHub
source (`constants.js`/`printer.js`/`printers.json`/`app.js`): there's no length-related constant
or job-splitting logic anywhere in it, and `handlePrint` only ever sends one raster job per Print
click - so this isn't a software limit either project is quietly working around. Most likely
explanation: a real firmware/gap-sensor search-window limit specific to die-cut mode, bypassed
entirely once gap detection is disabled (continuous mode's `1F 11 0B`). **Practical takeaway: use
continuous media for labels longer than ~80mm, not die-cut** - no code fix needed or attempted for
the die-cut case itself, since it looks like a real hardware constraint, not a bug. (An earlier
version of this session explored a `DSeriesPrinter.printContinuousSegmented` - splitting a long
continuous print into several shorter jobs - built on the untested assumption that continuous mode
might have its own, lower ceiling; once real-hardware testing showed a single continuous job
already works past 80mm, that method was removed as unneeded rather than kept speculatively.)

`cz.bliksoft.ptlabelprint.protocol.Transport` is shared by both families - a plain
`connect`/`disconnect`/`write(byte[])`/`setRawDataListener` contract, no per-write response-mode
knob (tried adding one during debugging, reverted once it proved unnecessary - see "Debugging
history").

A git repo was initialized locally (`git init`) but nothing has been committed yet.

### Phomemo M421: `m110` protocol implemented and confirmed printing (2026-09-30)

**This supersedes every "`m110` is cataloged, not implemented" statement elsewhere in this file** -
those were true until this section; `m02`/`m04`/generic `m-series`/`p12`/`tspl` are still data only
(5 unimplemented Phomemo families now, not 6).

A real Phomemo M421 (4"-class, 203 DPI, advertises as `M421`) **printed a 40x20mm gap label
correctly** - right way up, not shifted, not cropped - via `ptlabelprint-cli phomemo-m110-print-test`.
phomymo doesn't know the M421 at all, so the protocol was established from evidence, not naming:

- It answers Phomemo M-series status queries over the usual `ff00`/`ff02`/`ff03` channel:
  `1F 11 07` → `1a 07 00 02 06` (firmware), `1F 11 08` → `1a 04 2a`, `1F 11 11` → `1a 06 89`
  (paper), `1F 11 12` → `1a 05 98` (cover). On connect it sends `01 07`, `02 b6 00`,
  `1a 3b 04 00 04 00 00` unprompted (meaning unknown); `1a 0f 0c` arrives when a job finishes.
- phomemo-tools (GPL-3.0 - **facts only, no code ported**) has a `cups/drv/phomemo-m421.drv` that
  routes the M421 through its M110 filter: rows at the *label's* width, not rotated, media type
  `0a` gaps / `0b` continuous / `26` marks.
- That dialect is phomymo's MIT `m110` protocol (`printM110`), ported as `M110Commands`/
  `M110Printer`: `1B 4E 0D <speed>`, `1B 4E 04 <density>`, `1F 11 <media>`, `GS v 0` raster header,
  data in 128-byte chunks, footer `1F F0 05 00 1F F0 03 00`.

Wired into `.printer`: `PrinterCatalog` has `phomemo-m421` (confirmed) next to the existing
`phomemo-m110` (M110/M120 - same code path, **untested**), `PrinterFactory` returns a
`PhomemoM110LabelPrinter`, `M110PrinterModels` holds the per-model capabilities. Unlike `d-series`,
rows are sent at the image's own width - the caller sizes the image to the loaded label (8px/mm);
only an image wider than the printhead is cropped. The unified path is hardware-confirmed too:
`print-test <address> --pattern-mm 40x20` printed the same label, identically, through
`LabelPrinter#print(BufferedImage, PrintJob)`. The M421's printhead is taken as 912px: that is the
row width (114 bytes) Phomemo's own app sends it - see "M421: what the official app does".

Things that differ from every other printer tested so far:

- **It needs an OS-level bonded link.** Read `ff01` and write `ff02` work unbonded, but subscribing
  to `ff03` fails with Windows `0x80650005` ("attribute requires authentication"), so `BleTransport`
  can't connect at all until the device is paired. BSToolbox-BLE's programmatic
  `BlePeripheral.pair()` returned `Failed` (status 19) for every PIN tried (empty, `000000`,
  `123456`, `0000`); it only worked once the user accepted a Windows pairing prompt by hand.
  `raw --pair[=PIN]` / `--no-subscribe` were added for exactly this kind of diagnosis.
- **It has credit-based flow control, and large labels need it** - see "M421 large labels" below.
  `01 nn` grants write credits (`01 07` on connect, `01 01` per write taken), `02 b6 00` is the
  largest write it takes (182 bytes). The Q30 note below calling `01 01` unrelated to writes was
  observed on the Q30 only; don't carry it over to the M421.
- **Its name isn't in the first advertisement** (same as the Q30, per the user), so an
  address-filtered scan returned a null name and `connect`/`print-test` couldn't detect the family;
  both now scan with `new ScanFilter().withAddress(address).requireName()` - BSToolbox-BLE's own
  answer to this, don't hand-roll a second scan for it.
- `print-test` refuses to run on this family without `--image` or `--pattern-mm`: its default
  pattern is a printhead-sized square, which on a ~100mm head would print far past a 40mm label.
  `--pattern-mm=WIDTHxHEIGHT` (any family) generates the asymmetric alignment pattern at the
  connected printer's own DPI instead.

**Label positioning on small labels is the printer's own behaviour, not a bug here.** With 40x20mm
labels the content sits slightly low, the label's bottom edge stays under the tear edge (it can't be
torn off cleanly), and the feed button advances *two* labels per press. The user confirmed all of
this persists after the printer's own calibration **and when printing through the official app** -
so there's nothing for this project's print flow to fix. The printer does apply *some* tear offset
(it feeds back before the next print, per the user) - just slightly too small for this stock - so it
is presumably a stored printer setting, but no command for it appears in either reference project
(searched phomymo's `printer.js`/`constants.js` and phomemo-tools' README; don't invent one). **The
official app has no such setting either** (user-checked), so there is nothing to capture: Phomemo's
own labels simply have a bigger gap, which is what makes the fixed offset work for them. Closed as
a property of the media, not of any software. The user considers it acceptable provided large labels
(e.g. 100x150mm, the size this shipping-label printer is built for) don't show it - **not yet
tested**, no such media has been through it.

Everything above was tested over this PC's own Bluetooth adapter, **not** the ESP32 remote bridge.

### M421 large labels: data starvation, fixed by sending on the printer's credits (2026-09-30)

A 100x150mm label (800x1200px, 120 KB) with phomymo's pacing **started, slowed down, and stopped
after ~20mm**, twice, over the local adapter. What the evidence showed, in order:

- *Wrong first guess - receive-buffer overflow* (20mm x 100 bytes/row is suspiciously close to
  16 KB). Gating every write on a returned credit, stop-and-wait, changed nothing, and the log
  refuted it: all 943 `01 01` credits for the job came back on time, none withheld.
- That same log showed the printer notifying **`1a 0b b8` 6s into the job - "print failed"**
  (phomymo's `ble.js` status parser: type `0b`, value `b8` = -1; `1a 0f 0c` = done).
- The user's description ("slowed down and stopped") plus the arithmetic gave the real cause: the
  M421 **prints while it receives**, and 128 bytes per ~20-30ms is ~4-5 KB/s - for 100-byte rows
  that's ~5mm of label per second. It slows to match, runs dry, and fails the job. A 40mm-wide label
  needs 2.5x less data per mm, which is why small labels were fine.

Fix (`M110Printer`): the credits are a *throughput* mechanism. The printer announces 7 credits and a
182-byte maximum write right after the notify subscribe - so they're captured by a listener
installed **before** `connect()` (`M110Printer.listenForLinkInfo`, done in
`PhomemoM110LabelPrinter.connect()`) - and the data then goes out in 182-byte writes with no fixed
delay, each write spending a credit, up to 7 in flight. A failed-job notification aborts the send
with an `IOException`. A printer that announces no credits (nothing is known about the real
M110/M120) keeps phomymo's pacing untouched. Result on the same label: 665 writes in ~18s
(~6.7 KB/s, ~27ms per write over the Windows adapter), the printer reported the job finished, and
**the label printed complete (user-confirmed) - but still slowing progressively along its length.**

**That remaining slow-down is not this code's send rate - the printer itself takes raw raster at
only ~7 KB/s over BLE.** Measured without printing, by credit-gating harmless `1F 11 11` status
queries of different sizes: 3-byte writes are credited back in 7ms, 60-byte in 13ms, 120-byte in
18ms, 180-byte in ~25ms - the credit return time scales with the write size and throughput flattens
at 6.6-7.4 KB/s, while this PC can issue a write every ~7ms on a 15ms connection interval
(requesting `THROUGHPUT_OPTIMIZED` changed nothing). So the 7-credit window fills and the printer
sets the pace. At 100 bytes per row that's ~70 rows/s, under 9mm of label per second: a 100x150mm
label can't take less than ~17s this way, and the printer slows as its initial buffer drains. The
fixed pacing failed outright only because ~4-5 KB/s fell below whatever the printer tolerates.

Don't try to "fix" this by sending faster with the same commands - there is no faster over BLE. The
capture of the official app (next section) showed how Phomemo gets around it: it doesn't use BLE.

Also open: this path is one `transport.write` per chunk, i.e. one round trip each through a remote
bridge, where `DSeriesPrinter`'s single-`writeStream` trick can't be used (the firmware can't see
credits). If a round trip costs more than the ~26ms the printer needs per 182-byte write anyway, the
bridge falls below the ceiling and may starve the printer again - **large labels through the ESP32
bridge are untested.** `phomemo-m110-print-test -v` logs every notification with a timestamp plus the
captured link info; that's the tool for it.
An unprompted `1a 05 99` also appeared 15s into the failed run - the cover being opened (`98` is
closed on this printer, see the capture notes below).

### M421: what the official app does (HCI snoop capture, 2026-09-30)

A capture of Phomemo's Android app printing a full 100x150mm label on the M421 (Samsung S24 Ultra;
see "Getting an HCI snoop log off a Samsung" below). **Facts from that capture:**

- **The app uses classic Bluetooth, not BLE.** It opens a BR/EDR connection to the same address
  (`07:9F:2C:8E:83:45` - the printer is dual-mode), looks up the Serial Port service (SDP, UUID
  `0x1101`) and talks over RFCOMM channel 1. No GATT traffic to the printer at all.
- **Throughput is the whole difference.** 136,889 bytes went out in 662-byte RFCOMM frames at
  ~17-19 KB/s once the raster started (~7s for the data), against the ~7 KB/s BLE ceiling measured
  above. The raster is **raw, uncompressed** - the app explicitly selects compression mode 0.
- **Its command set is the `m04`-style one, not phomymo's `m110`:**
  ```
  1f 11 0a                 media type: gaps
  1f 11 02 06              density (m04's DENSITY command, value 6)
  1f 11 24 00              unknown, sent twice
  1f 11 35 00              compression: 0 = raw
  1b 40                    ESC @
  1d 76 30 00 72 00 b0 04  GS v 0, 114 bytes wide, 1200 rows
  <136800 raster bytes>    and nothing after it - no footer, no feed command
  ```
  Before that it polls status: `1f 11 38`, `12`, `13`, `07`, `09`, `11`, `19`. The printer reported
  `1a 0f 0c` (finished) ~3s after the last raster byte.
- **Rows are 114 bytes = 912px**, for a 100mm (800px) label - the full printhead width, with the ink
  in columns 70-731. `M110PrinterModels` now uses 912 for the M421 (it was 812, a guess from
  phomemo-tools' widest media). This project's label-width rows print correctly positioned too, so
  no padding to 912 was added.
- Replies seen: `1a 17 03`, `1a 05 98`, `1a 03 a8`, `1a 07 00 02 06`, `1a 08 <15 ASCII chars>`
  (serial), `1a 06 89`, `1a 0c 0a`. **`1a 05 98` is "cover closed" on this printer** (it was
  printing) - the reverse of phomymo's `ble.js` mapping - so the unprompted `1a 05 99` seen in a
  failed run here was the cover being opened.

**What this means for the project:** a fast M421 needs a classic-Bluetooth serial transport, not a
cleverer BLE sender - now built, see the next section. The ESP32-C6 bridge can't help: that chip is
BLE-only. `M110Printer` still sends phomymo's `m110` commands (hardware-confirmed over both BLE and
serial); switching to the app's sequence is possible but hasn't been needed or tried.

### Serial transport (`SerialTransport`, classic-Bluetooth SPP) - 2026-09-30

**"Serial transport: not implemented" elsewhere in this file is superseded by this section.**
`protocol.SerialTransport` is a `Transport` over jSerialComm: open a port, a reader thread feeding
the raw-data listener, blocking writes, and a `writeStream` that ignores the BLE chunk/delay pacing
and just writes (the link has real flow control). Nothing printer-specific in it.

Its first real use is the M421 over classic Bluetooth: once the printer is **paired with Windows as
a classic device** (separately from the BLE pairing - the user did it through Windows' Bluetooth
settings), Windows creates an outgoing SPP COM port for it (here `COM31`, described as `JL_SPP`).
Over that port the printer answers status queries in ~6ms and sends no `01`/`02` credit
notifications - those are BLE-side only - so `M110Printer` takes its plain (uncredited) path, which
through this transport means "write the whole raster at once".

- `ptlabelprint-cli ports` lists serial ports; `print-test --serial COM31 --model M421 --pattern-mm
  100x150` prints through one. `--model` is required: a COM port has no advertised name for
  `PrinterCatalog` to detect the family from. `--baud` exists but is meaningless for Bluetooth ports.
- **Result: the 100x150mm pattern printed in 11.2s through the unified `LabelPrinter` path, the
  printer reporting the job finished** - against ~18s and a visibly slowing print over BLE.
  User-confirmed: complete, at even speed, and dimensionally right (the pattern's frame measured
  97.8 x 148mm against a nominal 98 x 148).
- **Horizontal position: the M421 aligns media to the left and starts printing about 1mm in from
  that edge** - which is where a label with the usual 1mm side gap (backing-paper edge to label
  edge) begins, so such stock prints in place with no help (the 40x20 stock: unshifted = in place;
  the same job moved 1mm right landed at 2mm). Stock with a wider gap prints too far left by the
  difference: on the 102mm stock the frame, drawn 1mm into a 100mm-wide image, landed on the
  label's left edge with 3.8mm free on the right, and moving it 1mm right gave 0.5mm left / 1.5mm
  right - so that stock wants ~1.5mm. The setting for it is `PrintJob#setMediaSideGapMm` (CLI
  `--side-gap-mm`): **the extra gap to add, applied as given - unset or 0 means no shift** (pads
  blank columns on the left). **Only `PhomemoM110LabelPrinter` honours it.** History, so nobody
  re-derives it: this went raw `leftMarginMm` pad -> "real side gap minus the printer's own 1mm
  offset" (`M110PrinterModelMeta#getLeftOffsetMm` = 1.0) -> back to a plain additive value with the
  M421's offset set to 0, at the user's decision ("handle the default left gap as 0 for the M421,
  just allowing to add gap if needed") - callers shouldn't have to know the printer's 1mm. The
  per-model offset field remains, 0 for every model. `--side-gap-mm 1.5` on the 102mm stock is
  derived, not yet printed.
- **Vertical position is off by ~1mm the other way on this stock and nothing compensates for it:**
  on that same print the frame had almost no margin at the top and ~2mm at the bottom (nominal 1
  and 1), i.e. the image starts ~1mm early. The user attributes it to the gap sensor's alignment.
  The 40x20 stock showed the opposite (print slightly low), so it's media-dependent too.
  `PrintJob#setTopOffsetMm` (CLI `--top-offset-mm`, signed: positive = later/down, negative =
  earlier/up) now moves the image along the feed direction, **m110 family only** like the left
  margin. By default the job keeps its length - rows pushed past one end are cut, the other end is
  blank; `setTopOffsetKeepsLength(false)` (CLI `--top-offset-changes-length`) lengthens/shortens the
  job by the offset instead. On the 40x20 stock the vertical position turned out centred with no
  offset (baseline, two consecutive labels). Two mechanism tests were then printed there, two
  consecutive labels each, `--side-gap-mm 1 --top-offset-mm 2`: once keeping the length, once with
  `--top-offset-changes-length` (a 22mm job on 20mm labels). User-reported result: **all four
  labels came out the same** - the offset moved the print down in both modes, and the 2mm-over-long
  job did **not** upset the feed (no skipped label, no drift on the following one); what it pushed
  past the label's end simply didn't appear. Keeping the length stays the default anyway - only a
  2mm overrun was tried, about the size of the gap. The one difference seen: the pattern's bottom
  bar, which the offset put right on the label's bottom edge, came out thinner on the first and the
  last of the four - so **label-to-label registration along the feed varies by roughly half a
  millimetre**. Sideways the frame measured ~1.5mm left / 0 right here (nominal 1 / 1) with no shift
  applied, where an earlier run with 1mm added read 2mm: treat the 1mm printer offset and any side
  gap as good to about +-0.5mm, which is also about how well a ruler reads them.
- **USB is deliberately not handled here.** The M421's USB port enumerates as a USB *printer-class*
  device (`USBPRINT`, VID 0483 PID 5740, with a Windows print queue), not a serial port, so
  jSerialComm can't see it; a raw device-file transport was started and dropped at the user's
  request - USB printing goes through the OS print system (the user's own SYSTEM / LBL-SYSTEM
  printing path), not through this library. (Nothing was learned about the USB protocol: the
  probe that seemed to show the device "echoing" status queries never reached it - Java turned the
  `\\?\USB#...` device path into a plain file of that name in the working directory and read its
  own writes back. BSToolbox-print's `link.usb` opens such devices properly, via JNA.)
- The other transports are unaffected; BLE remains what the ESP32 bridge and every other printer use.

**Getting an HCI snoop log off a Samsung (One UI, Android 16):** enable "Bluetooth HCI snoop log" in
developer options, **then toggle Bluetooth off and on** (or nothing is recorded - `adb shell svc
bluetooth disable/enable` works), reproduce, dial `*#9900#` -> "Run dumpstate/logcat" -> "Copy to
sdcard", then `adb pull /sdcard/log/bluetooth` (a `btsnoop_hci_*.cfa`, plain btsnoop format).
`adb bugreport` is no use here: it only carries a truncated, events-only summary.

### Downstream wiring (2026-09-30): BSToolbox-print, BSToolbox-jfx-print, BSLabelDesigner

The serial transport and the media-positioning settings are wired into the sibling projects, all
uncommitted there too, each building against a **locally installed** `ptlabelprint` 0.4.0-SNAPSHOT
(`mvn install` here; nothing was pushed or deployed):

- **BSToolbox-print** (`lbl.raster.PtLabelPrintRasterTarget` / `lbl.ptlabelprint.
  LocalTransportConnection`): advanced property `transport` = `BLE` (default) or `SERIAL`
  (`comPort`, `baudRate`, `printerModel` - resolved through `PrinterCatalog.detectUnambiguous`), and
  print settings `mediaSideGapMm`, `printTopOffsetMm`, `printTopOffsetKeepsLength` mapped onto
  `PrintJob`. Hardware-checked: its own connection class opened the M421 on COM31 and printed a
  40x20 label.
- **BSToolbox-jfx-print**: editable combos for `comPort` (this machine's serial ports with their
  descriptions) and `printerModel` (every catalog name prefix), plus warnings when a PTLABELPRINT
  printer lacks what its transport needs. Compiled and unit-tested only - not yet seen in a running
  app.
- **BSLabelDesigner**: dependency bump and credits text only; the settings are plain advanced
  properties of the printer, so the app's own code didn't change. **Confirmed end to end by the
  user:** a printer configured as PTLABELPRINT / `transport=SERIAL` / `comPort=COM31` /
  `printerModel=M421` printed correctly from the designer (template -> ZPL emulator ->
  BSToolbox-print -> `SerialTransport` -> M421).

Consequence for API changes here: `PrintJob`'s `mediaSideGapMm` / `topOffsetMm` /
`topOffsetKeepsLength`, `PrinterCatalog.all()` + `PrinterDefinition.getNamePrefixes()`, and
`SerialTransport(String, int)` now have an external consumer.

### CLI bridge server mode (`--bridge-port`)

Every subcommand gets its adapter from `Cli.openAdapter()`. By default that's a local `BleAdapter`;
with `--bridge-port <port>` (plus `--bridge-token`, optional `--bridge-name`/`--bridge-wait`, or the
`PTLABELPRINT_BRIDGE_PORT`/`_TOKEN`/`_NAME` env vars) the process hosts BSToolbox-BLE's
`RemoteAdapterServer` for the duration of that one command, waits for a bridge to dial in to
`ws://<this host>:<port>/ble-remote`, and runs the command on the adapter that bridge provides. The
options are picocli `INHERIT`-scoped, so they work before or after the subcommand.

- The bridge is the WebSocket *client*: nothing can happen until it (re)dials, so each invocation
  blocks on that (default 60s) - an ESP32 that was idle reconnects on its own backoff.
- **A token is mandatory.** `RemoteAdapterServer` answers 401 when it has no expected token, and the
  ESP32 firmware (`firmware-BSBleRemoteBridge`) deliberately stops retrying after a 401 until it is
  restarted - so `openAdapter()` refuses `--bridge-port` without a token rather than start a server
  that would lock the bridge out. A *wrong* token has the same effect on the ESP32.
- Plain `ws://` only (`RemoteAdapterServer` has no TLS) - the firmware's `ws_url` must be `ws://`.
- **Verified against the real ESP32 bridge** (channel `jjble`, dialing in to port 8091; also with
  BSToolbox-BLE's own `--remote` client as a stand-in): `discover` found the M421 and `connect`
  detected it and connected, the ESP32 dialing in ~4s after the server started. **`connect` through
  the ESP32 needed no pairing step at all** - the notify subscription that Windows refuses on an
  unbonded link just worked there (why isn't established - the firmware's NimBLE stack presumably
  secures the link itself).
- **Printing through the ESP32 is confirmed on the M421** (complete 40x20 label, user-verified) -
  after one real failure: the first attempt printed only the top ~2.5mm. The whole job had been sent,
  but this mode's server lives only as long as the command, and the process ended ~0.5s after the
  footer - shutting the server down disconnects the bridge, which takes the printer's BLE link with
  it mid-print. That's specific to this per-command server: per the user, Niimbot and Q30 printing
  through the same bridge works against a persistent server (StorageManagerServer), which never
  pulls the link out from under a job. Fix (the user's diagnosis): `M110Printer.print` now waits for
  the printer's `1a 0f 0c` job-finished notification (timeout 5s + 10ms per raster line) before
  returning, taking over the transport's raw-data listener for the call.
- **The other printers don't share that problem through this CLI mode** (2026-09-30, all via the
  ESP32, server shut down right after each command as usual): a Q30 30x12 label
  (`phomemo-print-test --label 30x12`) and a D11_H 22x12 pattern (`print-test --pattern-mm 22x12`)
  both printed complete, user-confirmed - so `DSeriesPrinter` needs no wait-for-finish, despite
  returning as soon as the data is sent. The M2 got a 48x30 pattern on a 50x30 label (48mm rather
  than 50 because `NiimbotLabelPrinter` doesn't crop and the M2's printhead is narrower than that
  label) - which printed only its top, for an unrelated reason: see "Niimbot row-repeat overflow"
  below.
- **One transient failure seen:** the first M2 attempt died in `connect` with "adapter closed" - the
  ESP32 dropped its WebSocket and re-dialed mid-connect, and a re-registration replaces (closes) the
  adapter in use. An immediate retry worked. Cause not investigated; the M2 had the weakest signal of
  the four (rssi -77).

### Niimbot row-repeat overflow (found and fixed 2026-09-30, on the M2)

**A run of more than 255 identical rows used to print only the top of the label.** A 48x30mm
alignment pattern on a real M2_H (354 rows, 270 of them identical in the middle) came out as "just
the top", reproducibly, while a solid 576x240 rectangle printed fine. The `-v` packet log showed why:
the 270-row run went out as one `PRINT_BITMAP_ROW` packet with repeat `0x0e` - 270 truncated to the
single byte the field is. It had nothing to do with the bridge or with disconnect timing (the M2's
status poll only reports `page == 1` once print and feed progress are both 100).

niimbluelib never hits this because its encoder pushes a `check` marker row every 200 rows
(`row % 200 === 199`) *unconditionally* - only turning it into a `PrinterCheckLine` packet is
optional (`enableCheckLine`) - and that marker ends any run, capping repeats at 200. This port had
left the marker out as "not needed, test patterns are far smaller than 200 rows". Now ported
(`NiimbotImageEncoder`, `CHECK_ROW_INTERVAL`); the packet generator still skips the marker rather
than emitting a check-line packet. Regression test:
`NiimbotImageEncoderTest#longRunOfIdenticalRowsNeverExceedsOneByteRepeat`. After the fix the same
run goes out as repeat 152 + repeat 118, and the reprint came out complete (user-confirmed, through
the ESP32 bridge). This was a latent bug for every Niimbot model on any label with a long uniform stretch -
including real use through `StorageManagerServer` - not something the CLI introduced. **Lesson**:
"upstream only does X when a flag is on" needs checking at both ends - here the flag gated the
packet, not the marker, and the marker had a second job.

Two smaller things from the same session:

- `NiimbotDevice.waitUntilPrintFinishedByStatusPoll` now also waits for the last page's print and
  feed progress to read 100 (falling back to "progress unchanged for 3s" for a model that reports
  none) - a deliberate deviation from niimbluelib, which stops at `page == pagesToPrint`. The M2
  turned out not to need it, but per the user a printer that reports progress should be waited on
  to completion, since callers here disconnect right after printing.
- `print-test -v` turns on Niimbot packet logging (it's what found the bug above).

### Debugging history: three real issues found printing to the Q30, in order

Useful precedent for bringing up the next device - none of these were guessable from source alone:

1. **Wrong protocol family entirely** - see "Corrected finding" below. Fixed by reading phomymo's
   actual source instead of trusting a README's wording.
2. **Misaligned/doubled print** from an arbitrary un-rotated test-image height. In `d-series`, the
   pre-rotation image height becomes the printer's declared row width *after* rotation - it must
   equal the real printhead's physical dot-width (confirmed via phomymo's own
   `D_SERIES_LABEL_SIZES`, e.g. 96px/12mm), not be picked freely. An arbitrary too-small value
   produced a garbled, doubled-looking print rather than a clean error or a merely-cropped one -
   the printer appears to reinterpret the byte stream against its own fixed row width. Fixed by
   using one of phomymo's confirmed preset dimensions instead of guessing.
3. **Striped print** (bands along the feed direction, like gaps between printhead elements) after
   fix #2, that took several rounds to root-cause correctly:
   - First suspected BLE write-without-response pacing being unreliable on this stack (phomymo's
     own fixed inter-chunk delay assumes a browser's Web Bluetooth backpressure, which this
     Java/BSToolbox-BLE stack doesn't necessarily have) - switched to `withResponse=true` writes.
     No change.
   - Then suspected a weak printer battery (the symptom looked like a known undervoltage pattern on
     these thermal heads) - replaced it. No change.
   - Then suspected chunk/row misalignment (phomymo's fixed 128-byte chunks aren't a multiple of a
     12-byte row, so a chunk boundary - and its inter-chunk delay - can fall mid-row) - switched to
     row-aligned chunk sizing. No change.
   - **Root cause: an unreliable power source on the test rig** (not a fixed value - "weak
     battery" was on the right track, just not confirmed carefully enough the first time). All
     three code changes above were reverted back to matching phomymo's original choices exactly
     (plain write-without-response, fixed 128-byte chunks) - confirmed via a clean isolated retest
     (this exact reverted code, unchanged, with fresh/reliable batteries) that they print correctly
     as originally ported, with none of the extra complexity needed.
   - **Lesson**: when a symptom doesn't respond to any code change, stop changing code and isolate
     the environment/hardware variable first (power, in this case) - don't keep piling up unproven
     "defensive" fixes for a problem whose cause hasn't actually been confirmed yet.

### Corrected finding: the Phomemo Q30 does NOT speak the Niimbot protocol

Earlier in this project's history, phomymo's README table entry "D30/D35/D50/Q30/Q30S - similar to
D30" was misread as "speaks a Niimbot-compatible protocol." **That was wrong.** Live testing
against a real Q30, followed by reading phomymo's actual source
(`src/web/constants.js`/`printer.js`/`printers.json`), confirmed:

- The Q30 (and D30/D35/D50, per phomymo's own current `printers.json` - see the D110 correction
  below) speak phomymo's own **`d-series` protocol** - an ESC/POS-derived,
  **fire-and-forget** raster command stream (`ESC @` init, `GS v 0` raster header, `ESC d 0` /
  `ESC J n` feed, plus a couple of proprietary `0x1f 0x11 ..` config commands) - **not** Niimbot's
  `0x55 0x55 ... 0xAA 0xAA` request/response framing at all. "Similar to D30" meant "shares
  phomymo's `d-series` protocol tag," nothing more - it says nothing about Niimbot compatibility.
- This is why every attempt to get an `IN_CONNECT` response out of the Q30 failed: the protocol
  never sends acknowledgements between commands, so there was never going to be a `0x55 0x55...`
  reply to wait for, independent of the `0x03`-prefix quirk, write-with/without-response, or OS
  pairing - none of which were actually the problem.
- BLE channel: same as guessed/found via `gatt` - service `0000ff00`, write `0000ff02`
  (`WRITE`/`WRITE_WITHOUT_RESPONSE`), notify `0000ff03` - confirmed exactly matching phomymo's own
  `BLE.SERVICE_UUID`/`WRITE_CHAR_UUID`/`NOTIFY_CHAR_UUID` constants. So `BleTransport`'s
  channel-discovery logic was correct all along; only the bytes sent and the expectation of a reply
  were wrong. The `01 01` / `02 b6 00` notifications seen during testing are unrelated to any
  written command (confirmed by writing garbage and observing an identical, periodic pattern) -
  likely a status/telemetry ping independent of the print protocol.
- **Lesson for future device bring-up**: don't infer wire-protocol compatibility from a reference
  project's grouping/naming wording ("similar to X") without checking its actual protocol
  dispatch code. Verify against source or hardware before writing it into architecture docs.

Phomemo support therefore needed a **new protocol family** (`cz.bliksoft.ptlabelprint.protocol.phomemo`,
now created and confirmed working for `d-series`) ported from phomymo's
`printer.js`/`constants.js`/`printers.json` (MIT - safe to port directly, see "Licensing constraint"
below) rather than reusing `.protocol.niimbot`. See "Protocol families" below for the concrete
command bytes and the fact that Phomemo's own lineup actually spans several distinct sub-protocols
(`d-series` done, `m02`/`m04`/`m110`/generic `m-series`/`p12` not started), not just one.

The project went through two renames while being bootstrapped: `cz.bliksoft.niim` / `bsniimdriver`
(Niimbot-only) → `cz.bliksoft.labelprint` / `portable-labelprint` (added Phomemo D/Q-series scope)
→ current `cz.bliksoft.ptlabelprint` / `ptlabelprint` (shortened to avoid colliding with the
user's other label-printing libraries, e.g. the sibling `ZPL` project below). If you find a stray
`niim`/`labelprint`-only reference anywhere, it's a leftover from one of those renames, not
intentional.

## Build / test commands

```bash
mvn test              # compile + run the test suite (packet framing/parsing/generator round-trips;
                       # no real hardware involved)
mvn package -Pdist    # standalone CLI distribution: target/ptlabelprint-<version>/ (+ .zip)
```

`common-java-utils-ble` is on the published `0.9.0` release (`bstoolbox-ble.version` in `pom.xml`).
It was pointed at `0.10.0-SNAPSHOT` for a few hours on 2026-09-30 and put back for the 0.4.0
release: nothing had changed in BSToolbox-BLE since 0.9.0, so there was no 0.10.0 to wait for. Its
own flattened POM still drops its `jackson-databind` dependency, which is why this project declares
`jackson-databind` directly too - see the `pom.xml` comment on that dependency.

Manual real-hardware check, once built (`mvn package -Pdist`, then from `target/ptlabelprint-<version>/`):

```bash
./ptlabelprint-cli.sh scan                 # list nearby printers advertising Niimbot's own service UUID
                                            # (won't find a Q30 - it doesn't advertise that UUID; use
                                            # gatt/raw below, or an unfiltered scan, for non-Niimbot devices)
./ptlabelprint-cli.sh info <BLE address>   # connect and print PrinterInfo (Niimbot protocol only)
./ptlabelprint-cli.sh media <BLE address>  # connect and print live state (heartbeat) + loaded-roll
                                            # RFID info + sound settings (Niimbot protocol only)
./ptlabelprint-cli.sh niimbot-calibrate <address>   # label positioning calibration - USES REAL
                                            # CONSUMABLES (ejects ~15cm of paper)
./ptlabelprint-cli.sh niimbot-set-time <address>    # set the printer's real-time clock
./ptlabelprint-cli.sh niimbot-firmware-upgrade <address> <file> <version> --confirm-firmware-risk
                                            # HIGH RISK - can permanently brick the printer; never
                                            # tested against real hardware, see "Status"
./ptlabelprint-cli.sh gatt <BLE address>   # protocol-agnostic: dump GATT services/characteristics
./ptlabelprint-cli.sh raw <address> <hex>  # protocol-agnostic: write raw hex, print whatever comes back
                                            # (--service/--write-char/--notify-char/--with-response/--read
                                            # for testing an unfamiliar device's channel by hand;
                                            # --pair[=PIN] bonds first, --no-subscribe skips notify)
./ptlabelprint-cli.sh phomemo-m110-print-test <address> [--width-mm=40] [--height-mm=20]
                                            # alignment pattern via M110Printer - USES REAL CONSUMABLES
                                            # (Phomemo M421 confirmed; M110/M120 untested). Size it
                                            # to the loaded label. The M421 must be OS-paired first.
./ptlabelprint-cli.sh phomemo-print-test <address>   # prints a test square via DSeriesPrinter -
                                            # USES REAL CONSUMABLES (Phomemo D30/D35/D50/Q30/Q30S
                                            # only; confirmed working on a real Q30, see "Status").
                                            # --label picks any of DSeriesLabelSizes' mm presets
                                            # (default 12x12, the hardware-confirmed size)
./ptlabelprint-cli.sh discover             # unfiltered scan + PrinterCatalog family guess (any family)
./ptlabelprint-cli.sh connect <address>    # auto-detect + connect through the .printer abstraction layer
./ptlabelprint-cli.sh ports                # list serial ports (incl. paired classic-Bluetooth SPP ports)
./ptlabelprint-cli.sh print-test --serial COM31 --model M421 --pattern-mm 100x150
                                            # same print-test, over a serial port instead of BLE
./ptlabelprint-cli.sh print-test <address> [--image=<file> | --pattern-mm=40x20] [--copies] [--continuous] [--density]
                                            # [--rotation=auto|none|90|180|270]
                                            # USES REAL CONSUMABLES - manufacturer-agnostic
                                            # LabelPrinter#print(BufferedImage, PrintJob), any
                                            # detected family; default image is a synthetic solid
                                            # square sized to the connected printer's printhead
```

## What this is

A Java client library and CLI for BLE (optionally Serial/USB) label printers **across
manufacturers**, built as a printer abstraction layer over one or more wire-protocol families — not
a single-manufacturer driver. A Niimbot D11_H, a Niimbot M2, and a Phomemo Q30 are available
locally for real-hardware testing; be careful, consumables are expensive.

Two protocol families are in scope right now:

- **niimbot** — Niimbot-branded printers only (D11_H, M2), ported from
  [niimbluelib](https://github.com/MultiMote/niimbluelib) (MIT). Implemented in
  `cz.bliksoft.ptlabelprint.protocol.niimbot`, unverified against real hardware yet (see "Status").
  **Not** confirmed to cover any Phomemo model - see the corrected finding above.
- **phomemo** — Phomemo's own printer families (`d-series` covers the Q30 in hand, plus
  D30/D35/D50/Q30S (not D110 - see the correction near "Status"); also `m02`/`m04`/`m110`/generic
  `m-series`/`p12`/`tspl`), ported from
  [phomymo](https://github.com/transcriptionstream/phomymo) (MIT). `d-series` implemented and
  hardware-confirmed; the other 6 tags are now cataloged (all 17 of phomymo's non-d-series
  `printers.json` rows, in `protocol.phomemo.PhomemoPrinterModels`) but not implemented - see
  "Protocol families" below for the concrete bytes to port if/when one of them gets built.

**Zebra (ZPL) and Brother P-touch support are planned** as additional protocol families (see
"Protocol families" below) — not implemented yet, but the package layout and printer abstraction
layer are designed with them in mind.

Maven coordinates: `groupId cz.bliksoft.ptlabelprint`, `artifactId ptlabelprint`.

## Protocol families

Per [phomymo](https://github.com/transcriptionstream/phomymo)'s own source (`src/web/printer.js`,
`constants.js`, `printers.json`) — a niimblue-like browser app that already supports every Phomemo
family below in production, and is the direct inspiration for this project's
printer-abstraction-layer design:

- **niimbot** (in progress) — Niimbot-branded printers only (D11_H, M2 confirmed; not Phomemo, see
  "Status" above). `cz.bliksoft.ptlabelprint.protocol.niimbot`.
- **phomemo** (not implemented yet, but now unblocked with a verified MIT reference) — Phomemo's
  own lineup, which is itself **several distinct sub-protocols**, all dispatched by
  `printers.json`'s per-model `"protocol"` tag:
  - `d-series` — Q30/Q30S/D30/D35/D50 (**not** D110 - phomymo's own current `printers.json`
    `namePatterns` for this row are `["D30","D35","D50","Q30S","Q30","D"]`, no `"D110"` among them;
    an earlier version of this file's D-series lists included it anyway, almost certainly confused
    with Niimbot's own real `D110`/`D110_M` models - see `protocol.niimbot.PrinterModel` - given
    this exact project's own history of that exact confusion elsewhere; not re-added to
    `PrinterCatalog` since doing so with no phomymo source behind it would also permanently tie
    against the real Niimbot `D110` catalog entry for longest-prefix matching, on every device
    actually named `D110...`). **Implemented and confirmed printing on a real Q30**
    (`RasterImage`/`DSeriesCommands`/`DSeriesPrinter` in `cz.bliksoft.ptlabelprint.protocol.phomemo`
    - see "Status"/"Debugging history") - the print flow itself is fully model-generic (every
    dimension is a caller parameter, nothing Q30-specific), so D30/D35/D50/Q30S are expected to work
    via the identical code path, though only the Q30 is actually hardware-confirmed. BLE: service
    `0xff00`, write char `0xff02` (`WRITE`/`WRITE_WITHOUT_RESPONSE` - this port uses plain
    write-without-response, matching phomymo exactly; see "Debugging history" #3 for why that's
    confirmed fine as-is), notify char `0xff03` (unused for acks - this protocol is fire-and-forget).
    Rotated raster (labels print sideways - rotate the image 90° CW before sending,
    `RasterImage#rotate90Clockwise`, ported from phomymo's `rotateRaster90CW`). **The pre-rotation
    image height must equal the target printer's physical printhead dot-width** (confirmed the hard
    way - see "Debugging history" #2); `DSeriesLabelSizes` ports phomymo's own
    `D_SERIES_LABEL_SIZES` mm presets (8 entries) as the source of truth rather than guessing -
    `ptlabelprint-cli phomemo-print-test --label <key>` selects one.
    Commands (all one-way, no response expected, 128-byte chunks with a 20ms delay per
    `BLE.CHUNK_SIZE`/`CHUNK_DELAY_MS` - not row-aligned, and confirmed fine that way, see
    "Debugging history" #3):
    1. `HEAT_SETTINGS`: `1B 37 <maxDots=07> <heatTime> <heatInterval=02>` (`heatTime` from a
       density-1-to-8 → `[40,60,80,100,120,140,160,200]` lookup table)
    2. media type: `1F 11 <0x0A gaps | 0x0B continuous>`
    3. header: `1B 40` (ESC @ init) + `1D 76 30 00` (GS v 0, raster bit image) + width/height as
       four little-endian-split bytes (`n%256, n/256` each)
    4. raster data, chunked
    5. end: `1B 64 00` (ESC d 0 - triggers gap-detection cut/feed; continuous tape instead bakes
       feed into padding rows before sending, see `DSeriesPrinter`'s `CUTTER_OFFSET_ROWS`/`feedDots` handling)
  - `m02`, `m04`, `m110`, generic `m-series`, `p12` — other Phomemo model families, each with their
    own command set in `printer.js` (`M02_CMD`/`M04_CMD`/`M110_CMD`/`CMD`/`P12_CMD`). **Cataloged**
    (`PhomemoPrinterModel`/`PhomemoPrinterModelMeta`/`PhomemoPrinterModels` - all 12 models across
    these 5 tags, ported verbatim from `printers.json`: width, dpi, alignment, rotation, tape) but
    **not implemented** (no command-builder/print-flow code) - port on demand when a model in one of
    these families needs real print support. `PrinterFamily` has a constant per tag
    (`PHOMEMO_M02`/`PHOMEMO_M04`/`PHOMEMO_M110`/`PHOMEMO_M_SERIES`/`PHOMEMO_P12`) so
    `PrinterCatalog`/`PrinterFactory` correctly *identify* a device from one of them instead of
    misdispatching it as `d-series` - `PrinterFactory` throws `UnimplementedPrinterFamilyException`
    for these, not a generic "unknown printer" error.
  - `tspl` — PM-241, a **text-based** protocol (`SIZE`/`GAP`/`DENSITY`/`BITMAP`/`PRINT`/`END` as
    CRLF-terminated ASCII commands, per phomymo's `TSPL` object) - generic enough to also cover
    non-Phomemo TSPL printers. **Cataloged** (`PhomemoPrinterModel.PM241` in `PhomemoPrinterModels`,
    `PrinterFamily.PHOMEMO_TSPL`) but not implemented, same as the tags above.
  A dispatcher keyed on `printers.json`'s `protocol` field (mirroring `printer.js`'s per-family
  command objects) isn't built yet - `d-series` is used directly since it's the only sub-protocol
  implemented so far; add that dispatch once a second sub-protocol lands.
- **zebra** (planned) — Zebra printers accept raw ZPL text/bytes written to a serial-like BLE
  characteristic; there is no binary framing/CRC protocol to port here, unlike niimbot (confirmed
  via [Zebra's own developer blog on BLE printing](https://developer.zebra.com/community/home/blog/2017/12/21/printing-labels-through-bluetooth-low-energy-on-ios),
  which is the closest thing to an official reference — no public GitHub repo of Zebra's own BLE
  demo code was found). Would land in `cz.bliksoft.ptlabelprint.protocol.zebra` — mostly transport
  + printer-definition glue, not a framing implementation. **Don't reimplement ZPL generation
  here**: the user's own sibling library `cz.bliksoft.java:zpl` (source at
  `c:\Users\jakub\work\Bliksoft\Java\GIT\ZPL\`, package `cz.bliksoft.zpl`) already does that —
  FreeMarker macros for text/barcodes/QR/shapes, `ZPLConverter` for image→`^GFA` graphics, plus an
  in-process ZPL emulator for rendering/testing without hardware. This `zebra` package would
  depend on it the same `provided`+optional way `common-java-utils` is used for IconSpecEngine.
- **brother** (planned) — Brother P-touch. More fragmented than niimbot/zebra/phomemo: protocol
  varies across the older USB-only models (e.g. PT-1230-style), the USB QL-series (raster
  language), and the newer BLE PT-P300BT-style models — likely three sub-cases, not one, once
  implemented. Would land in `cz.bliksoft.ptlabelprint.protocol.brother`. References found (verify
  against current hardware before trusting exact bytes — these are all volunteer
  reverse-engineering, not vendor specs):
  - [TomasHubelbauer/brother-p-touch-d600](https://github.com/TomasHubelbauer/brother-p-touch-d600)
    — MIT. USB communication trace / status-query docs for the D600.
  - [pklaus/brother_ql](https://github.com/pklaus/brother_ql) — GPL-3.0. QL-series raster protocol,
    well documented; facts-only reference (see licensing note below).
  - [furrtek/PTouchHH](https://github.com/furrtek/PTouchHH) — GPL-3.0. Handheld P-touch
    reverse-engineering; facts-only reference.
  - [cbdevnet/pt1230](https://github.com/cbdevnet/pt1230) — no license declared on GitHub; treat as
    all-rights-reserved, reference-only (don't even treat it as "facts you can act on" without
    separately verifying against hardware/other sources).

The printer abstraction layer (`cz.bliksoft.ptlabelprint.printer`) is what dispatches across all of
these — declarative per-model definitions (currently just which protocol family a model speaks;
label width/DPI/rotation stay in each family's own device configmaps, e.g. `PrinterModels` for
niimbot) plus auto-detection from the BLE advertised name, mirroring phomymo's `printers.json` +
auto-detect approach. **Implemented and confirmed on real hardware** (D11_H, M2_H) once there were
two real families to actually generalize across - see "Status" above for what's confirmed and
"Architecture" below for the package layout. Has catalog entries for `niimbot` (all 77 models),
`phomemo_d_series`, and the 6 cataloged-but-unimplemented Phomemo sub-protocols (14 of their 17
models have a BLE name to match on - see "Status"); extend `PrinterCatalog` further when
`zebra`/`brother` get implemented.

## Licensing constraint on protocol research

- [niimbluelib](https://github.com/MultiMote/niimbluelib) / [niimblue](https://github.com/MultiMote/niimblue),
  [phomymo](https://github.com/transcriptionstream/phomymo), and
  [TomasHubelbauer/brother-p-touch-d600](https://github.com/TomasHubelbauer/brother-p-touch-d600) —
  MIT. Safe to port code/logic from directly, with attribution. phomymo is now the primary source
  for the `phomemo` protocol family (see above) - its `printer.js`/`constants.js` command bytes are
  meant to be ported close to verbatim (translated to Java), not just used as design inspiration.
- [vivier/phomemo-tools](https://github.com/vivier/phomemo-tools),
  [yaddran/thermal-print](https://github.com/yaddran/thermal-print),
  [pklaus/brother_ql](https://github.com/pklaus/brother_ql), and
  [furrtek/PTouchHH](https://github.com/furrtek/PTouchHH) — **GPL-3.0**. Usable only as references
  for wire-format *facts* (frame layout, command bytes, etc. — facts aren't copyrightable the way
  code expression is); **no code may be ported from them** into this MIT-licensed project. Any
  protocol family built primarily from a GPL-3.0 reference must be a clean-room reimplementation.
- [cbdevnet/pt1230](https://github.com/cbdevnet/pt1230) — no declared license: treat as
  all-rights-reserved, reference-only, same as the GPL sources above (arguably more conservatively,
  since there's no explicit grant to rely on at all).

## License

MIT (matching niimbluelib and phomymo). The README links/credits both, plus the GPL-3.0 Brother/
Phomemo research repos as reference-only, and should be checked against each project's current
`master` for upstream protocol/configmap updates before extending device support.

## Architecture

Modeled on `java-bshmidriver` (`c:\Users\jakub\work\Bliksoft\Java\GIT\java-bshmidriver\`) for
project wiring/deployment — read that repo's CLAUDE.md/README for the concrete pattern this
mirrors — combined with `phomymo`'s printer-abstraction-layer design for the
manufacturer/protocol-family split described above:

- **Single Maven module**, not multi-module. The CLI is the same jar with a `Main-Class` manifest
  entry, distributed via a `dist` build profile (maven-dependency-plugin `copy-dependencies` +
  maven-assembly-plugin, off by default, `mvn package -Pdist`) that bundles `provided`-scope runtime
  deps into `lib/` alongside launch scripts (`.sh`/`.bat`/`.command`). Not a separate `-cli` module.
- **BLE dependency stays `provided`**, deliberately unpinned to a specific version, so a consuming
  application supplies `common-java-utils-ble` on its own runtime classpath only if it actually uses
  the BLE transport. Same treatment for `jSerialComm` if/when serial support is added, and for
  `cz.bliksoft.java:zpl` once the `zebra` family exists. The standalone CLI distribution packs
  whichever of these are actually used into its `lib/` folder (it *is* the consuming application).
- **IconSpecEngine integration must stay decoupled**, loaded/used only when a caller actually
  invokes icon-spec-based printing — mirror `java-bshmidriver`'s `IconSpecCache` pattern
  (`common-java-utils` as `provided`, an `isAvailable()` runtime check, a clear error instead of a
  hard dependency). Reference: `IconSpecEngine`/`ImageFilter` at
  `c:\Users\jakub\work\Bliksoft\Java\GIT\BSToolbox\src\main\java\cz\bliksoft\javautils\images\iconspec\`.
  Image scaling/dithering/B&W conversion beyond that is deferred — pull from BSToolbox's images
  package later rather than reimplementing now.
- **Package split**: `cz.bliksoft.ptlabelprint.protocol` holds what's genuinely shared across
  families - `Transport` (the generic connect/write/receive interface) and `BleTransport` (its
  BSToolbox-BLE impl, GATT channel discovery only, no protocol-specific framing; named after the
  mechanism, not any brand, since the same discovery logic isn't brand-specific - see its own
  javadoc). Moved here from `.protocol.niimbot` once `.protocol.phomemo` needed the same transport
  too - don't move framing/commands here, only what's truly shared.
  - `.protocol.niimbot` - the Niimbot family: framing/commands (`NiimbotPacket`,
    `RequestCommandId`/`ResponseCommandId`, `PacketGenerator`/`PacketParser`), device configmaps
    (`PrinterModel`/`PrinterModelMeta`/`PrinterModels` - full 77-model port), image encoding
    (`NiimbotImageEncoder`), the 7 print flows (`AbstractNiimbotPrintTask` and its subclasses -
    `D110V4PrintTask`/`B1PrintTask`/`OldD11PrintTask`/`D110PrintTask`/`B21V1PrintTask`/
    `B21L2BPrintTask`/`H1SPrintTask`; `NiimbotPrintTasks` is the model→task dispatch table), and the
    high-level API (`NiimbotDevice`). Confirmed against a real D11_H and M2_H, both info and
    printing (see "Status") - the other 5 print tasks/67 models are ported but untested.
  - `.protocol.phomemo` - Phomemo's families: `RasterImage` (1bpp raster + rotation),
    `DSeriesCommands` (byte builders), `DSeriesPrinter` (the `d-series` print flow, model-generic -
    confirmed working against a real Q30, D30/D35/D50/Q30S untested but expected to work via the
    same code path - see "Status"/"Debugging history"), `DSeriesLabelSizes` (phomymo's own
    `D_SERIES_LABEL_SIZES` mm presets). `PhomemoPrinterModel`/
    `PhomemoPrinterModelMeta`/`PhomemoPrinterModels` catalog the other 6 sub-protocols
    (`m02`/`m04`/`m110`/generic `m-series`/`p12`/`tspl`) - data only, no command-builder/print-flow
    code for any of them yet; add sibling classes/sub-packages here when one is actually ported,
    dispatched (once there's a second real sub-protocol to dispatch between) the way
    `printers.json`'s `protocol` field does upstream.
  `.printer` is the cross-family abstraction layer, now implemented (see "Status" above):
  `PrinterFamily`/`PrinterDefinition`/`PrinterCatalog` (name-based family detection, including the
  6 cataloged-but-unimplemented Phomemo families), `LabelPrinter`/`AbstractLabelPrinter`/
  `NiimbotLabelPrinter`/`PhomemoLabelPrinter`/`PhomemoDSeriesLabelPrinter`/`PrinterFactory`/
  `UnimplementedPrinterFamilyException` (unified connect lifecycle + per-family dispatch, with a
  real (not speculative) shared-implementation hierarchy beneath `LabelPrinter`, plus `PrintJob`/
  `Rotation`/`RotationResolver` for the common `print(BufferedImage, PrintJob)` entry point - see
  `LabelPrinter`'s own javadoc for exactly what that surface covers vs. what stays family-specific,
  and `PhomemoLabelPrinter`'s for why it exists despite having one concrete subclass today). `.image`
  now has real content (see "Unified image-print abstraction" above): `PixelSource` (moved here from
  `.protocol.niimbot`), `BufferedImagePixelSource`, `CanvasResize`, `ImageRotation`,
  `PrinterCapabilities`. An IconSpecEngine bridge still isn't built - not worth it without a caller
  that needs icon-spec-based printing yet.
  `cz.bliksoft.ptlabelprint.Cli` (top level) and `.printer` are the only other packages with real
  code. Don't create `.protocol.zebra`/`.protocol.brother` package stubs before there's real work
  to put in them - the "Protocol families" section above documents intent, not required
  scaffolding. **Narrow exception**: the 6 `PrinterFamily.PHOMEMO_*` constants beyond
  `PHOMEMO_D_SERIES` (and their `PrinterCatalog`/`PhomemoPrinterModels` data) are *not* a violation
  of this rule despite having no print-flow implementation - no new `.protocol.phomemo.m02`-style
  *package* was created for any of them, only enum/catalog data, added specifically so
  `PrinterCatalog`/`PrinterFactory` correctly identify one of these devices (`PrinterFactory` throws
  a distinct `UnimplementedPrinterFamilyException`) instead of misdispatching it as `d-series` -
  exactly the class of bug this file's own "Corrected finding" section is about. This is scoped
  narrowly to Phomemo's already-fully-documented sub-protocols above; it is not a general license to
  add family constants speculatively (zebra/brother still get none).
- **CLI**: `scan`/`info`/`gatt`/`raw` (Niimbot-focused, plus protocol-agnostic BLE diagnostics),
  `niimbot-print-test`/`phomemo-print-test` (exercise each family's own real print flow end to end -
  see "Build / test commands" above), `discover`/`connect` (go through the `.printer` abstraction
  layer - manufacturer-agnostic scan + auto-detect + unified connect), and `print-test`
  (manufacturer-agnostic image print through `LabelPrinter#print(BufferedImage, PrintJob)` - see
  "Unified image-print abstraction" above) are all implemented. A dedicated iconspec-print command
  still isn't built - land it here once a caller actually needs icon-spec-based printing - similar
  shape to `java-bshmidriver`'s `Cli`/`HmiUtils` (one transport-connect helper per transport, so
  using BLE-only never forces Serial's dependency onto the classpath, and vice versa) is still the
  intended pattern once a Serial transport exists.

## BSToolbox-BLE API surface (what the BLE transport will be built on)

Package `cz.bliksoft.javautils.ble` (`cz.bliksoft.java:common-java-utils-ble`). Core flow: scan on
a `BleAdapter` (a peripheral must be discovered via `scan()` on that same adapter instance before
`getPeripheral(address)`/`connect()` will work on it), then use `BlePeripheral` for
discover/read/write/subscribe. `BleUtils` wraps common patterns (`scan`, `find`, `findOne`) and
`ScanFilter` supports exact (`withAddress`/`withName`, auto-stopping) or substring
(`withMatchingAddress`/`withMatchingName`) matching. See that repo's README for the full API and
usage examples — don't re-derive it from scratch.

Note from that library's own docs: GATT-level bonding/pairing is not implemented yet, and Niimbot
printers are called out there as a device family that may need it — a printer requiring OS-level
pairing before GATT access will need to be paired through the OS first until that lands upstream.

## Release process

Tagged releases (Maven Central + GitHub release with the `-Pdist` CLI zip attached) use the
`prepare-maven-release` / `deploy-maven-release` skills — don't hand-edit the `<revision>` property
or run `mvn deploy` directly. Untagged `master` pushes also publish a `-SNAPSHOT` build to
Forgejo's own package registry via `.forgejo/workflows/maven-snapshot-deploy.yml`
(`maven-mirror-sync.yml` forces an immediate pull-mirror sync so that snapshot deploy doesn't wait
for Forgejo's periodic mirror interval). `.forgejo/workflows/` files are the source of truth on
GitHub (Forgejo pull-mirrors this repo); a commit made only on the Forgejo side would be silently
discarded on the next sync.
