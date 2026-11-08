# Share links, GitHub Pages and App Links

Share links look like `https://jutlandiatech.github.io/ArchivePlayer/p#v1.<payload>` (R54). The payload lives in the fragment, so it is never sent to a server.

## What is published

`.github/workflows/pages.yml` publishes on every push to `main` that touches `site/` or the category map:

| Path | Purpose |
|---|---|
| `/ArchivePlayer/` (`site/index.html`) | Landing page with download links. |
| `/ArchivePlayer/p` (`site/p/`) | Share page for people without the app (R56): decodes the fragment in the browser, lists the entries with archive.org links and fetches item titles from archive.org. |
| `/ArchivePlayer/catalog/category-map.json` | Category map the app fetches and caches (R15); copied from `app/src/main/assets/catalog/category-map.json`. |

One-time setup (repository admin): **Settings → Pages → Source: GitHub Actions**.

## Opening links directly in the app (R55)

The app declares a verified App Link for `https://jutlandiatech.github.io/ArchivePlayer/p`. Android only verifies it against `https://jutlandiatech.github.io/.well-known/assetlinks.json`, the root of the domain. A project site (`/ArchivePlayer/`) cannot serve that path, so it needs the organisation site repository `jutlandiatech/jutlandiatech.github.io` with this file at `.well-known/assetlinks.json`:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.jutlandiatech.archiveplayer",
    "sha256_cert_fingerprints": ["<SHA-256 of the signing certificate>"]
  }
}]
```

Get the fingerprint of the key that signs the APKs with:

```bash
keytool -list -v -keystore <keystore> -alias <alias> | grep SHA256
```

The APKs in `dist/` are signed with a debug key, which differs per machine. For links to open the app on everyone's phone, sign releases with one release key and list its fingerprint. Add a `.nojekyll` file to the organisation site so GitHub Pages serves the `.well-known` folder.

Until verification succeeds, Android opens share links in the browser. The share page then shows the playlist and links to the app download, and links shared from the app to other apps still work through the import preview (`ACTION_SEND`).

Check verification on a phone with:

```bash
adb shell pm verify-app-links --re-verify com.jutlandiatech.archiveplayer
adb shell pm get-app-links com.jutlandiatech.archiveplayer
```
