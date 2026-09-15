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
`Rotation.NONE` uses that as final regardless of fit, an explicit `CW_90`/`CW_180`/`CW_270`
pre-rotates the *original* source by that much before the mandatory step (an authoritative override,
skips auto-fit), and the default `Rotation.AUTO` tries the mandatory-only result first and, only if
it doesn't fit the printhead axis, tries exactly one additional 90°CW pre-rotation before falling
back to cropping (`RotationResolverTest` verifies this math directly, including the not-yet-applied-
mandatory-rotation-aware fit check). Phomemo also gained `DSeriesPrinterModel`/
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

**D11_H needs a real hardware re-verification, not just code review, next time it's available**:
implementing `PrintDirection`-aware rotation is a genuine behavior change to an already-hardware-
confirmed path - niimbluelib rotates 90°CW whenever `PrintDirection == LEFT` (D11_H's value; M2_H's
is `TOP`, needing no rotation, unchanged), which this project's own `NiimbotImageEncoder` never
implemented before now. The earlier "confirmed working" D11_H print used a solid rectangle, which
can't reveal a 90°-orientation bug - so this was a real latent gap the existing test couldn't have
caught, not a regression risk introduced by this change. Use an **asymmetric** test pattern (e.g. an
"L"/"F" shape, or a rectangle with one corner marked) for the re-check, not another solid one.

**Neither family's real device advertises the service UUID `BleTransport.scanFilter()`/the CLI's
`scan` command filters on** - confirmed for both the Phomemo Q30 and a genuine Niimbot D11_H (found
instead via an unfiltered scan, by name: `D11_H-<serial>`). niimbluelib's own scan logic treats
that UUID as only one of several OR'd criteria, alongside model-name-prefix matching, which is
evidently the one that actually matters in practice on real hardware. **Fixed** by `discover`
(unfiltered scan + `PrinterCatalog` name matching, see above) - `scan`'s own UUID-only filter is
left as-is (still not very useful for either real family) since `discover` is the better tool now,
not because the underlying gap was left open.

`cz.bliksoft.ptlabelprint.protocol.Transport` is shared by both families - a plain
`connect`/`disconnect`/`write(byte[])`/`setRawDataListener` contract, no per-write response-mode
knob (tried adding one during debugging, reverted once it proved unnecessary - see "Debugging
history").

A git repo was initialized locally (`git init`) but nothing has been committed yet.

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

`common-java-utils-ble` is on the published `0.5.0` release (for its unified `ScanFilter` API) -
its own flattened POM still drops its `jackson-databind` dependency (confirmed present in the
actual 0.5.0 release, not just an older snapshot), which is why this project declares
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
                                            # for testing an unfamiliar device's channel by hand)
./ptlabelprint-cli.sh phomemo-print-test <address>   # prints a test square via DSeriesPrinter -
                                            # USES REAL CONSUMABLES (Phomemo D30/D35/D50/Q30/Q30S
                                            # only; confirmed working on a real Q30, see "Status").
                                            # --label picks any of DSeriesLabelSizes' mm presets
                                            # (default 12x12, the hardware-confirmed size)
./ptlabelprint-cli.sh discover             # unfiltered scan + PrinterCatalog family guess (any family)
./ptlabelprint-cli.sh connect <address>    # auto-detect + connect through the .printer abstraction layer
./ptlabelprint-cli.sh print-test <address> [--image=<file>] [--copies] [--continuous] [--density]
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
