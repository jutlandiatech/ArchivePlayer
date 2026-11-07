# TDD evidence files

Machine-readable records of actual test runs, referenced from [TDD_EVIDENCE.md](../../TDD_EVIDENCE.md):

- `red-<slice>.json` — run after writing tests and an API skeleton, before implementation: command, timestamp, per-suite counts, failure types, SHA-256 of each test source.
- `green-<slice>.json` — the same suite after implementation; test-source hashes must match the red record (tests not weakened).
- `*.txt` — raw console excerpts for Android/instrumentation runs.

No runs recorded yet.
