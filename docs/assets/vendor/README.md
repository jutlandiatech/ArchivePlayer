# Offline documentation dependencies

Pinned upstream browser distributions, included so HTML and diagrams work without a CDN:

| Library | Version | Source | License |
|---|---|---|---|
| marked | 15.0.12 | https://cdn.jsdelivr.net/npm/marked@15.0.12/lib/marked.umd.js | marked-LICENSE.md |
| Mermaid | 10.9.3 | https://cdn.jsdelivr.net/npm/mermaid@10.9.3/dist/mermaid.min.js | mermaid-LICENSE.txt |

`marked` is used at build time by `tools/build-docs.cjs`; Mermaid renders diagrams in the browser. No network request is needed to read the generated documentation. External source links still require internet access. Preserve license notices when updating these files.
