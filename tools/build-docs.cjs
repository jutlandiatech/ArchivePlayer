/* Regenerate the offline HTML edition from the authoritative Markdown files. */
const fs = require('node:fs');
const path = require('node:path');
const { marked } = require('../docs/assets/vendor/marked.umd.js');
const root = path.resolve(__dirname, '..');
const documents = [
  ['overview', 'Overview', 'docs/README.md', 'Start here'],
  ['user-guide', 'User guide', 'docs/USER_GUIDE.md', 'Using ArchivePlayer'],
  ['getting-started', 'Getting started', 'docs/GETTING_STARTED.md', 'Development'],
  ['architecture', 'Architecture & UML', 'docs/ARCHITECTURE_UML.md', 'Engineering'],
  ['data', 'Data & integrations', 'docs/DATA_AND_INTEGRATIONS.md', 'Engineering'],
  ['maintenance', 'Maintenance', 'docs/MAINTENANCE.md', 'Development'],
  ['app-links', 'App Links', 'docs/APP_LINKS.md', 'Development'],
  ['requirements', 'Specification', 'SPECIFICATION.md', 'Reference'],
  ['testing', 'Testing & traceability', 'TESTING.md', 'Reference'],
  ['evidence', 'Recorded test evidence', 'TDD_EVIDENCE.md', 'Reference'],
  ['environment', 'Development environment', 'DEVELOPMENT_ENVIRONMENT.md', 'Reference'],
  ...['SHELL','SETUP','HOME','SEARCH','ITEM','PLAYER','LIBRARY','SETTINGS'].map(s =>
    [`ui-${s.toLowerCase()}`, `UI: ${s[0]+s.slice(1).toLowerCase()}`, `UI_${s}_SPEC.md`, 'UI contracts'])
];
const byPath = new Map(documents.map(d => [d[2], d[0]]));
const escape = s => s.replaceAll('&','&amp;').replaceAll('"','&quot;').replaceAll('<','&lt;').replaceAll('>','&gt;');
let headingSlugs = new Map();
const renderer = new marked.Renderer();
renderer.heading = function ({tokens, depth}) {
  const content = this.parser.parseInline(tokens);
  const base = content.replace(/<[^>]*>/g,'').toLowerCase().replace(/[^\p{L}\p{N}\s_-]/gu,'').replace(/ /g,'-');
  const duplicate = headingSlugs.get(base) || 0;
  headingSlugs.set(base,duplicate+1);
  return `<h${depth} id="${escape(base+(duplicate ? '-'+duplicate : ''))}">${content}</h${depth}>`;
};
renderer.code = ({text, lang}) => lang === 'mermaid'
  ? `<figure class="diagram"><div class="diagram-toolbar"><span>UML / ARCHITECTURE</span><button type="button" class="diagram-toggle">Show source</button></div><pre class="mermaid">${escape(text)}</pre><pre class="diagram-source" hidden><code>${escape(text)}</code></pre></figure>`
  : `<pre><code class="language-${escape(lang || 'text')}">${escape(text)}</code></pre>`;
marked.use({renderer, gfm:true});
for (const [id,title,file,group] of documents) {
  headingSlugs = new Map();
  let body = marked.parse(fs.readFileSync(path.join(root,file),'utf8'));
  body = body.replace(/href="([^"]+)"/g, (match, href) => {
    if (/^(https?:|mailto:|#)/.test(href)) return match;
    const [target,anchor] = href.split('#');
    const resolved = path.posix.normalize(path.posix.join(path.posix.dirname(file),target));
    const page = byPath.get(resolved);
    const localAsset = resolved.startsWith('docs/') && !resolved.endsWith('.md');
    const destination = page ? `${page === 'overview' ? 'index' : page}.html${anchor ? '#'+anchor : ''}` : localAsset ? `${resolved.slice(5)}${anchor ? '#'+anchor : ''}` : `https://github.com/jutlandiatech/ArchivePlayer/blob/main/${resolved}${anchor ? '#'+anchor : ''}`;
    return `href="${escape(destination)}"`;
  });
  const nav = documents.map(d => `<a href="${d[0] === 'overview' ? 'index' : d[0]}.html" ${d[0] === id ? 'aria-current="page"' : ''}><span>${escape(d[1])}</span></a>`).join('');
  const hero = id === 'overview' ? `<div class="hero"><span class="eyebrow">PROJECT HANDBOOK / ANDROID</span><h1>Explore the archive.<br>Understand the app.</h1><p>A practical guide to ArchivePlayer, from your first stream to the systems behind playback, storage and Cast.</p><div class="hero-actions"><a class="primary" href="user-guide.html">Read the user guide <span>↗</span></a><a class="secondary" href="ArchivePlayer-Documentation-Summary.pdf">Download PDF summary ↓</a></div><div class="hero-meta"><span>2 Gradle modules</span><span>2 distribution flavours</span><span>7 architecture diagrams</span></div></div><div class="cards"><a href="architecture.html"><b>01 / Architecture</b><h2>The system, mapped.</h2><p>Components, class models, sequences and download states.</p></a><a href="getting-started.html"><b>02 / Development</b><h2>From source to device.</h2><p>Set up the toolchain, build flavours and run the right checks.</p></a><a href="data.html"><b>03 / Data</b><h2>Know the boundaries.</h2><p>Persistence, external requests, imports and exports.</p></a></div>` : '';
  const html = `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="description" content="ArchivePlayer documentation: ${escape(title)}"><title>${escape(title)} · ArchivePlayer Docs</title><link rel="stylesheet" href="assets/docs.css"><script defer src="assets/vendor/mermaid.min.js"></script><script defer src="assets/docs.js"></script></head><body><a class="skip" href="#content">Skip to content</a><header class="topbar"><a class="brand" href="index.html"><span class="brand-mark" aria-hidden="true">A<span>≋</span></span><span>ArchivePlayer <small>Documentation</small></span></a><div class="top-actions"><a href="ArchivePlayer-Documentation-Summary.pdf">PDF summary ↓</a><a href="https://github.com/jutlandiatech/ArchivePlayer">Source ↗</a><button id="menu" aria-expanded="false" aria-controls="sidebar">Menu</button></div></header><div class="layout"><aside id="sidebar"><span class="eyebrow">THE HANDBOOK</span><label class="search-label" for="nav-search">Find a document</label><input id="nav-search" type="search" placeholder="Search documents…"><nav aria-label="Documentation">${nav}</nav><p id="no-results" hidden>No matching documents.</p><div class="sidebar-note">Source-led documentation<br><strong>HTML edition · 16 Nov 2026</strong></div></aside><main id="content">${hero}<div class="page-heading"><span class="eyebrow">${escape(group)}</span><div><span>ArchivePlayer / ${escape(title)}</span><a href="https://github.com/jutlandiatech/ArchivePlayer/blob/main/${file}">Edit Markdown ↗</a></div></div><div class="reading-layout"><article>${body}</article><aside class="on-this-page"><span class="eyebrow">ON THIS PAGE</span><nav id="toc" aria-label="On this page"></nav></aside></div><footer>ArchivePlayer · Independent of the Internet Archive.<br>Implementation notes and historical test evidence are distinguished throughout.</footer></main></div></body></html>`;
  fs.writeFileSync(path.join(root,'docs',id === 'overview' ? 'index.html' : id+'.html'),html);
}
console.log(`Generated ${documents.length} offline HTML pages.`);
