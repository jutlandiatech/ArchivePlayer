#!/usr/bin/env python3
"""Generates the synthetic archive.org fixtures served to Android UI tests by FixtureServer.

Output: app/src/androidTest/assets/fixtures/{metadata,search,media}. Media files need ffmpeg
(libmp3lame, libvorbis, libx264). Run from the repository root: python3 tools/gen_ui_fixtures.py
"""
import json, os, shutil, subprocess

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "app/src/androidTest/assets/fixtures")
CORE = os.path.join(ROOT, "core/src/test/resources/fixtures/archive")
LENGTH = 180  # every served media file is 3 minutes long


def f(name, fmt, source="derivative", **extra):
    d = {"name": name, "format": fmt, "source": source, "size": "1000"}
    d.update({k: str(v) for k, v in extra.items()})
    return d


def audio_entry(ident, n, title, rich=False):
    base = f"{ident}_e{n:02d}"
    files = [f(f"{base}.mp3", "VBR MP3", "original" if not rich else "derivative", track=n, title=title, length=LENGTH)]
    if rich:
        files += [f(f"{base}.flac", "Flac", "original", track=n, title=title, length=LENGTH),
                  f(f"{base}_128kb.mp3", "128Kbps MP3", track=n, title=title, length=LENGTH),
                  f(f"{base}.ogg", "Ogg Vorbis", track=n, title=title, length=LENGTH)]
    return files


def item(ident, title, mediatype, files, creator=None, date=None, language=None, collection=None, subject=None, description=None):
    md = {"identifier": ident, "title": title, "mediatype": mediatype}
    for k, v in [("creator", creator), ("date", date), ("language", language), ("collection", collection),
                 ("subject", subject), ("description", description)]:
        if v is not None:
            md[k] = v
    files = files + [f(f"{ident}.png", "PNG"), f(f"{ident}_meta.xml", "Metadata", "original")]
    return {"metadata": md, "files": files}


def audio_item(ident, title, count, rich=False, titles=None, **kw):
    files = []
    for n in range(1, count + 1):
        files += audio_entry(ident, n, (titles or (lambda i: f"Episode {i:02d}"))(n), rich)
    return item(ident, title, "audio", files, **kw)


def film(ident, title, subtitles=True, **kw):
    files = [f(f"{ident}.mpeg", "MPEG2", "original", length=LENGTH),
             f(f"{ident}.mp4", "h.264", length=LENGTH, title=title),
             f(f"{ident}_512kb.mp4", "512Kb MPEG4", length=LENGTH, title=title)]
    if subtitles:
        files += [f(f"{ident}.eng.srt", "SubRip", "original"), f(f"{ident}.asr.vtt", "Web Video Text Tracks")]
    return item(ident, title, "movies", files, **kw)


ITEMS = {
    "fx_vertical": film("fx_vertical", "Fixture Vertical Clip", subtitles=False),
    "fx_radio_show": audio_item("fx_radio_show", "Fixture Radio Show", 6, rich=True, creator="Fixture Radio", date="1950",
                                language="eng", collection=["fx_collection"], subject=["Radio", "Drama"],
                                description="<p>A <b>fixture</b> radio show.</p>"),
    "fx_series_40": audio_item("fx_series_40", "Fixture Series Forty", 40, creator="Fixture Radio", date="1951",
                               language="eng", collection=["fx_collection"]),
    "fx_series_12": audio_item("fx_series_12", "Fixture Series Twelve", 12, creator="Fixture Radio", date="1952",
                               language="eng", collection=["fx_collection"]),
    "fx_english": audio_item("fx_english", "Fixture English Talk", 1, creator="Fixture Radio", date="1953",
                             language="English", collection=["fx_collection"]),
    "fx_french": audio_item("fx_french", "Fixture Causerie", 1, creator="Radio Fixture", date="1954",
                            language="fre", collection=["fx_collection"]),
    "fx_untagged": audio_item("fx_untagged", "Fixture Untagged Recording", 1, creator="Unknown", date="1955",
                              collection=["fx_collection"]),
    "fx_film": film("fx_film", "Fixture Film", creator="Fixture Pictures", date="1940", language="eng",
                    collection=["fx_films"], subject=["Comedy"]),
    "fx_adult": film("fx_adult", "Fixture Adult Film", subtitles=False, date="1970", collection=["no-preview"],
                     subject=["xxx"]),
    "fx_nothing": item("fx_nothing", "Fixture Text Only", "movies", [f("fx_nothing.pdf", "Text PDF", "original")],
                       date="1960", language="eng", collection=["fx_films"]),
    "fx_collection": item("fx_collection", "Fixture Collection", "collection", [], description="Fixture collection."),
    "fx_films": item("fx_films", "Fixture Films", "collection", [], description="Fixture film collection."),
}
SEARCH_DEFAULT = ["fx_collection", "fx_radio_show", "fx_film", "fx_series_40", "fx_series_12", "fx_english",
                  "fx_french", "fx_untagged", "fx_adult", "fx_nothing"]


def doc(ident, downloads):
    md = ITEMS[ident]["metadata"]
    d = {k: md[k] for k in ("identifier", "title", "mediatype", "creator", "date", "language", "collection", "subject") if k in md}
    d["downloads"] = downloads
    return d


def search(docs):
    return {"responseHeader": {"status": 0}, "response": {"numFound": len(docs), "start": 0, "docs": docs}}


def write(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=1, ensure_ascii=False)


def ffmpeg(*args):
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", *args], check=True)


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    for ident, data in ITEMS.items():
        write(f"metadata/{ident}.json", data)
    for name in ("OTRR_Philip_Marlowe_Singles", "his_girl_friday"):
        shutil.copy(os.path.join(CORE, f"{name}.metadata.json"), os.path.join(OUT, f"metadata/{name}.json"))
    write("search/default.json", search([doc(i, 1000 - n) for n, i in enumerate(SEARCH_DEFAULT)]))
    write("search/empty.json", search([]))
    paging = [{"identifier": f"fx_page_{n:03d}", "title": f"Fixture Page Item {n:03d}", "mediatype": "audio",
               "language": "eng", "downloads": 500 - n} for n in range(1, 121)]
    write("search/paging.json", search(paging))
    shutil.copy(os.path.join(CORE, "search_gunsmoke_audio.json"), os.path.join(OUT, "search/gunsmoke.json"))
    media = os.path.join(OUT, "media")
    os.makedirs(media, exist_ok=True)
    tone = ["-f", "lavfi", "-i", f"sine=frequency=440:duration={LENGTH}", "-ac", "1", "-ar", "22050"]
    ffmpeg(*tone, "-c:a", "libmp3lame", "-b:a", "16k", os.path.join(media, "tone.mp3"))
    ffmpeg(*tone, "-c:a", "libvorbis", "-q:a", "0", os.path.join(media, "tone.ogg"))
    ffmpeg("-f", "lavfi", "-i", f"testsrc=size=160x90:rate=1:duration={LENGTH}", "-f", "lavfi", "-i",
           f"anullsrc=r=22050:cl=mono", "-t", str(LENGTH), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "10",
           "-c:a", "aac", "-b:a", "16k", "-movflags", "+faststart", os.path.join(media, "clip.mp4"))
    ffmpeg("-f", "lavfi", "-i", f"testsrc=size=90x160:rate=1:duration={LENGTH}", "-f", "lavfi", "-i",
           f"anullsrc=r=22050:cl=mono", "-t", str(LENGTH), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "10",
           "-c:a", "aac", "-b:a", "16k", "-movflags", "+faststart", os.path.join(media, "clip_vertical.mp4"))
    ffmpeg("-f", "lavfi", "-i", "color=c=0x2769AD:size=64x64:duration=1", "-frames:v", "1", os.path.join(media, "art.png"))
    with open(os.path.join(media, "subs.srt"), "w") as fh:
        fh.write("1\n00:00:01,000 --> 00:00:05,000\nFixture subtitle\n")
    with open(os.path.join(media, "subs.vtt"), "w") as fh:
        fh.write("WEBVTT\n\n00:00:01.000 --> 00:00:05.000\nFixture auto subtitle\n")


if __name__ == "__main__":
    main()
