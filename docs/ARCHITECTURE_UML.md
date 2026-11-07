# Application architecture (proposed)

Proposed alongside specification v0.2; becomes binding with the implementation gate and is updated as code lands.

## Modules and boundaries

```mermaid
flowchart LR
  subgraph core["core (Kotlin/JVM, no Android)"]
    model[Catalog model]
    query[Query builder]
    lang[Language normalizer]
    filter[Adult filter]
    fmt[Format and quality selection]
    pl[Playlist and library codec]
    rec[Recommendations]
    pol[Settings, resume, sleep timer, download-location policies]
  end
  subgraph app["app (Android)"]
    ui[ui: Compose screens, theme, AppIds]
    setup[setup: first-launch flow, TextCatalog]
    playback[playback: Media3 session, player]
    data[data: archive.org client, Room, DataStore, category map]
    platform[platform: downloads, storage volumes, backup]
    cast["cast (full flavour only)"]
  end
  ui --> core
  setup --> core
  playback --> core
  data --> core
  platform --> core
  playback --> cast
  data -->|HTTPS| archive[(archive.org)]
  data -->|HTTPS| pages[(GitHub Pages: category map, keyword list, share page)]
```

`core` takes plain inputs (parsed responses, settings, timestamps) and returns decisions, so it is tested on the JVM without an emulator. Android adapters stay thin.

## Flavours

| Source set | Contents |
|---|---|
| `app/src/main` | Everything shared; playback through an interface that does not reference Cast |
| `app/src/full` | Cast integration (Media3 Cast, `MediaRouteButton`, options provider) |
| `app/src/foss` | No-op Cast bindings; no Google dependencies |

## Main flows

```mermaid
sequenceDiagram
  participant U as User
  participant UI as ui
  participant D as data
  participant C as core
  participant P as playback
  U->>UI: open item
  UI->>D: metadata(identifier)
  D-->>UI: files
  UI->>C: chooseFormat(files, device, castActive, dataSaver)
  C-->>UI: entry list with chosen files
  U->>UI: Play all
  UI->>P: setQueue(entries)
  P-->>U: local playback or Cast
```
