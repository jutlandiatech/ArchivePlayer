"""Full source-driven PDF. Run build-docs.cjs --pdf-input and render UML first.

Requires reportlab and Pillow. See docs/README.md for the complete export command.
"""
import json
import re
import textwrap
from pathlib import Path
from xml.sax.saxutils import escape
from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate, Paragraph,
    Spacer, PageBreak, NextPageTemplate, Image, LongTable, TableStyle, HRFlowable)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / 'tmp' / 'pdfs'
OUTPUT = ROOT / 'docs' / 'ArchivePlayer-Full-Handbook.pdf'
MANIFEST = json.loads((TMP / 'handbook-input.json').read_text(encoding='utf-8'))
INK=colors.HexColor('#152e39'); GREEN=colors.HexColor('#087b70')
MUTED=colors.HexColor('#596d77'); PALE=colors.HexColor('#eaf3ee')
FONTS=Path('C:/Windows/Fonts')
for name,file in [('AP','arial.ttf'),('AP-Bold','arialbd.ttf'),('AP-Italic','ariali.ttf'),('AP-BoldItalic','arialbi.ttf'),('APMono','consola.ttf')]:
    pdfmetrics.registerFont(TTFont(name,str(FONTS/file)))
pdfmetrics.registerFontFamily('AP',normal='AP',bold='AP-Bold',italic='AP-Italic',boldItalic='AP-BoldItalic')
WIDTH=A4[0]-100
styles={
 'body':ParagraphStyle('Body',fontName='AP',fontSize=9.5,leading=14,textColor=INK,spaceAfter=8,splitLongWords=True),
 'h1':ParagraphStyle('Chapter',fontName='AP-Bold',fontSize=25,leading=30,textColor=INK,spaceAfter=18,keepWithNext=True),
 'h2':ParagraphStyle('Section',fontName='AP-Bold',fontSize=16,leading=21,textColor=GREEN,spaceBefore=17,spaceAfter=8,keepWithNext=True),
 'h3':ParagraphStyle('Subsection',fontName='AP-Bold',fontSize=11,leading=16,textColor=INK,spaceBefore=12,spaceAfter=6,keepWithNext=True),
 'small':ParagraphStyle('Small',fontName='AP',fontSize=8,leading=11,textColor=MUTED,spaceAfter=9),
 'label':ParagraphStyle('Label',fontName='AP-Bold',fontSize=9,leading=13,textColor=GREEN,spaceAfter=12,keepWithNext=True),
 'cell':ParagraphStyle('Cell',fontName='AP',fontSize=8,leading=11,textColor=INK,spaceAfter=0,splitLongWords=True),
 'code':ParagraphStyle('Code',fontName='APMono',fontSize=7.6,leading=11,textColor=INK,backColor=PALE,borderPadding=9,spaceBefore=5,spaceAfter=12),
}
unknown=set()
def clean(text):
    text=text.replace('\u2011','-').replace('\u2013','-').replace('\u2014',' - ').replace('\u00a0',' ').replace('\u22ef','...').replace('\u25b6','[play]').replace('\u2661','[like]')
    supported=pdfmetrics.getFont('AP').face.charToGlyph
    output=[]
    for char in text:
        if ord(char) not in supported and char not in '\n\t\r':
            unknown.add(char)
            output.append({'✅':'[pass]','❌':'[fail]','⚠':'[note]','📡':'[Cast]','⎚':'[Cast]'}.get(char,f'[U+{ord(char):04X}]'))
        else:output.append(char)
    return ''.join(output)
def esc(text):return escape(clean(text))
def url_for(href,file):
    if not href:return 'https://github.com/jutlandiatech/ArchivePlayer/blob/main/'+file
    if re.match(r'^(https?:|mailto:)',href):return href
    if href.startswith('#'):return 'https://github.com/jutlandiatech/ArchivePlayer/blob/main/'+file+href
    target,_,anchor=href.partition('#')
    dest=(ROOT/Path(file).parent/target).resolve().relative_to(ROOT).as_posix()
    return 'https://github.com/jutlandiatech/ArchivePlayer/blob/main/'+dest+('#'+anchor if anchor else '')
def inline(tokens,file):
    result=[]
    for t in tokens:
        kind=t['type']; text=t.get('text','')
        if kind=='strong': result.append('<b>'+inline(t['tokens'],file)+'</b>')
        elif kind=='em':result.append('<i>'+inline(t['tokens'],file)+'</i>')
        elif kind=='del':result.append('<strike>'+inline(t['tokens'],file)+'</strike>')
        elif kind=='codespan':result.append('<font name="APMono" size="8">'+esc(text)+'</font>')
        elif kind=='link':result.append('<link href="'+escape(url_for(t['href'],file),{'"':'&quot;'})+'" color="#087b70">'+inline(t['tokens'],file)+'</link>')
        elif kind=='br':result.append('<br/>')
        elif kind=='html':
            if re.match(r'<br\s*/?>',text):result.append('<br/>')
            else:result.append(esc(re.sub('<[^>]+>','',text)))
        elif kind=='image':result.append(esc('[Image: '+text+']'))
        elif t.get('tokens'):result.append(inline(t['tokens'],file))
        else:result.append(esc(text).replace('\n',' '))
    return ''.join(result)
def para(text,style='body'):return Paragraph(text,styles[style])
story=[]; diagram_index=0
def blocks(tokens,file,indent=0):
    global diagram_index
    out=[]
    for t in tokens:
        kind=t['type']
        if kind in ('space','def'):continue
        if kind in ('paragraph','text'):
            out.append(para(inline(t.get('tokens',[]),file) if t.get('tokens') else esc(t.get('text',''))))
        elif kind=='heading':
            # Chapter title is provided once, from the shared document manifest.
            if t['depth']==1:continue
            style='h2' if t['depth']==2 else 'h3'
            out.append(para(inline(t['tokens'],file),style))
        elif kind=='list':
            start=t.get('start',1) or 1
            for i,item in enumerate(t['items']):
                children=blocks(item['tokens'],file,indent+1)
                if children and isinstance(children[0],Paragraph):
                    first=children[0];style=ParagraphStyle('List',parent=styles['body'],leftIndent=14*(indent+1),firstLineIndent=0,bulletIndent=14*indent)
                    bullet=str(start+i)+'.' if t.get('ordered') else '-'
                    if item.get('task'):bullet='[x]' if item.get('checked') else '[ ]'
                    children[0]=Paragraph(first.text,style,bulletText=bullet)
                out.extend(children)
        elif kind=='blockquote':out.extend(blocks(t['tokens'],file,indent))
        elif kind=='hr':out.append(HRFlowable(width='100%',color=GREEN,spaceBefore=12,spaceAfter=12))
        elif kind=='table':
            rows=[t['header']]+t['rows'];count=len(t['header'])
            # Allocate space by typical cell length, bounded so no column collapses.
            weights=[]
            for col in range(count):
                lengths=sorted(len(re.sub('<[^>]+>','',r[col]['text'])) for r in rows)
                weights.append(min(55,max(14,lengths[len(lengths)//2])))
            widths=[WIDTH*w/sum(weights) for w in weights]
            data=[[para(inline(cell['tokens'],file),'cell') for cell in row] for row in rows]
            table=LongTable(data,colWidths=widths,repeatRows=1,hAlign='LEFT',splitByRow=1,splitInRow=1)
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),0.7,GREEN),('LINEBELOW',(0,1),(-1,-1),0.35,colors.HexColor('#dce4e3')),('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7)]))
            out.extend([Spacer(1,6),table,Spacer(1,12)])
        elif kind=='code' and t.get('lang')=='mermaid':
            diagram_index+=1
            image=TMP/f'architecture-{diagram_index}.png'
            if not image.is_file():raise FileNotFoundError(f'Rendered UML missing: {image}')
            title='Architecture diagram '+str(diagram_index)
            if out and isinstance(out[-1],Paragraph) and out[-1].style.name=='Section':title=out.pop().getPlainText()
            out.extend([NextPageTemplate('wide'),PageBreak(),para(esc(f'UML {diagram_index:02d} / '+title),'h2')])
            with PILImage.open(image) as im:w,h=im.size
            ratio=min((landscape(A4)[0]-100)/w,375/h)
            out.append(Image(str(image),width=w*ratio,height=h*ratio,hAlign='CENTER'))
            out.append(Spacer(1,10));out.append(para('Rendered from '+esc(file)+'. Mermaid source remains editable in the Markdown document.','small'))
            out.extend([NextPageTemplate('normal'),PageBreak()])
        elif kind=='code':
            lines=[]
            for line in clean(t['text']).expandtabs(4).splitlines():
                lines.extend(textwrap.wrap(line,width=98,replace_whitespace=False,drop_whitespace=False) or [''])
            # Paragraphs permit a long code block to split cleanly across pages.
            for chunk in range(0,len(lines),22):
                out.append(para('<br/>'.join(escape(s).replace(' ','&#160;') for s in lines[chunk:chunk+22]),'code'))
        elif kind=='html':
            text=re.sub('<[^>]+>','',t.get('text','')).strip()
            if text:out.append(para(esc(text)))
        else:raise ValueError('Unsupported Markdown block: '+kind)
    return out

class Handbook(BaseDocTemplate):
    def afterFlowable(self,flowable):
        if hasattr(flowable,'bookmark'):
            key,title=flowable.bookmark
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(title,key,level=0,closed=False)
            self.notify('TOCEntry',(0,title,self.page,key))

def frame(c,doc):
    w,h=c._pagesize
    c.setTitle('ArchivePlayer - Full Documentation Handbook')
    c.setAuthor('JutlandiaTech / ArchivePlayer contributors')
    c.setFillColor(GREEN);c.rect(0,h-8,w,8,fill=1,stroke=0)
    c.setFont('AP-Bold',8);c.setFillColor(INK);c.drawString(50,h-34,'ARCHIVEPLAYER')
    c.setFont('AP',8);c.setFillColor(MUTED);c.drawRightString(w-50,h-34,'FULL HANDBOOK / 16 NOV 2026')
    c.setStrokeColor(colors.HexColor('#dce4e3'));c.line(50,43,w-50,43)
    c.setFont('AP',7.5);c.drawString(50,29,'Source documentation / Implementation and historical evidence remain distinct')
    c.drawRightString(w-50,29,str(doc.page))

story.extend([Spacer(1,65),para('ARCHIVEPLAYER / FULL HANDBOOK','label'),para('The complete<br/>project handbook.','h1'),Spacer(1,16),para('Application guides, architecture and UML, requirements, testing, maintenance and UI contracts.','h2'),Spacer(1,25),para('Edition: 16 November 2026<br/>Source version: 1.0.0 / code 3<br/>19 documents / 7 rendered UML diagrams'),Spacer(1,30),para('This edition contains the full Markdown content behind the HTML handbook. Landscape pages preserve diagram detail. Use PDF bookmarks or the linked contents to jump between documents.'),para('The documentation preserves source dates and historical test results. Exporting this handbook does not rerun or establish acceptance-test success. Source links point to the GitHub main branch; a later branch revision may differ.','small'),PageBreak(),para('Contents','h1')])
toc=TableOfContents();toc.levelStyles=[ParagraphStyle('TOC',fontName='AP',fontSize=10,leading=17,textColor=INK,leftIndent=0,firstLineIndent=0,spaceBefore=4)]
story.extend([toc,PageBreak()])
for index,document in enumerate(MANIFEST):
    if index:story.append(PageBreak())
    story.append(para(esc(document['group'].upper()),'label'))
    heading=para(esc(document['title']),'h1');heading.bookmark=(document['id'],document['title']);story.append(heading)
    story.append(para('Source: <link href="'+url_for('',document['file'])+'" color="#087b70">'+esc(document['file'])+'</link>','small'))
    story.extend(blocks(document['tokens'],document['file']))

doc=Handbook(str(OUTPUT),pagesize=A4,leftMargin=50,rightMargin=50,topMargin=60,bottomMargin=58)
normal=Frame(50,58,A4[0]-100,A4[1]-118,id='normal',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)
wide_size=landscape(A4)
wide=Frame(50,58,wide_size[0]-100,wide_size[1]-118,id='wide',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)
doc.addPageTemplates([PageTemplate(id='normal',frames=[normal],onPage=frame,pagesize=A4),PageTemplate(id='wide',frames=[wide],onPage=frame,pagesize=wide_size)])
doc.multiBuild(story)
print(f'Created {OUTPUT}; {diagram_index} rendered diagrams.')
if unknown:print('Unsupported font characters replaced explicitly:',[f'U+{ord(c):04X}' for c in sorted(unknown)])
