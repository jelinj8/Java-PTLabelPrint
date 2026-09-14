# PtLabelPrint

Java client library and CLI for BLE (optionally Serial/USB) label printers across manufacturers -
a printer abstraction layer over one or more wire-protocol families, so callers work against one
typed API regardless of brand. A Niimbot D11_H, a Niimbot M2, and a Phomemo Q30 are available for
real-hardware testing; **both the Niimbot and Phomemo `d-series` protocols print successfully on
real hardware** (see "Status").

## Status

The Niimbot protocol is implemented and **confirmed printing on a real Niimbot D11_H**: connect
handshake, model ID (correctly resolved via `PrinterModels`), serial number, battery, label type,
firmware versions, and DPI class all came back correct via `info` (no OS pairing needed), and
`niimbot-print-test` printed successfully across three runs. One run had a single missing line;
a same-parameters retry showed it in a different position with an irregular appearance - evidence
points to a mechanical printhead artifact, not a bug in the row encoding (see CLAUDE.md's "Status"
for the reasoning). Unlike Phomemo's `d-series`, the Niimbot protocol doesn't require the image
width to exactly match the printhead's physical capacity:

```bash
mvn package -Pdist
cd target/ptlabelprint-<version>/
./ptlabelprint-cli.sh info <BLE address>              # connect and print model/serial/battery/etc.
./ptlabelprint-cli.sh niimbot-print-test <address>    # prints a small full-width test rectangle -
                                                       # USES REAL CONSUMABLES
./ptlabelprint-cli.sh scan                 # list nearby printers advertising Niimbot's service UUID -
                                            # NOTE: real Niimbot printers often don't advertise it (see
                                            # below); find yours by an unfiltered scan/name instead
./ptlabelprint-cli.sh gatt <BLE address>   # protocol-agnostic: dump GATT services/characteristics
./ptlabelprint-cli.sh raw <address> <hex>  # protocol-agnostic: write raw hex, print the response
```

Phomemo's `d-series` protocol (D30/D35/D50/D110/Q30/Q30S) is implemented and **confirmed printing
on a real Q30**:

```bash
./ptlabelprint-cli.sh phomemo-print-test <BLE address>   # prints a small 12x12mm test square -
                                                          # USES REAL CONSUMABLES
```

**Neither device advertises the Niimbot service UUID `scan`/`BleTransport.scanFilter()` filter on**
- confirmed for both the Q30 and a genuine D11_H (found instead via an unfiltered scan, by name:
`D11_H-<serial>`). Not fixed yet; see CLAUDE.md's "Status" for the full note.

**Corrected finding**: a Phomemo Q30 does **not** speak the Niimbot protocol - live testing plus
reading phomymo's own source confirmed it speaks a completely different, ESC/POS-derived,
fire-and-forget protocol (phomymo's own `d-series` tag - see "Protocol families" below). An earlier
version of this project incorrectly assumed "similar to D30" in phomymo's README meant Niimbot
compatibility; it doesn't.

Getting `d-series` working on real hardware took three real issues, in order: the wrong protocol
family (above), a garbled/misaligned print from an arbitrary test-image size (the pre-rotation
image height must match the real printhead's physical dot-width - use phomymo's own
`D_SERIES_LABEL_SIZES` as ground truth, don't guess), and a striped/weak print that took several
rounds to root-cause - two code-side hypotheses (BLE write pacing, chunk/row alignment) were tried
and ruled out before landing on the real cause: an unreliable power source on the test rig. The
code ended up back to exactly matching phomymo's original approach (plain write-without-response,
fixed 128-byte chunks) - confirmed correct once tested with reliable power. See CLAUDE.md's
"Debugging history" for the full account - useful precedent before bringing up another device.

Not implemented yet: a real image pipeline (dithering/scaling arbitrary images into a raster -
`phomemo-print-test` hand-builds a trivial one), Phomemo's other sub-protocols, the printer
abstraction layer, and a Serial transport.

## Scope

Not every printer sold under these brands speaks the same wire protocol - see "Protocol families"
below.

- **phomemo** (`d-series` implemented and hardware-confirmed) - Phomemo's own printer families,
  most relevantly `d-series` (D30/D35/D50/D110/Q30/Q30S).
- **niimbot** (implemented, unverified against real hardware yet) - Niimbot-branded printers
  (D11_H, M2) only.

Zebra (ZPL) and Brother P-touch support are planned as additional protocol families (see below).

## Protocol families

- **niimbot** - Niimbot-branded printers only. Ported from
  [niimbluelib](https://github.com/MultiMote/niimbluelib) (MIT). **Not** Phomemo's D/Q-series -
  see the corrected finding above.
- **phomemo** - Phomemo's own lineup, which is itself several distinct sub-protocols dispatched by
  [phomymo](https://github.com/transcriptionstream/phomymo) (MIT)'s `printers.json`:
  - `d-series` (Q30/Q30S/D30/D35/D50/D110) - **implemented, printed successfully on a real Q30**
    (`RasterImage`/`DSeriesCommands`/`DSeriesPrinter`). ESC/POS-derived, **fire-and-forget** (no
    responses to wait for), rotated raster printing. BLE: service `0xff00`, write `0xff02`, notify
    `0xff03` (notify channel isn't used for acks in this protocol). See CLAUDE.md's "Protocol
    families" section for the exact command bytes.
  - `m02`/`m04`/`m110`/generic `m-series`/`p12` - other Phomemo families, not yet tested here.
  - `tspl` (PM-241) - a text-based protocol, generic enough to cover non-Phomemo TSPL printers too.
  Would land in `cz.bliksoft.ptlabelprint.protocol.phomemo`.
- **zebra** (planned) - Zebra printers accept raw ZPL text/bytes on a serial-like BLE
  characteristic - no binary framing protocol to port, unlike niimbot (see
  [Zebra's own developer blog on BLE printing](https://developer.zebra.com/community/home/blog/2017/12/21/printing-labels-through-bluetooth-low-energy-on-ios)).
  The ZPL content itself can be generated with the sibling `cz.bliksoft.java:zpl` library
  (`../ZPL`; FreeMarker macros + `ZPLConverter` for images) rather than reimplemented here - this
  package would mostly be transport + printer-definition glue.
- **brother** (planned) - Brother P-touch. More fragmented than niimbot/zebra/phomemo: protocol
  varies somewhat across the USB QL-series, the older PT-1230-style USB models, and the newer BLE
  PT-P300BT-style models. References (see licensing note below):
  [TomasHubelbauer/brother-p-touch-d600](https://github.com/TomasHubelbauer/brother-p-touch-d600)
  (MIT, USB trace/status-query docs), and reverse-engineering-only reads of
  [pklaus/brother_ql](https://github.com/pklaus/brother_ql) (GPL-3.0, QL-series raster protocol),
  [furrtek/PTouchHH](https://github.com/furrtek/PTouchHH) (GPL-3.0), and
  [cbdevnet/pt1230](https://github.com/cbdevnet/pt1230) (no declared license - reference-only).

The printer abstraction layer (`cz.bliksoft.ptlabelprint.printer`) is what dispatches across these
families - declarative per-model definitions (label width/DPI, rotation/alignment quirks, which
protocol family a model speaks) plus auto-detection from the BLE advertised name, modeled on
phomymo's `printers.json` + auto-detect approach. Not implemented yet.

## Licensing note on protocol research

[niimbluelib](https://github.com/MultiMote/niimbluelib) / [niimblue](https://github.com/MultiMote/niimblue)
and [phomymo](https://github.com/transcriptionstream/phomymo) are MIT - safe to port code from
directly (with attribution); phomymo is the primary source for the `phomemo` family, ported close
to verbatim rather than just used as design inspiration. Several Brother references above are
**GPL-3.0** (or have no declared license) - usable only as a reference for wire-format *facts*,
never as a source to copy code from, since this project stays MIT throughout. Any GPL-adjacent
protocol family must be a clean-room reimplementation from the facts.

## Java API

```java
try (BleAdapter adapter = new BleAdapter()) {
    List<BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress("AA:BB:CC:DD:EE:FF"), 5000);
    BlePeripheral peripheral = found.get(0).getPeripheral(adapter);

    NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
    PrinterInfo info = device.connect(); // handshake + fetches model/serial/battery/etc. (Niimbot-branded printers only)
    System.out.println(info);
    device.disconnect();
}
```

`BleTransport.scanFilter()` returns a `ScanFilter` pre-set to Niimbot's own advertised GATT
service UUID, for discovering unknown Niimbot printers rather than an already-known address - it
won't find a Phomemo Q30 (which doesn't advertise that UUID, and wouldn't speak this protocol
even if it did).

Phomemo `d-series` (D30/D35/D50/D110/Q30/Q30S):

```java
try (BleAdapter adapter = new BleAdapter()) {
    List<BleDeviceResult> found = BleUtils.scan(adapter, new ScanFilter().withAddress("AA:BB:CC:DD:EE:FF"), 5000);
    BlePeripheral peripheral = found.get(0).getPeripheral(adapter);

    BleTransport transport = new BleTransport(peripheral);
    transport.connect();

    // image height (before rotation) must match the printer's physical printhead dot-width -
    // see DSeriesPrinter's javadoc; this example uses a 12mm printhead (96px) at 203 DPI
    RasterImage image = new RasterImage(myRasterBytes, /*widthBytes*/ 12, /*heightLines*/ 96);
    DSeriesPrinter.print(transport, image, /*density*/ 6, /*continuous*/ false, /*feedDots*/ 0, null);

    transport.disconnect();
}
```

## Building

```bash
mvn test
```

`common-java-utils-ble` (BLE transport), `jSerialComm` (Serial transport), `picocli` (CLI), and
`common-java-utils` (optional `IconSpecEngine` integration for the CLI's iconspec-to-print mode)
are all `provided` - a consuming application pulls in only the one(s) it actually uses on its own
runtime classpath.

`common-java-utils-ble` is currently pinned to a local `0.5.0-SNAPSHOT` install (for its unified
`ScanFilter` API) - install it first: `cd ../BSToolbox-BLE && mvn install`.

## CLI distribution

```bash
mvn package -Pdist
```

Produces `target/ptlabelprint-<version>/` (and a matching `.zip`) containing
`ptlabelprint-cli.jar`, a `lib/` folder with the CLI's runtime dependencies (BSToolbox-BLE,
jSerialComm, picocli - not `common-java-utils`, which is optional), and launch scripts
(`ptlabelprint-cli.sh` / `.bat` / `.command`).

## License

MIT, see `LICENSE` - matching niimbluelib and phomymo, the implementations this project draws on.
