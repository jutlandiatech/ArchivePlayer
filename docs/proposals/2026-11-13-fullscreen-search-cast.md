# Proposal: full-screen video, better search, Cast fix (draft for approval)

## 1. Full-screen video (new R86, UI_PLAYER_SPEC P21-P24)

- The full player shows a maximize button (`player_fullscreen_button`) on the video surface for video entries.
- The video surface uses the video's real aspect ratio (Media3 `VideoSize`), not a fixed 16:9 box.
- Maximize, video aspect >= 4:3 (width/height >= 1.33): the app switches to landscape (either landscape direction, following the sensor), hides the status and navigation bars, and shows the video edge to edge, letterboxed only if the video is wider or narrower than the screen. Controls overlay the video and hide after 3 s; a tap shows them.
- Maximize, video narrower than 4:3 (e.g. portrait or square video): full screen in portrait, same immersive behaviour.
- Tilting the phone to landscape while a >= 4:3 video plays in the full player also enters full screen (as YouTube does); tilting back to portrait leaves it, unless the user locked full screen with the button.
- Leave full screen: the minimize button (same ID, toggled state), Android Back, or tilting back to portrait. The previous orientation and system bars are restored.
- Picture-in-picture, sleep timer, subtitles and the queue keep working in full screen.
- Tests (emulator): button present only for video; maximize with a 16:9 fixture requests landscape and hides system bars; Back restores portrait; a 9:16 fixture stays portrait.

## 2. Search (R22/R24 change, new R87)

Findings (13 Nov 2026):
- Google: the Custom Search JSON API is closed to new customers and shuts down on 1 January 2027. Google's full-web API is partner-only.
- Bing Search APIs were retired in 2025.
- Brave Search API: works with `site:archive.org`, $5 per 1,000 requests with $5 free credit per month, needs an API key and attribution.
- Mojeek API: about £2 per 1,000 requests, needs a key.
- archive.org's own website search (the endpoint behind archive.org/search, `services/search/beta/page_production`, metadata backend) ranks far better than the `advancedsearch.php` query the app uses today. Same queries, top results:
  - "his girl friday": `HisGirlFriday-1940` (148k downloads) first; today's app query returns Voice of America and church podcasts first.
  - "philip marlowe": the Philip Marlowe 1947-1951 collections; today: unrelated single episodes.
  - "night of the living dead" (audio + video only): the 1968 film versions; today: podcasts and VOA.
  It is undocumented ("beta") and may change, so the app falls back to `advancedsearch.php` with title-boosted relevance when it fails.

Options:
- **A (recommended):** switch to archive.org's website search with the fallback. No key, no cost, nothing leaves archive.org, R04/R75 unchanged.
- **B:** A plus an optional "Web search (Brave)" setting where the user enters their own Brave Search API key; queries go to Brave with `site:archive.org`, results are mapped to archive.org items. Needs a change to R04/R75 (a third network host, off by default).
- **C:** a Brave/Mojeek key built into the app. Not recommended: keys in an open-source APK are public, and costs scale with users.

## 3. Cast plays nothing (R46 bug)

Likely causes found in the code (cannot be confirmed without a physical Chromecast):
- Media items are sent without a MIME type, so the Default Media Receiver gets an empty content type (Media3 logs "null MIME type ... Assuming MEDIA_TYPE_MOVIE").
- Downloaded entries are sent as `file://` URIs, which a Chromecast cannot open (R61 says cast must stream).
- SRT subtitle tracks are attached; Cast only supports WebVTT/TTML.

Fix: set the MIME type from the archive.org format (h.264 / 512Kb MPEG4 -> video/mp4, MP3 -> audio/mpeg, Ogg Vorbis -> audio/ogg), always give the receiver the https archive.org URL, send only WebVTT subtitles, and log Cast load errors. Regression tests check the converted media items. Final check needs your Chromecast.
