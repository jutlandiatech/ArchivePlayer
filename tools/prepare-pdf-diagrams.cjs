/* Print layout changes only: relationships and labels remain source-driven. */
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'tmp', 'pdfs');
fs.mkdirSync(output, {recursive:true});
const source = fs.readFileSync(path.join(root,'docs','ARCHITECTURE_UML.md'),'utf8');
fs.writeFileSync(path.join(output,'uml-input.md'),source.replace(/classDiagram\r?\n/g,'classDiagram\n  direction LR\n'));
fs.writeFileSync(path.join(output,'mermaid-print.json'),JSON.stringify({theme:'base',themeVariables:{primaryColor:'#eaf3ee',primaryTextColor:'#152e39',primaryBorderColor:'#438577',lineColor:'#59717a',secondaryColor:'#edf1f4',tertiaryColor:'#f6f6ed',fontFamily:'Arial',fontSize:'16px'}}));
