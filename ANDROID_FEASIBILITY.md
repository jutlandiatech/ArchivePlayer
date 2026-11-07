# Feasibility findings and proposed technical design

Findings gathered November 2026 while writing the specification. Counts are time-dependent. These are findings and proposals; the approved behaviour is in SPECIFICATION.md.

## archive.org

- Size: roughly 17.6M `movies`, 14.3M `audio` and 0.3M `etree` items.
- Metadata: `subject` is free text and noisy, `genre` is sparse; curated collections (e.g. `Film_Noir`, `Comedy_Films`, `SciFi_Horror`, `silent_films`, classic TV by decade, `etree`, `GratefulDead`, `oldtimeradio`, `podcasts`, `audio_music`, `78rpm`, `netlabels`) are the cleanest taxonomy → hand-curated category map (R14–R17).
- `language` mixes codes and names (`fre`, `French`, `fr`, `Français`) and is missing on many items → normalization in `core`, untagged excluded only while a language filter is active (R28).
- Playable files are progressive derivatives (h.264 MP4, "512Kb MPEG4", VBR MP3, Ogg Vorbis, FLAC). Some items have no playable derivative → filtered (R09).
- Download URLs redirect to `dn*.ca.archive.org`, support HTTP byte ranges and send `Access-Control-Allow-Origin: *` → seeking, Cast and the GitHub Pages share page work without a proxy.
- About 5.8M videos (and many audio items) carry `.srt`/`.vtt` files, many machine-generated → offered, labelled "Auto-generated" (R45).
- The `no-preview` collection (about 60k items) is the main adult-content signal → plus a remote keyword list (R70).

## Playback and Cast

- Media3 `CastPlayer` with a local ExoPlayer hands playback over between phone and Chromecast; the scaffold already does this with a sample stream.
- The default Cast receiver plays MP4/h.264 and MP3; Ogg is not sent to Cast, MP3 is used instead (R40).
- The Cast SDK is proprietary (Google Play Services) → `foss` flavour without Cast for F-Droid (R80, R81).

## Storage

- Media3 `DownloadService` for background, resumable audio downloads (R60).
- `Context.getExternalFilesDirs` returns app-specific folders on internal storage and mounted SD cards without a storage permission; files are removed on uninstall (R85).

## Validation gates (before release)

1. Every category-map query returns enough playable items (live check).
2. Format choice plays on a real phone and a real Chromecast for video and audio samples, including multi-file albums.
3. `foss` APK contains no Play Services classes.
4. Downloads to SD card survive app restart, card removal and reinsertion.
5. Search latency targets (R77) on a typical 4G connection.
