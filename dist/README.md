# APKs

Tested builds of ArchivePlayer.

| File | Flavour | Notes |
|---|---|---|
| `ArchivePlayer-full-<version>.apk` | `full` | Includes Chromecast (needs Google Play Services on the phone). |
| `ArchivePlayer-foss-<version>.apk` | `foss` | No Google Play Services, Cast, MediaRouter or Firebase. |

`SHA256SUMS.txt` lists the checksums. Builds are signed with the debug key (not a release key), so they install side by side with nothing else but cannot update a Play Store build.

Install on a phone with USB debugging enabled:

```bash
adb install -r dist/ArchivePlayer-full-<version>.apk
```

Or copy the file to the phone and open it (allow "Install unknown apps" for your file manager).

Rebuild them yourself with `./gradlew :app:assembleFullDebug :app:assembleFossDebug` (see [docs/GETTING_STARTED.md](../docs/GETTING_STARTED.md)).
