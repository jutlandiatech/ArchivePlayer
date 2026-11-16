# User guide

ArchivePlayer discovers, streams and casts Internet Archive audio and video. Availability and metadata depend on the source catalog. The app is independent of the Internet Archive.

## First launch

Complete setup to choose the app language and content languages, theme, data saver and download preferences. English, German and Danish text catalogs are bundled. Interface language and content languages are separate: changing the interface language does not replace content preferences. Adult content is hidden by default; metadata/keyword filtering cannot guarantee classification.

## Discover and play

Use **Home** for category rows, continued playback and followed collections. Open an item to inspect its playable entries. Use **Search** for queries and filters such as media kind, language, decade and duration. Search can also find matching local playlists. Retry a failed load; a later-page error can occur while earlier results remain visible.

On an item, play an entry or the available list, or add entries to the queue. The mini-player leads to the full player, with seeking, queue, speed, quality, subtitles where available and a sleep timer. Video supports full-screen presentation; PiP behavior needs device verification. A failed media format triggers an attempt to use alternatives before reporting failure.

## Library and sharing

Use **Library** for playlists, likes, followed collections, downloads and history. Playlists contain Archive item/filename references rather than media bytes. A reference can stop resolving if an upstream file changes or disappears.

Share a playlist through Android's share chooser. Short enough playlists use an HTTPS link; longer ones can be shared as `.archiveplaylist` files. Import opens a preview before saving. For links that stay in the browser, see [App Links](APP_LINKS.md).

Library export/import in Settings uses Android's document picker. Exports include library references and settings, not downloaded media. Import merges library data and keeps conflicting playlists as copies; imported settings can replace current settings. Export a copy before transferring an important library.

## Download audio

Downloads are audio-only. Playlist downloads skip video and report its count. Wi-Fi-only policy can leave work waiting; a mobile-data override is available per item. Internal app storage is the default; removable storage can be selected when available. Changing location can offer to move existing downloads.

A finished local file is used when available. A missing volume/file causes playback to use streaming, which requires a network. Downloads are app-private and can be removed by uninstalling the app. A library export preserves references and settings, not those files.

## Cast flavours

The `full` build includes Chromecast controls; `foss` has no Cast integration. The receiver needs access to Archive's remote media even when the phone has a local copy. Cast subtitles use WebVTT; SRT tracks usable locally are not sent as receiver text tracks. Receiver compatibility and handover require hardware checks.

## Troubleshooting

| Symptom | Check |
|---|---|
| No or unexpected results | Simplify query, clear filters and check content languages/adult filtering. |
| Search fails | Retry later; catalog requests back off on rate limits/server errors. |
| Download waits | Check connectivity, Wi-Fi-only settings and mobile-data override. |
| Downloaded audio streams | Check that its volume is mounted and file exists. |
| Cast control absent | Check that the installed build is `full`. |
| Missing quality/subtitles | Available controls depend on source formats and receiver support. |

For build/test problems, see [maintenance](MAINTENANCE.md).
