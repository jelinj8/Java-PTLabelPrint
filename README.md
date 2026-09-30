# PtLabelPrint

Java client library and CLI for Bluetooth (BLE, or classic via a serial port) label printers across manufacturers - a printer abstraction
layer over several wire-protocol families, so callers detect, connect to, and print on a printer
through one API regardless of brand (see "Printer abstraction layer" below for what's unified vs.
kept family-specific).

Three protocol families are implemented and **confirmed printing on real hardware**:

| Family | Confirmed on | Also covered, untested |
|---|---|---|
| `niimbot` | Niimbot D11_H, Niimbot M2 (M2_H) | the other models niimbluelib has a print task for |
| Phomemo `d-series` | Phomemo Q30 | D30, D35, D50, Q30S |
| Phomemo `m110` | Phomemo M421 | M110, M120 |

## Status

### Niimbot

Confirmed against a real **D11_H** and a real **M2 (M2_H)**, both info queries and printing; no
OS-level pairing needed for either.

- `info`: connect handshake, model ID (resolved via `PrinterModels`), serial number, battery, label
  type, firmware versions, and DPI class all came back correct.
- `media`: live state (lid, paper, ribbon, battery, head temperature) and the loaded consumables'
  RFID tag data. The M2 is thermal-transfer, so it reports a ribbon tag separately from the paper
  tag, each with its own counters.
- Printing: the D11_H prints through `D110V4PrintTask`, the M2 through `B1PrintTask`. The D11_H's
  mandatory 90° rotation (its printhead runs along the label) is confirmed correct in direction with
  an asymmetric test pattern, not just a solid block.

The full niimbluelib catalog is ported - metadata for all 77 models and all 7 print tasks,
dispatched by model and protocol version (`NiimbotPrintTasks`). Only the two print tasks above are
hardware-confirmed; the other five are ported from source and untested. Most of the 77 models have
no print task in niimbluelib either, and this project doesn't invent one.

Also ported: sound settings, label positioning calibration, setting the printer clock, and firmware
upgrade. **Firmware upgrade has never been run against real hardware** and can permanently brick a
printer - the CLI command requires an explicit `--confirm-firmware-risk` flag.

Unlike Phomemo's `d-series`, the Niimbot protocol doesn't require the image width to exactly match
the printhead's physical capacity.

### Phomemo `d-series`

Confirmed printing on a real **Q30**. The print flow is model-generic, so D30/D35/D50/Q30S are
expected to work through the same code path, but none of them has been tested.

- The image's pre-rotation height must equal the printhead's physical dot-width (96px / 12mm on the
  Q30). `DSeriesLabelSizes` holds phomymo's own label-size presets; an arbitrary height produces a
  garbled print rather than an error.
- **Die-cut labels longer than about 80mm are silently ignored by the Q30** - the job transfers
  without error and nothing prints. The same length prints fine on continuous media, so use
  continuous media for long labels. This looks like a firmware/gap-sensor limit, not a bug here.
- The Q30 does **not** speak the Niimbot protocol, despite an early assumption in this project that
  it did - see "Protocol families".

### Phomemo `m110` (M421)

Confirmed printing on a real **Phomemo M421** (a 4"-class, 203 DPI label printer) with 40x20mm gap
labels - right way up, not shifted, not cropped - both through `M110Printer` directly and through
the unified `LabelPrinter` API. M110/M120 share the same code path but are untested.

Things to know about the M421:

- **It must be paired with the OS first.** Unlike the other printers here, it rejects notification
  subscription on an unbonded link ("attribute requires authentication"), so connecting fails until
  it is paired - on Windows, accept the system pairing prompt. `raw --pair` attempts programmatic
  pairing, but that did not succeed on its own against this printer.
- **Size the image to the label, not the printhead** (8 px/mm). Rows are sent at the image's own
  width; the printhead (~100mm) is much wider than most of its media, which is why `print-test`
  refuses to print its default printhead-sized square on this family.
- **Large labels are slow over BLE; use the serial port.** The printer takes data at only about
  7 KB/s over BLE, so a 100x150mm label takes 18s or more and visibly slows down while printing.
  Phomemo's own app uses classic Bluetooth instead. Pair the printer with the OS as a classic
  Bluetooth device, find the serial port that creates (`ports`), and print through that:
  `print-test --serial COM31 --model M421 --pattern-mm 100x150` took about 11s.
- **It aligns media to the left.** The image starts at the first printhead dot, but a label often
  sits a little in from the edge of its backing paper (about 2.5mm on a 102mm stock tested, against 1mm on ordinary stock), which puts
  the print that far too far left. The printer itself starts about 1mm in, so ordinary stock
  prints in place; for a wider gap, `PrintJob#setMediaSideGapMm` (CLI `--side-gap-mm`) moves the
  image that many extra millimetres in (default: none). It is honoured by this family only. `setTopOffsetMm` (`--top-offset-mm`, positive = later,
  negative = earlier) does the same along the feed direction, for stock on which the print starts
  slightly early or late.
- **Small labels stop short of the tear edge.** With 40x20mm labels the print sits slightly low,
  the label's bottom edge stays under the tear edge, and the feed button advances two labels per
  press. This is the printer's own behaviour - it persists after the printer's calibration and when
  printing from the official app - not something this library controls. Larger labels are untested.

### Not implemented

- Dithering or scaling: the image pipeline converts to black/white with a fixed threshold and pads
  or crops to fit - it never scales.
- Phomemo's other sub-protocols (`m02`, `m04`, generic `m-series`, `p12`, `tspl`). Their models are
  cataloged, so such a printer is identified correctly, but it can't print.
- USB. The M421's USB port is a printer-class device with its own OS print queue, not a serial
  port; printing over USB is left to the OS print system.
- Zebra and Brother families (planned, see "Protocol families").

## CLI

```bash
mvn package -Pdist
cd target/ptlabelprint-<version>/
```

Any family, through the abstraction layer:

```bash
./ptlabelprint-cli.sh discover                 # unfiltered scan + detected family per device
./ptlabelprint-cli.sh connect <address>        # auto-detect the family and connect
./ptlabelprint-cli.sh ports                     # serial ports, incl. paired classic-Bluetooth ones
./ptlabelprint-cli.sh print-test --serial <port> --model <name> ...
                                               # the same print-test over a serial port instead of BLE
./ptlabelprint-cli.sh print-test <address> [--image=<file> | --pattern-mm=40x20]
                                               # [--copies] [--continuous] [--density]
                                               # [--rotation=auto|none|90|180|270]
                                               # USES REAL CONSUMABLES. --pattern-mm prints a generated
                                               # alignment pattern of that size at the printer's DPI
```

Niimbot:

```bash
./ptlabelprint-cli.sh info <address>                 # model, serial, battery, firmware, ...
./ptlabelprint-cli.sh media <address>                # live state + loaded-roll RFID info + sound settings
./ptlabelprint-cli.sh niimbot-print-test <address>   # full-width test rectangle - USES REAL CONSUMABLES
./ptlabelprint-cli.sh niimbot-calibrate <address>    # label positioning calibration - USES REAL
                                                     # CONSUMABLES (ejects ~15cm of paper)
./ptlabelprint-cli.sh niimbot-set-time <address>     # set the printer's real-time clock
./ptlabelprint-cli.sh niimbot-firmware-upgrade <address> <file> <version> --confirm-firmware-risk
                                                     # HIGH RISK - can brick the printer; never tested
./ptlabelprint-cli.sh scan                           # printers advertising Niimbot's service UUID -
                                                     # real ones often don't, use 'discover' instead
```

Phomemo:

```bash
./ptlabelprint-cli.sh phomemo-print-test <address> [--label=12x12]
                                               # d-series test square - USES REAL CONSUMABLES
./ptlabelprint-cli.sh phomemo-m110-print-test <address> [--width-mm=40] [--height-mm=20]
                                               # m110 alignment pattern - USES REAL CONSUMABLES
```

Protocol-agnostic diagnostics, for bringing up an unfamiliar device:

```bash
./ptlabelprint-cli.sh gatt <address>           # dump GATT services/characteristics
./ptlabelprint-cli.sh raw <address> <hex>      # write raw hex, print whatever comes back
                                               # (--pair[=PIN], --no-subscribe, --read, --with-response, ...)
```

### Using a remote BLE bridge

Any command can run through a remote BLE bridge - e.g. an ESP32 running
`firmware-BSBleRemoteBridge`, or another machine running BSToolbox-BLE's `--remote` client - instead
of this machine's own Bluetooth adapter. The bridge dials in to the CLI, so the CLI listens:

```bash
export PTLABELPRINT_BRIDGE_TOKEN=<token the bridge is provisioned with>
./ptlabelprint-cli.sh discover --bridge-port 8765
./ptlabelprint-cli.sh print-test <address> --pattern-mm 40x20 --bridge-port 8765 [--bridge-name <channel>]
```

Point the bridge's server URL at `ws://<this host>:8765/ble-remote` (plain `ws://`, no TLS). Each
command waits for the bridge to connect (`--bridge-wait`, default 60s), runs, and disconnects.
A token is required, and it must match: the ESP32 firmware stops retrying after a rejected token
until it is restarted. `--bridge-token` also works, but the environment variable keeps the token
out of the process list. Verified against a real ESP32 bridge for scanning and
connecting (the M421 connected through it with no pairing step). Printing through it needs the
printer to finish before the link drops, which the `m110` flow now waits for.

**No tested printer advertises the Niimbot service UUID that `scan` filters on** - not the Q30, and
not a genuine D11_H either. `discover` finds printers by advertised name instead.

## Printer abstraction layer

`cz.bliksoft.ptlabelprint.printer` detects a printer's protocol family from its BLE advertised name
(`PrinterCatalog`, longest-prefix-wins, mirroring phomymo's own `detectPrinterConfig`), builds the
matching `LabelPrinter` (`PrinterFactory`), and gives every family the same connect lifecycle and
the same print call. Detect-and-connect is confirmed on the D11_H, the M2, and the M421; the unified
print call is confirmed on the D11_H and the M421.

**What's unified** - `LabelPrinter#print(BufferedImage, PrintJob)` and `getCapabilities()`:

- The image is a standard `java.awt.image.BufferedImage`; each family converts it to its own wire
  format internally. Conversion to black/white is a fixed threshold, and fitting is pad/crop, never
  scaling.
- `PrintJob` carries copies, continuous-vs-gapped media, density, and rotation.
- `getCapabilities()` reports DPI, printhead width in pixels, and the density range.
- Density is passed through in the connected family's own scale, not normalized across families,
  and validated against that family's range.

**Rotation.** Each family's own mandatory orientation is always applied (Phomemo `d-series`: always
90°; Niimbot: 90° for models whose printhead runs along the label, e.g. the D11_H; `m110`: none).
On top of that, `Rotation.NONE` - the default - adds nothing; an explicit `CW_90`/`CW_180`/`CW_270`
pre-rotates the image; `Rotation.AUTO` tries one extra 90° turn if the image doesn't fit the
printhead. `AUTO` is opt-in because on gapped media rotating doesn't remove an overage, it moves it
onto the equally fixed feed axis - it is only safe on continuous media.

**What stays family-specific.** The families genuinely differ, so the common surface is deliberately
small. Niimbot has a rich info-query catalog (model, battery, serial, RFID) that Phomemo has no
equivalent for; Niimbot also has page-colour and tube/half-cut options. Reach those through
`NiimbotLabelPrinter#getDevice()`, or the low-level `print(RasterImage, ...)` calls on the Phomemo
printers.

`PrinterFactory` throws `UnimplementedPrinterFamilyException` for a printer it recognizes but can't
print on (the cataloged-only Phomemo sub-protocols), which is distinct from "not recognized".

## Protocol families

Not every printer sold under one brand speaks the same wire protocol, and similar model names across
brands mean nothing about compatibility.

- **niimbot** - Niimbot-branded printers only. Ported from
  [niimbluelib](https://github.com/MultiMote/niimbluelib) (MIT). Request/response framing
  (`0x55 0x55 ... 0xAA 0xAA`). `cz.bliksoft.ptlabelprint.protocol.niimbot`.
- **phomemo** - Phomemo's own lineup, which is itself several distinct sub-protocols, ported from
  [phomymo](https://github.com/transcriptionstream/phomymo) (MIT).
  `cz.bliksoft.ptlabelprint.protocol.phomemo`. The two implemented ones share one BLE channel:
  service `0xff00`, write `0xff02`, notify `0xff03`.
  - `d-series` (Q30/Q30S/D30/D35/D50) - **implemented, confirmed on a Q30**
    (`RasterImage`/`DSeriesCommands`/`DSeriesPrinter`). ESC/POS-derived, one-way (no responses to
    wait for), rotated raster printing.
  - `m110` (M421; M110/M120 untested) - **implemented, confirmed on an M421**
    (`M110Commands`/`M110Printer`). Not rotated, rows sent at the label's own width. phomymo doesn't
    list the M421; that it speaks this protocol was established from the printer's own replies to
    Phomemo status queries plus [phomemo-tools](https://github.com/vivier/phomemo-tools)' M421
    driver definition (GPL-3.0 - used for wire-format facts only, see the licensing note below).
  - `m02`/`m04`/generic `m-series`/`p12` - cataloged for detection, not implemented.
  - `tspl` (PM-241) - a text-based protocol, generic enough to cover non-Phomemo TSPL printers too.
    Cataloged, not implemented.
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

**The Phomemo Q30 does not speak the Niimbot protocol.** An earlier version of this project read
"similar to D30" in phomymo's README as Niimbot compatibility; live testing plus phomymo's actual
source showed it speaks the unrelated `d-series` protocol. Getting `d-series` working then took two
more real-hardware issues: a garbled print from an arbitrary test-image size (see the printhead
dot-width rule above), and a striped print that turned out to be an unreliable power source on the
test rig, not the code. CLAUDE.md's "Debugging history" has the full account - useful precedent
before bringing up another device.

## Licensing note on protocol research

[niimbluelib](https://github.com/MultiMote/niimbluelib) / [niimblue](https://github.com/MultiMote/niimblue)
and [phomymo](https://github.com/transcriptionstream/phomymo) are MIT - safe to port code from
directly (with attribution); phomymo is the primary source for the `phomemo` family, ported close
to verbatim rather than just used as design inspiration.
[vivier/phomemo-tools](https://github.com/vivier/phomemo-tools) and several Brother references above
are **GPL-3.0** (or have no declared license) - usable only as a reference for wire-format *facts*,
never as a source to copy code from, since this project stays MIT throughout. Any GPL-adjacent
protocol family must be a clean-room reimplementation from the facts.

## Java API

Any detected family, through the abstraction layer (shown for a Phomemo M421 with 40x20mm labels -
the image is sized to the label at 8 px/mm):

```java
try (BleAdapter adapter = new BleAdapter()) {
    // requireName(): family detection needs the advertised name, which can arrive after the first packet
    List<BleDeviceResult> found = BleUtils.scan(adapter,
            new ScanFilter().withAddress("AA:BB:CC:DD:EE:FF").requireName(), 5000);
    BleDeviceResult result = found.get(0);

    PrinterDefinition definition = PrinterCatalog.detectUnambiguous(result.getName())
            .orElseThrow(() -> new IllegalStateException("Unknown printer: " + result.getName()));
    try (LabelPrinter printer = PrinterFactory.create(definition, new BleTransport(result.getPeripheral(adapter)))) {
        printer.connect(); // the M421 must already be paired with the OS
        System.out.println(printer.getCapabilities());

        BufferedImage label = new BufferedImage(320, 160, BufferedImage.TYPE_INT_RGB); // 40x20mm
        // ... draw the label ...
        printer.print(label, new PrintJob().setCopies(1));
    }
}
```

Niimbot's own API, for what the common surface doesn't expose (printer info, RFID, heartbeat, ...):

```java
NiimbotDevice device = new NiimbotDevice(new BleTransport(peripheral));
PrinterInfo info = device.connect(); // handshake + fetches model/serial/battery/etc.
System.out.println(info);
device.disconnect();
```

Phomemo `d-series`, low-level:

```java
BleTransport transport = new BleTransport(peripheral);
transport.connect();

// image height (before rotation) must match the printer's physical printhead dot-width -
// see DSeriesPrinter's javadoc; this example uses a 12mm printhead (96px) at 203 DPI
RasterImage image = new RasterImage(myRasterBytes, /*widthBytes*/ 12, /*heightLines*/ 96);
DSeriesPrinter.print(transport, image, /*density*/ 6, /*continuous*/ false, /*feedDots*/ 0, null);

transport.disconnect();
```

`BleTransport.scanFilter()` returns a `ScanFilter` pre-set to Niimbot's own advertised GATT service
UUID - of limited use in practice, since real printers often don't advertise it.

## Building

```bash
mvn test              # compile + run the test suite (no real hardware involved)
mvn package -Pdist    # standalone CLI distribution
```

`common-java-utils-ble` (BLE transport), `jSerialComm` (Serial transport), `picocli`
(CLI), and `common-java-utils` are all `provided` - a consuming application pulls in only the one(s)
it actually uses on its own runtime classpath. The build is against `common-java-utils-ble` 0.9.0.

`mvn package -Pdist` produces `target/ptlabelprint-<version>/` (and a matching `.zip`) containing
`ptlabelprint-cli.jar`, a `lib/` folder with the CLI's runtime dependencies, and launch scripts
(`ptlabelprint-cli.sh` / `.bat` / `.command`).

## License

MIT, see `LICENSE` - matching niimbluelib and phomymo, the implementations this project draws on.
