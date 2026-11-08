#!/usr/bin/env python3
"""Generates app/src/main/assets/catalog/category-map.json (R14-R17, R68)."""
import json, pathlib

def leaf(id, en, de, da, kind, collections=(), subjects=(), extra=None):
    q = {"collections": list(collections), "subjects": list(subjects)}
    if extra: q["extra"] = extra
    return {"id": id, "names": {"en": en, "de": de, "da": da}, "kind": kind, "query": q}

def node(id, en, de, da, kind, children, **q):
    n = {"id": id, "names": {"en": en, "de": de, "da": da}, "kind": kind, "children": children}
    if q: n["query"] = {"collections": list(q.get("collections", ())), "subjects": list(q.get("subjects", ()))}
    return n

decades = [leaf(f"video.decades.{d}s", f"{d}s", f"{d}er", f"{d}'erne", "video", ["feature_films", "moviesandfilms"], extra=f"year:[{d} TO {d+9}]") for d in range(1940, 2000, 10)]
video = node("video", "Video", "Video", "Video", "video", [
    node("video.movies", "Movies", "Filme", "Film", "video", [
        leaf("video.movies.comedy", "Comedy", "Komödie", "Komedie", "video", ["Comedy_Films"], ["comedy"]),
        leaf("video.movies.drama", "Drama", "Drama", "Drama", "video", ["feature_films"], ["drama"]),
        leaf("video.movies.noir", "Film Noir", "Film noir", "Film noir", "video", ["Film_Noir"], ["film noir"]),
        leaf("video.movies.scifi_horror", "Sci-Fi & Horror", "Sci-Fi & Horror", "Sci-fi & gys", "video", ["SciFi_Horror"], ["science fiction", "horror"]),
        leaf("video.movies.westerns", "Westerns", "Western", "Westerns", "video", ["western_movies"], ["western"]),
        leaf("video.movies.silent", "Silent films", "Stummfilme", "Stumfilm", "video", ["silent_films"], ["silent film"]),
    ], collections=["feature_films", "moviesandfilms"]),
    leaf("video.animation", "Animation & cartoons", "Animation & Zeichentrick", "Animation & tegnefilm", "video", ["animationandcartoons"], ["animation", "cartoon"]),
    leaf("video.tv", "TV", "Fernsehen", "TV", "video", ["classic_tv"], ["television"]),
    node("video.decades", "Decades", "Jahrzehnte", "Årtier", "video", decades),
    leaf("video.documentaries", "Documentaries", "Dokumentarfilme", "Dokumentarer", "video", ["documentaries"], ["documentary"]),
    leaf("video.newsreels", "Newsreels & historical footage", "Wochenschauen & historische Aufnahmen", "Ugerevyer & historiske optagelser", "video", ["newsandpublicaffairs", "prelinger"], ["newsreel"]),
])
audio = node("audio", "Audio", "Audio", "Lyd", "audio", [
    node("audio.music", "Music", "Musik", "Musik", "audio", [
        leaf("audio.music.rock", "Rock", "Rock", "Rock", "audio", [], ["rock"]),
        leaf("audio.music.pop", "Pop", "Pop", "Pop", "audio", [], ["pop"]),
        leaf("audio.music.jazz", "Jazz", "Jazz", "Jazz", "audio", ["jazz"], ["jazz"]),
        leaf("audio.music.classical", "Classical", "Klassik", "Klassisk", "audio", ["classicalmusic"], ["classical"]),
        leaf("audio.music.folk", "Folk & World", "Folk & Weltmusik", "Folk & verdensmusik", "audio", [], ["folk", "world music"]),
        leaf("audio.music.hiphop", "Hip-hop", "Hip-Hop", "Hiphop", "audio", ["hiphopmixtapes"], ["hip hop"]),
        leaf("audio.music.electronic", "Electronic/Netlabels", "Elektronisch/Netlabels", "Elektronisk/netlabels", "audio", ["netlabels"], ["electronic"]),
        leaf("audio.music.78rpm", "78rpm records", "Schellackplatten (78 rpm)", "78-plader", "audio", ["78rpm"], []),
        leaf("audio.music.live", "Live concerts", "Livekonzerte", "Livekoncerter", "audio", ["etree"], []),
    ], collections=["audio_music"]),
    node("audio.otr", "Old Time Radio", "Altes Radio", "Gammel radio", "audio", [
        leaf("audio.otr.mystery", "Mystery & Suspense", "Krimi & Spannung", "Krimi & spænding", "audio", ["oldtimeradio"], ["mystery", "detective"]),
        leaf("audio.otr.scifi", "Sci-Fi", "Science-Fiction", "Science fiction", "audio", ["oldtimeradio"], ["science fiction"]),
        leaf("audio.otr.comedy", "Comedy", "Komödie", "Komedie", "audio", ["oldtimeradio"], ["comedy"]),
        leaf("audio.otr.westerns", "Westerns", "Western", "Westerns", "audio", ["oldtimeradio"], ["western"]),
    ], collections=["oldtimeradio"]),
    leaf("audio.podcasts", "Podcasts", "Podcasts", "Podcasts", "audio", ["podcasts"], ["podcast"]),
    leaf("audio.spoken", "Audiobooks & spoken word", "Hörbücher & gesprochenes Wort", "Lydbøger & oplæsning", "audio", ["audio_bookspoetry", "librivoxaudio"], []),
])
data = {"schemaVersion": 1, "version": "2026-11-08", "categories": [video, audio]}
out = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/catalog/category-map.json"
out.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("wrote", out)
