# Design references

- [archiveplayer-mockup.html](archiveplayer-mockup.html) — interactive, self-contained mockup of setup, Home, Search/Browse, item page, player, Library and Settings. The side panel switches Nordic light/noir, English/German/Danish, phone/tablet, `full`/`foss`, SD card present/absent, and shows the stable element IDs used by tests. Data is static sample data; nothing is fetched.
- [logo/](logo/) — "Signal A" logo (SPECIFICATION.md R84, D29): `archiveplayer-launcher.svg` (white glyph on `#2769AD`), `archiveplayer-logo.svg` (blue in-app glyph), `archiveplayer-monochrome.svg` (Android 13+ themed icon), and `logo-concepts.html` with all five concepts that were compared.

The mockup establishes layout, wording and IDs. Its implementation shortcuts (simulated playback, Cast, downloads) are not product behaviour; screenshots of the real app go in `design/native-*` folders once it exists.
