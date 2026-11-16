# ArchivePlayer documentation

Maintainer documentation reviewed against source on 15 November 2026. Specifications and historical test evidence retain their own dates and authority.

Open the [HTML handbook](index.html) for a styled navigation view with document search across 19 titles and offline UML rendering. Download the [four-page PDF summary](ArchivePlayer-Documentation-Summary.pdf) for an overview of the app, architecture, runtime flows and maintenance. Both can be read locally; external source links require internet access.

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
```

Run from the repository root. HTML generation requires Node.js and uses the pinned Markdown parser in `docs/assets/vendor`; reading HTML requires no build or network connection. PDF generation requires Python with `reportlab` (`python -m pip install reportlab`). The PDF is a curated summary maintained in its generator, rather than an automatic export of every specification. Update it when behavior changes, then render and inspect the PDF.

Generated `.html` files and the PDF are documentation artifacts. Edit Markdown rather than generated HTML. Styling and browser interactions live in `docs/assets/docs.css` and `docs/assets/docs.js`. Vendor versions/licenses are recorded in [assets/vendor/README.md](assets/vendor/README.md). This pass does not publish a hosted site or change the existing Pages deployment.
