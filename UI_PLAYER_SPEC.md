# Player

Status: **approved** (gate 2), 8 November 2026. Extends the mini-player and full-player shell in UI_SHELL_SPEC.md. Implements SPECIFICATION.md R34–R45, R46–R49 (UI part), R62–R67. Reference: [interactive mockup](design/archiveplayer-mockup.html) → player.

## Mini-player

Defined in UI_SHELL_SPEC.md (above the bottom bar/at the bottom of the content area, all tabs). This spec adds: tapping the mini-player body opens the full player; swiping it down stops playback and clears the queue (with Undo snackbar `player_undo_stop`); the progress bar is not interactive.

## Full player

- Opens as a full-screen layer over the current tab; `player_collapse_button` or Android Back returns to the tab.
- **Audio entry:** large artwork (`player_artwork`), title, item title/creator (tap opens the item page and collapses the player).
- **Video entry:** 16:9 video surface (`player_video_surface`); tap toggles control visibility; landscape shows the video full-screen with overlaid controls.
- Mixed queues switch layout per entry without leaving the player (R35).
- Controls (R37): seek bar with elapsed/remaining time, skip back 10 s, previous, play/pause, next, skip forward 30 s, shuffle (on/off), repeat (off → all → one), like (current entry).
- Secondary row: Queue, Quality, Subtitles (shown only when the entry has subtitle files), Speed, Sleep timer, Cast (`full` only).
- Previous restarts the current entry when more than 3 s have played, otherwise goes to the previous entry.

## Sheets

| Sheet | Content | Behaviour |
|---|---|---|
| Queue `sheet_queue` | Now playing, then upcoming entries | Drag handles reorder; swipe or ⋯ → Remove; tap jumps to the entry; "Clear upcoming" |
| Quality `sheet_quality` | "Auto ({chosen})" plus each playable format of the entry (R42) | Choice applies to the current entry only; FLAC listed but never auto-chosen (R40) |
| Subtitles `sheet_subtitles` | Off (default) + one option per `.srt`/`.vtt` file with language label; machine-generated tracks suffixed "Auto-generated" (R45) | Choice applies to the current entry |
| Speed `sheet_speed` | 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0 | Applies to the session until changed |
| Sleep timer `sheet_sleep` | Off, 15, 30, 45, 60 min, End of current entry | Active timer shows remaining time on the button; pauses (does not stop) at expiry |

## Errors (R43)

On a playback error the player first falls back silently to the next suitable format once. If that also fails it shows `player_error` with the catalog message and `player_retry_button`; Retry restarts at the same position. Network loss pauses and auto-resumes when the network returns within 30 s.

## Background, notification, PiP

- Media3 `MediaSessionService` (R34): notification and lock-screen controls; audio focus loss pauses; becoming-noisy pauses.
- Leaving the app during video playback enters PiP (R38) with play/pause, skip back, skip forward actions; without PiP (disabled by user) the audio continues (R39).
- While casting (`full`): the player shows "Casting to {device}" over the artwork, PiP is not offered, local subtitles hide when the receiver cannot show them (R49).

## Text catalog contract

| Key | English |
|---|---|
| `player.playing` / `player.paused` | Playing / Paused (state descriptions) |
| `player.skipBack` / `player.skipForward` | Back 10 seconds / Forward 30 seconds |
| `player.previous` / `player.next` | Previous / Next |
| `player.shuffle.on` / `.off` | Shuffle on / Shuffle off |
| `player.repeat.off` / `.all` / `.one` | Repeat off / Repeat all / Repeat one |
| `player.queue` / `player.queue.clear` | Queue / Clear upcoming |
| `player.quality` / `player.quality.auto` | Quality / Auto ({format}) |
| `player.subtitles` / `player.subtitles.off` / `player.subtitles.auto` | Subtitles / Off / Auto-generated |
| `player.speed` | Speed |
| `player.sleep` / `player.sleep.endOfEntry` / `player.sleep.minutes` / `player.sleep.remaining` | Sleep timer / End of current entry / {minutes} min / {remaining} left |
| `player.error` | Playback failed. |
| `player.castingTo` | Casting to {device} |
| `player.stopped.undo` | Playback stopped. Undo |

## Element IDs

Shell IDs from UI_SHELL_SPEC.md remain. Additional IDs:

| ID | Element |
|---|---|
| `player_title` / `player_subtitle` | Entry title / item + creator line |
| `player_elapsed` / `player_remaining` | Time labels |
| `player_state` | Invisible node whose state description is `playing`, `paused`, `buffering` or `ended` (test hook, also read by TalkBack) |
| `player_casting_label` | "Casting to …" (`full`) |
| `player_sleep_remaining` | Remaining-time label on the sleep button |
| `player_undo_stop` | Undo snackbar after swiping the mini-player away |
| `sheet_queue` / `queue_row_<index>` / `queue_drag_<index>` / `queue_remove_<index>` / `queue_clear_button` | Queue sheet |
| `sheet_quality` / `quality_option_auto` / `quality_option_<formatKey>` | Quality; `<formatKey>` lower snake case of the archive.org format, e.g. `vbr_mp3`, `h_264`, `512kb_mpeg4`, `ogg_vorbis`, `flac` |
| `sheet_subtitles` / `subtitle_option_off` / `subtitle_option_<index>` | Subtitles |
| `sheet_speed` / `speed_option_<value>` | Speed; `<value>` = `0_5`, `0_75`, `1_0`, `1_25`, `1_5`, `1_75`, `2_0` |
| `sheet_sleep` / `sleep_option_<off\|15\|30\|45\|60\|end>` | Sleep timer |

Shuffle and repeat expose their mode as a state description (`on`/`off`; `off`/`all`/`one`).

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| P01 | Play an audio entry from the item page: mini-player visible on all tabs with the title; tapping it opens `player_screen` with `player_artwork`; collapse returns to the same tab. |
| P02 | Play/pause on mini-player and full player toggle `player_state` between `playing` and `paused`. |
| P03 | Skip forward then back: position +30 s then −10 s (± 1 s). |
| P04 | Next/previous move through the queue; previous after > 3 s restarts the entry. |
| P05 | Repeat cycles off → all → one; shuffle toggles; state descriptions follow. |
| P06 | Speed 1.5×: `speed_option_1_5` selected; player speed 1.5 (read via MediaController in test). |
| P07 | Sleep timer "End of current entry" (A34): playback pauses when the entry ends; next entry not started. |
| P08 | Sleep timer 15 min with a fake clock: remaining label counts down; at 0 playback pauses. |
| P09 | Video entry offering h.264 and 512Kb MPEG4, data saver on, metered (A13): `quality_option_auto` labelled with 512Kb MPEG4; both listed; choosing h.264 applies to this entry only. |
| P10 | Audio entry with VBR MP3, 128 kbps MP3, Ogg, FLAC (A15): Auto = VBR MP3; FLAC listed. |
| P11 | Mixed queue audio → video → audio (A16): `player_artwork`, then `player_video_surface`, then `player_artwork`; order kept. |
| P12 | Queue sheet: drag `queue_drag_2` above row 1 reorders; `queue_remove_1` removes; tap a row jumps to it. |
| P13 | Entry with `.srt` and an ASR `.vtt` (A33): `sheet_subtitles` offers Off (selected), the language-labelled track and an "Auto-generated" track. |
| P14 | Fixture server returns 404 for the chosen format: player falls back to the next format without showing an error; when all fail, `player_error` and Retry shown. |
| P15 | Stop at 40:00 of a 90-min fixture video (A17), relaunch app: Home Continue row resumes at 40:00. |
| P16 | Play video and press Home (A32): activity enters picture-in-picture (`isInPictureInPictureMode`). |
| P17 | Media session (R34): a `MediaController` connected to the app's session sees the queue and can pause/resume playback. |
| P18 | Swipe the mini-player down: playback stops, mini-player hidden, Undo restores queue and position. |
| P19 | `full`: `player_cast_button` present; `foss`: absent, no `player_casting_label` (N13). |
| P20 | TalkBack labels: every player control has a non-empty content description from the text catalog in all three languages; touch targets ≥ 48 dp (R64). |
