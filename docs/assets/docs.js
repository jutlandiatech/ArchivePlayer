/* No fetch or remote fonts: the generated pages work from file:// as well as HTTP. */
const menu = document.getElementById('menu');
menu.addEventListener('click', () => {
  const open = document.getElementById('sidebar').classList.toggle('open');
  menu.setAttribute('aria-expanded', String(open));
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') { document.getElementById('sidebar').classList.remove('open'); menu.setAttribute('aria-expanded','false'); }
});
document.getElementById('nav-search').addEventListener('input', event => {
  const query = event.target.value.trim().toLowerCase();
  let count = 0;
  document.querySelectorAll('#sidebar nav a').forEach(link => {
    link.hidden = !link.textContent.toLowerCase().includes(query);
    if (!link.hidden) count++;
  });
  document.getElementById('no-results').hidden = count > 0;
});
const toc = document.getElementById('toc');
document.querySelectorAll('article h2').forEach(heading => {
  const link = document.createElement('a'); link.href = '#'+heading.id; link.textContent = heading.textContent; toc.append(link);
});
document.querySelectorAll('.diagram-toggle').forEach(button => button.addEventListener('click', () => {
  const source = button.closest('figure').querySelector('.diagram-source'); source.hidden = !source.hidden;
  button.textContent = source.hidden ? 'Show source' : 'Hide source';
}));
async function drawDiagrams() {
  const elements = document.querySelectorAll('.mermaid');
  if (!elements.length) return;
  if (!window.mermaid) { document.querySelectorAll('.diagram-source').forEach(s=>s.hidden=false); return; }
  mermaid.initialize({startOnLoad:false,securityLevel:'strict',theme:'base',themeVariables:{primaryColor:'#eaf3ee',primaryTextColor:'#152e39',primaryBorderColor:'#438577',lineColor:'#59717a',secondaryColor:'#edf1f4',tertiaryColor:'#f6f6ed',fontFamily:'Segoe UI, Arial, sans-serif',fontSize:'14px'},flowchart:{useMaxWidth:false},sequence:{useMaxWidth:false}});
  for (const element of elements) {
    try { await mermaid.run({nodes:[element]}); }
    catch (error) {
      const figure = element.closest('figure');
      figure.querySelector('.diagram-source').hidden = false;
      const message = document.createElement('p'); message.className='diagram-failure'; message.textContent='Diagram could not render. Editable source is shown below.'; figure.prepend(message);
    }
  }
  document.documentElement.dataset.diagramsReady = 'true';
}
drawDiagrams();
