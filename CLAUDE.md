# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: both protocol families confirmed printing on real hardware

`cz.bliksoft.ptlabelprint.protocol.niimbot` has a real (partial) port of niimbluelib's protocol:
packet framing/checksum (`NiimbotPacket`), the full `RequestCommandId`/`ResponseCommandId` catalog,
`NiimbotDevice` (connect handshake, printer info, heartbeat, RFID info, printer reset, plus the
page/bitmap-row/print-start-end primitives), `NiimbotImageEncoder` (ported from niimbluelib's
`ImageEncoder` - row encoding, run-length row collapsing, indexed/full bitmap row selection), and
`D110V4PrintTask` (ported from niimbluelib's `D110MV4PrintTask` - the print task niimbluelib's own
model-dispatch table assigns to the D11_H, among others) and `B1PrintTask` (ported from
niimbluelib's `B1PrintTask` - the print task the same dispatch table assigns to the M2_H, among
others; **not** `D110MV4PrintTask` despite the similar "D110M" naming - `Cli`'s
`niimbot-print-test` picks between the two by model) - all built on the shared `BleTransport`.
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

`Cli` wires the family up with `scan`/`info`/`media`/`gatt`/`raw`/`niimbot-print-test` subcommands.
**Not implemented yet**: Serial transport, firmware upgrade, other print tasks (`OldD11PrintTask`/
`D110PrintTask`/etc. - `D110V4PrintTask` and `B1PrintTask` are the only two ported, since they're
the only ones tested), and a real image pipeline (`niimbot-print-test` hand-builds a `PixelSource`,
no dithering/scaling from an arbitrary source image). See each class's javadoc for exactly which
niimbluelib methods it ports vs. omits.

`cz.bliksoft.ptlabelprint.protocol.phomemo` (`RasterImage`, `DSeriesCommands`, `DSeriesPrinter`)
ports phomymo's `d-series` protocol and **has successfully printed on a real Phomemo Q30** via
`ptlabelprint-cli phomemo-print-test <address>` - a 12mm x 12mm solid square, correctly sized,
positioned, and shaped. Getting there took three real-hardware iterations; see "Debugging history"
below for what each one actually was (useful precedent for the next device/model). Not implemented
yet for `phomemo`: the other sub-protocols (`m02`/`m04`/`m110`/generic `m-series`/`p12`/`tspl`) and
a real image pipeline (dithering/scaling into a `RasterImage` from an arbitrary source image) - the
test command hand-builds a trivial raster.

`cz.bliksoft.ptlabelprint.printer` - the printer abstraction layer - is now implemented, once there
were two real protocol families to actually generalize across: `PrinterCatalog` (BLE-advertised-
name → `PrinterDefinition`, longest-prefix-wins, mirroring phomymo's own `detectPrinterConfig`) and
`LabelPrinter` (a *connection-lifecycle-only* common interface - `NiimbotLabelPrinter`/
`PhomemoDSeriesLabelPrinter` expose each family's real, different print API after connecting; see
`LabelPrinter`'s own javadoc for why printing itself isn't unified - Phomemo's `d-series` has no
info-query capability at all, unlike Niimbot's rich `PrinterInfoType` catalog, and forcing one
print signature across genuinely different image models would lose real capability). **Confirmed
against real hardware**: `ptlabelprint-cli discover` (an unfiltered scan cross-referenced against
`PrinterCatalog` - the fix for `scan`'s Niimbot-service-UUID filter finding neither real device
family, noted below) correctly identified a real D11_H's advertised name
(`D11_H-G412010570` → `D11_H(NIIMBOT)`) live over the air; `ptlabelprint-cli connect <address>` then
auto-detected, dispatched via `PrinterFactory`, and connected through `NiimbotLabelPrinter`,
producing the same `PrinterInfo` as the family-specific `info` command. The catalog only has
entries for the models each family's protocol code already supports (the niimbluelib-ported
`PrinterModels` entries for `NIIMBOT`; `D30`/`D35`/`D50`/`Q30`/`Q30S` for `PHOMEMO_D_SERIES`,
deliberately excluding phomymo's own bare `"D"` wildcard pattern since it would collide with every
Niimbot D-series name) - see `PrinterDefinition#isConfirmedOnHardware()` for which of those are
actually hardware-tested (only D11_H and Q30 so far) vs. just ported-and-untested.

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

- The Q30 (and D30/D35/D50/D110) speak phomymo's own **`d-series` protocol** - an ESC/POS-derived,
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
                                            # RFID info (Niimbot protocol only)
./ptlabelprint-cli.sh gatt <BLE address>   # protocol-agnostic: dump GATT services/characteristics
./ptlabelprint-cli.sh raw <address> <hex>  # protocol-agnostic: write raw hex, print whatever comes back
                                            # (--service/--write-char/--notify-char/--with-response/--read
                                            # for testing an unfamiliar device's channel by hand)
./ptlabelprint-cli.sh phomemo-print-test <address>   # prints a small 12x12mm test square via
                                            # DSeriesPrinter - USES REAL CONSUMABLES (Phomemo
                                            # D30/D35/D50/D110/Q30/Q30S only; confirmed working
                                            # on a real Q30, see "Status")
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
  D30/D35/D50/D110; also `m02`/`m04`/`m110`/generic `m-series`/`p12`), ported from
  [phomymo](https://github.com/transcriptionstream/phomymo) (MIT). Not implemented yet - see
  "Protocol families" below for the concrete bytes to port.

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
  - `d-series` — Q30/Q30S/D30/D35/D50/D110. **Implemented and confirmed printing on a real Q30**
    (`RasterImage`/`DSeriesCommands`/`DSeriesPrinter` in `cz.bliksoft.ptlabelprint.protocol.phomemo`
    - see "Status"/"Debugging history"). BLE: service `0xff00`, write char `0xff02`
    (`WRITE`/`WRITE_WITHOUT_RESPONSE` - this port uses plain write-without-response, matching
    phomymo exactly; see "Debugging history" #3 for why that's confirmed fine as-is), notify char
    `0xff03` (unused for acks - this protocol is fire-and-forget). Rotated raster (labels print
    sideways - rotate the image 90° CW before sending, `RasterImage#rotate90Clockwise`, ported from
    phomymo's `rotateRaster90CW`). **The pre-rotation image height must equal the target printer's
    physical printhead dot-width** (confirmed the hard way - see "Debugging history" #2); use
    phomymo's `D_SERIES_LABEL_SIZES` as the source of truth per model rather than guessing.
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
    own command set in `printer.js` (`M02_CMD`/`M04_CMD`/`M110_CMD`/`CMD`/`P12_CMD`); not implemented,
    port on demand when a model in one of these families needs support.
  - `tspl` — PM-241, a **text-based** protocol (`SIZE`/`GAP`/`DENSITY`/`BITMAP`/`PRINT`/`END` as
    CRLF-terminated ASCII commands, per phomymo's `TSPL` object) - generic enough to also cover
    non-Phomemo TSPL printers. Not implemented.
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
auto-detect approach. **Implemented and confirmed on real hardware** (D11_H) once there were two
real families to actually generalize across - see "Status" above for what's confirmed and
"Architecture" below for the package layout. Only has catalog entries for `niimbot`/
`phomemo_d_series` so far; extend `PrinterCatalog` when `zebra`/`brother`/other `phomemo`
sub-protocols get implemented.

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
    (`PrinterModel`/`PrinterModelMeta`/`PrinterModels`), image encoding (`NiimbotImageEncoder`),
    the print flow (`D110V4PrintTask`), and the high-level API (`NiimbotDevice`). Confirmed
    against a real D11_H, both info and printing (see "Status").
  - `.protocol.phomemo` - Phomemo's families: `RasterImage` (1bpp raster + rotation),
    `DSeriesCommands` (byte builders), `DSeriesPrinter` (the `d-series` print flow). Confirmed
    working against a real Q30 (see "Status"/"Debugging history"). Other Phomemo sub-protocols
    (`m02`/`m04`/`m110`/generic `m-series`/`p12`/`tspl`) aren't ported yet - add sibling
    classes/sub-packages here when one is needed, dispatched (once there's a second sub-protocol to
    dispatch between) the way `printers.json`'s `protocol` field does upstream.
  `.printer` is the cross-family abstraction layer, now implemented (see "Status" above):
  `PrinterFamily`/`PrinterDefinition`/`PrinterCatalog` (name-based family detection),
  `LabelPrinter`/`NiimbotLabelPrinter`/`PhomemoDSeriesLabelPrinter`/`PrinterFactory` (unified
  connect lifecycle + per-family dispatch - see `LabelPrinter`'s own javadoc for why printing
  itself stays family-specific). `.image` (image staging/encoding, optional IconSpecEngine bridge)
  is still just a package-info - not worth building without a real image pipeline yet.
  `cz.bliksoft.ptlabelprint.Cli` (top level) and `.printer` are the only other packages with real
  code. Don't create `.protocol.zebra`/`.protocol.brother` package stubs before there's real work
  to put in them - the "Protocol families" section above documents intent, not required
  scaffolding.
- **CLI**: `scan`/`info`/`gatt`/`raw` (Niimbot-focused, plus protocol-agnostic BLE diagnostics),
  `niimbot-print-test`/`phomemo-print-test` (exercise each family's print flow end to end - see
  "Build / test commands" above), and `discover`/`connect` (go through the `.printer` abstraction
  layer instead - manufacturer-agnostic scan + auto-detect + unified connect) are all implemented.
  Further configure/send-image/iconspec-print commands land here once there's a real image pipeline
  to drive them - similar shape to `java-bshmidriver`'s `Cli`/`HmiUtils` (one transport-connect
  helper per transport, so using BLE-only never forces Serial's dependency onto the classpath, and
  vice versa) is still the intended pattern once a Serial
  transport exists.

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
