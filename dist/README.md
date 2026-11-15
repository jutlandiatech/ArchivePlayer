# APKs

Tested builds of ArchivePlayer.

| File | Flavour | Notes |
|---|---|---|
| `ArchivePlayer-full-0.2.0.apk` | `full` | Includes Chromecast (needs Google Play Services on the phone). |
| `ArchivePlayer-foss-0.2.0.apk` | `foss` | No Google Play Services, Cast, MediaRouter or Firebase. |

Both: package `com.jutlandiatech.archiveplayer`, version 0.2.0 (code 2), Android 8.0 (API 26) and newer, target SDK 37. Built from the commit recorded in [docs/tdd/green-v0.2.0.json](../docs/tdd/green-v0.2.0.json), after the test runs listed in [TDD_EVIDENCE.md](../TDD_EVIDENCE.md).

`SHA256SUMS.txt` lists the checksums (`sha256sum -c SHA256SUMS.txt`). Builds are signed with an Android debug key (certificate SHA-256 `81:E0:DA:E5:2E:60:BD:29:69:8E:FF:59:82:24:3F:DA:50:2C:18:6E:A7:E1:CC:32:8D:41:31:05:BE:FE:B2:07`), not a release key. The two flavours share the package name, so install one or the other; switching flavours or installing a build signed with another key needs an uninstall first (export your library in Settings before that).

Install on a phone with USB debugging enabled:

```bash
adb install -r dist/ArchivePlayer-full-0.2.0.apk
```

Or copy the file to the phone and open it (allow "Install unknown apps" for your file manager).

Rebuild them yourself with `./gradlew :app:assembleFullDebug :app:assembleFossDebug` (see [docs/GETTING_STARTED.md](../docs/GETTING_STARTED.md)).
