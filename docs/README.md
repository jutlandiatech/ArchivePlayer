# ArchivePlayer documentation

Maintainer documentation reviewed against source on 15 November 2026. Specifications and historical test evidence retain their own dates and authority.

Open the [HTML handbook](index.html) for a styled navigation view with document search across 19 titles and offline UML rendering. The [full PDF handbook](ArchivePlayer-Full-Handbook.pdf) contains all 19 documents, linked contents, chapter bookmarks and seven rendered UML diagrams. The [four-page PDF summary](ArchivePlayer-Documentation-Summary.pdf) provides a shorter overview. All editions can be read locally; external source links require internet access.

| Need | Document |
|---|---|
| Set up tools, build and launch | [Getting started](GETTING_STARTED.md) |
| Use the app | [User guide](USER_GUIDE.md) |
| Understand components, classes, flows and deployment | [Architecture and UML](ARCHITECTURE_UML.md) |
| Understand persistence and external boundaries | [Data and integrations](DATA_AND_INTEGRATIONS.md) |
| Change and verify the project | [Maintenance guide](MAINTENANCE.md) |
| Read agreed requirements | [Specification](../SPECIFICATION.md) and root `UI_*_SPEC.md` documents |
| Find test commands and traceability | [Testing](../TESTING.md) |
| Inspect recorded outcomes | [TDD evidence](../TDD_EVIDENCE.md) and [evidence format](tdd/README.md) |
| Configure playlist links | [App Links](APP_LINKS.md) |
| Check historical toolchain observations | [Development environment](../DEVELOPMENT_ENVIRONMENT.md) |
| Inspect design references | [Design assets](../design/README.md) |

The source build declares version `1.0.0` / code `3`; packaged APKs and older test evidence can describe earlier versions. Historical test counts are not a fresh verification of this checkout. Cast receivers, removable storage, PiP, backup, tablet layouts and performance still require the checks described in the testing documents.

The [sim-warning-app documentation](https://github.com/jutlandiatech/sim-warning-app/tree/main/docs) informed the source-linked UML and generated-PDF approach. Its repository was accessible on 16 November 2026. ArchivePlayer adds a local HTML handbook. Microsoft 365 is not required to maintain it.

## Regenerate the HTML and PDF

```powershell
node tools/build-docs.cjs
python tools/export-docs-summary.py
.\tools\export-docs-handbook.ps1
```

Run from the repository root. HTML generation requires Node.js and uses the pinned Markdown parser in `docs/assets/vendor`; reading HTML requires no build or network connection. PDF generation requires Python with `reportlab` (`python -m pip install reportlab`). The PDF is a curated summary maintained in its generator, rather than an automatic export of every specification. Update it when behavior changes, then render and inspect the PDF.

The full handbook is a source-driven export of the same 19 documents as the HTML edition. Its Windows export script requires Node.js/npm, Microsoft Edge and Python with `reportlab` and `Pillow` (`python -m pip install reportlab Pillow`). Pass `-Python` or `-Edge` to select executables. The first run installs the locked Mermaid CLI dependencies under ignored `tmp/docs-render`; later runs reuse them. Diagram rendering uses Edge headlessly without a CDN. Class-diagram direction is adjusted for landscape print pages; relationships and labels stay unchanged. Intermediate files live under ignored `tmp/pdfs`. The PDF uses embedded Arial and Consolas from Windows Fonts. Inspect the rendered PDF after regeneration.

Generated `.html` files and the PDF are documentation artifacts. Edit Markdown rather than generated HTML. Styling and browser interactions live in `docs/assets/docs.css` and `docs/assets/docs.js`. Vendor versions/licenses are recorded in [assets/vendor/README.md](assets/vendor/README.md). This pass does not publish a hosted site or change the existing Pages deployment.
