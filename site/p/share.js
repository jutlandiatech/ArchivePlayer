// Renders an ArchivePlayer share link (R54, R56). The fragment is "v1." + base64url(raw DEFLATE(JSON)),
// JSON = {"t": title, "d": description?, "e": [[identifier, fileName, title?], ...]} (see ShareLinkCodec).
"use strict";

const ARCHIVE = "https://archive.org";

async function decode(fragment) {
  if (!fragment.startsWith("v1.")) throw new Error("unsupported link version");
  let b64 = fragment.slice(3).replace(/-/g, "+").replace(/_/g, "/");
  while (b64.length % 4) b64 += "=";
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
  const data = JSON.parse(await new Response(stream).text());
  if (typeof data.t !== "string" || !Array.isArray(data.e)) throw new Error("invalid playlist");
  return data;
}

function link(href, text) {
  const a = document.createElement("a");
  a.href = href;
  a.textContent = text;
  a.rel = "noopener";
  return a;
}

function fileUrl(identifier, fileName) {
  return `${ARCHIVE}/download/${encodeURIComponent(identifier)}/${fileName.split("/").map(encodeURIComponent).join("/")}`;
}

async function itemTitle(identifier) {
  const r = await fetch(`${ARCHIVE}/metadata/${encodeURIComponent(identifier)}/metadata`);
  if (!r.ok) return null;
  const t = (await r.json()).result?.title;
  return Array.isArray(t) ? t[0] : typeof t === "string" ? t : null;
}

async function fillTitles(spansById) {
  const ids = [...spansById.keys()].slice(0, 100);
  for (let i = 0; i < ids.length; i += 4) {
    await Promise.all(ids.slice(i, i + 4).map(async (id) => {
      const title = await itemTitle(id).catch(() => null);
      if (title) spansById.get(id).forEach((s) => { s.textContent = title; });
    }));
  }
}

async function main() {
  const status = document.getElementById("status");
  let data;
  try {
    data = await decode(decodeURIComponent(location.hash.slice(1)));
  } catch (e) {
    status.textContent = "This link can't be read. It may be incomplete or from a newer app version.";
    return;
  }
  document.title = `${data.t} · ArchivePlayer`;
  document.getElementById("title").textContent = data.t || "Shared playlist";
  document.getElementById("description").textContent = typeof data.d === "string" ? data.d : "";
  status.textContent = `${data.e.length} entries`;
  const list = document.getElementById("entries");
  const spansById = new Map();
  for (const entry of data.e) {
    if (!Array.isArray(entry) || typeof entry[0] !== "string" || typeof entry[1] !== "string") continue;
    const [identifier, fileName, title] = entry;
    const li = document.createElement("li");
    li.appendChild(link(fileUrl(identifier, fileName), title || fileName));
    li.appendChild(document.createElement("br"));
    const item = document.createElement("span");
    item.className = "muted";
    const itemName = document.createElement("span");
    itemName.textContent = identifier;
    item.append("from ");
    item.appendChild(link(`${ARCHIVE}/details/${encodeURIComponent(identifier)}`, ""));
    item.lastChild.appendChild(itemName);
    li.appendChild(item);
    list.appendChild(li);
    if (!spansById.has(identifier)) spansById.set(identifier, []);
    spansById.get(identifier).push(itemName);
  }
  fillTitles(spansById);
}

main();
