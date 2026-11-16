"""Create the concise, source-linked PDF companion. Requires reportlab."""
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'docs' / 'ArchivePlayer-Documentation-Summary.pdf'
INK = colors.HexColor('#152e39')
GREEN = colors.HexColor('#087b70')
MUTED = colors.HexColor('#596d77')
PALE = colors.HexColor('#eaf3ee')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='TitleAP',fontName='Helvetica-Bold',fontSize=35,leading=39,textColor=INK,spaceAfter=18))
styles.add(ParagraphStyle(name='DeckAP',fontName='Helvetica',fontSize=13,leading=19,textColor=MUTED,spaceAfter=18))
styles.add(ParagraphStyle(name='HeadingAP',fontName='Helvetica-Bold',fontSize=21,leading=26,textColor=INK,spaceAfter=16))
styles.add(ParagraphStyle(name='SubAP',fontName='Helvetica-Bold',fontSize=12,leading=16,textColor=GREEN,spaceBefore=10,spaceAfter=5))
styles.add(ParagraphStyle(name='BodyAP',fontName='Helvetica',fontSize=10,leading=14,textColor=INK,spaceAfter=8))
styles.add(ParagraphStyle(name='SmallAP',fontName='Helvetica',fontSize=8,leading=12,textColor=MUTED,spaceAfter=7))
styles.add(ParagraphStyle(name='LabelAP',fontName='Helvetica-Bold',fontSize=9,leading=12,textColor=GREEN,spaceAfter=14))
story=[]
def p(text,style='BodyAP'):
    return Paragraph(text,styles[style])
def add(text,style='BodyAP'):
    story.append(p(text,style))
def section(title,text):
    story.append(KeepTogether([p(title,'SubAP'),p(text)]))
def table(rows,widths):
    cells=[[p(escape(c),'SmallAP') for c in row] for row in rows]
    t=Table(cells,colWidths=widths,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),0.7,GREEN),('LINEBELOW',(0,1),(-1,-1),0.4,colors.HexColor('#dce4e3')),('LEFTPADDING',(0,0),(-1,-1),10),('RIGHTPADDING',(0,0),(-1,-1),10),('TOPPADDING',(0,0),(-1,-1),9),('BOTTOMPADDING',(0,0),(-1,-1),6)]))
    story.append(t)
def sources(names):
    story.append(Spacer(1,12))
    add('SOURCE NOTES', 'LabelAP')
    for label,path in names:
        url='https://github.com/jutlandiatech/ArchivePlayer/blob/main/'+path
        add(f'<link href="{url}" color="#087b70">{escape(label)}</link> - {escape(path)}','SmallAP')
def newpage(label,title):
    story.append(PageBreak());add(label,'LabelAP');add(title,'HeadingAP')

add('ARCHIVEPLAYER / PROJECT HANDBOOK','LabelAP')
story.append(Spacer(1,28))
add('Discover the archive.<br/>Understand the app.','TitleAP')
add('Documentation summary<br/>Android application, architecture and maintenance','DeckAP')
add('Edition: 16 November 2026 | Source version: 1.0.0 / code 3','SmallAP')
story.append(Spacer(1,20))
section('Purpose','ArchivePlayer is an independent Android app for discovering, streaming and casting Internet Archive audio and video. It supports local playlists, likes, follows, playback history and audio downloads. There is no ArchivePlayer application backend in this repository.')
table([['At a glance','Implementation'],['Platform','Android API 26+; Kotlin; Jetpack Compose; Media3'],['Modules','app: Android integration and UI; core: Kotlin/JVM domain rules'],['Distribution','full includes Cast; foss omits proprietary Cast dependencies'],['Languages','Bundled English, German and Danish interface catalogs'],['Local data','SharedPreferences settings; JSON library/download index; app-private audio']],[120,375])
story.append(Spacer(1,18))
add('Read this summary alongside the offline HTML handbook in docs/index.html. The HTML edition contains the full specifications, test evidence and seven editable UML/architecture diagrams. Markdown remains the maintainable source.','BodyAP')
add('This document summarizes implementation. Historical test evidence is not a new verification of this checkout.','SmallAP')
sources([('Documentation index','docs/README.md'),('Agreed requirements','SPECIFICATION.md')])

newpage('01 / SYSTEM DESIGN','Architecture and ownership')
add('The app depends on a platform-independent core. AppViewModel coordinates setup, navigation, library actions, playback and downloads; it is a substantial coordinator rather than a thin adapter.')
d=Drawing(495,226)
def box(x,y,w,h,line1,line2=''):
    d.add(Rect(x,y,w,h,rx=7,ry=7,fillColor=PALE,strokeColor=colors.HexColor('#98b8ae')))
    d.add(String(x+w/2,y+h/2+(3 if line2 else -3),line1,textAnchor='middle',fontName='Helvetica-Bold',fontSize=10,fillColor=INK))
    if line2:d.add(String(x+w/2,y+h/2-12,line2,textAnchor='middle',fontName='Helvetica',fontSize=8,fillColor=MUTED))
def arrow(x1,y1,x2,y2):
    d.add(Line(x1,y1,x2,y2,strokeColor=GREEN,strokeWidth=1.2))
    d.add(Polygon([x2-3,y2+6,x2+3,y2+6,x2,y2],fillColor=GREEN,strokeColor=GREEN))
box(157,177,180,43,'Compose screens','AppViewModel / SearchController')
box(0,91,150,49,'Data adapters','Repository / JSON / preferences')
box(173,91,150,49,'Playback','Controller / session / player')
box(345,91,150,49,'Downloads','Worker / storage policies')
for x in [75,248,420]:
    d.add(Line(247,177,x,147,strokeColor=GREEN));arrow(x,147,x,140)
box(82,3,330,46,'core: Kotlin/JVM','Catalog / search / formats / codecs / policies')
for x in [75,248,420]:
    d.add(Line(x,91,247,56,strokeColor=GREEN))
arrow(247,56,247,49)
story.append(d)
section('Playback service boundary','PlayerConnection uses a MediaController to communicate with PlaybackService and its MediaSession. The service owns the player. The full flavour wraps ExoPlayer with CastPlayer; foss returns the local player. The view model tracks entry metadata, format fallback and history.')
section('Catalog identity','An ArchiveItem contains entries. Each entry has alternative MediaFormat files and optional subtitle tracks. Playlists contain EntryRef values (item identifier and filename), not media bytes. DownloadRecord separates entry identity from the stored format filename.')
section('External boundaries','Archive supplies search, metadata, images and media. GitHub Pages serves the category map and playlist share page. Cast receivers fetch the remote media URL directly, even when the phone has a downloaded copy.')
sources([('Seven detailed diagrams','docs/ARCHITECTURE_UML.md'),('Coordinator','app/src/main/java/com/jutlandiatech/archiveplayer/AppViewModel.kt')])

newpage('02 / RUNTIME BEHAVIOR','Search, playback and downloads')
section('Search with fallback','SearchController persists recent queries and requests 50 rows per page. ArchiveRepository uses website-ranked search when applicable, falling back to advancedsearch on request/parsing failure. Results are filtered and deduplicated; initial and later-page errors are tracked separately.')
section('Network behavior','ArchiveClient allows three total attempts for HTTP 429/5xx, honoring integer-second Retry-After or exponential delay. It checks the initial host and adds a version User-Agent. Direct downloads and Media3 have separate network paths; this is not a universal network gateway.')
section('Playback and history','FormatSelector chooses a playable file. Local playback prefers a finished downloaded file when present, otherwise a stream URL. Error callbacks try alternative formats. History is capped at 500 records, with position saved on pause and periodically during playback unless history is paused.')
section('Audio download lifecycle','The sequential worker moves queued entries through waiting, downloading, done or failed states. Pending transfers use .part files and Range requests; appending requires HTTP 206. Persisted in-progress entries reload as queued. Offline failures requeue; connected failures become failed.')
section('Storage moves and limitations','Internal or removable app-specific storage is supported. Moving completed audio uses copy, file-length verification, location update and source deletion. Length checks are not content checksums. Removing a download record does not explicitly cancel an active blocking HTTP transfer.')
table([['Boundary','Important detail'],['Cast subtitles','Receiver conversion retains WebVTT, not SRT.'],['Category updates','Validated cached map is selected on a later launch.'],['Offline use','Downloaded audio is available locally; metadata is not a persistent offline catalog.'],['Import/export','Library references/settings are portable; downloaded bytes are not bundled.']],[120,375])
sources([('Storage and integrations','docs/DATA_AND_INTEGRATIONS.md'),('End-user workflows','docs/USER_GUIDE.md')])

newpage('03 / OPERATIONS','Build, verify and maintain')
add('Use tools/run-gradle.ps1 from a PowerShell terminal at the repository root. SDK/JDK setup and emulator instructions are in the Getting started guide.')
table([['Task','Gradle arguments'],['Domain tests',':core:test'],['Build both flavours',':app:assembleFullDebug :app:assembleFossDebug'],['Android lint',':app:lintDebug'],['Foss separation',':app:verifyFossDependencies'],['Device/emulator UI tests',':app:connectedFullDebugAndroidTest\n:app:connectedFossDebugAndroidTest']],[145,350])
section('Verification discipline','Follow the specification-first workflow for behavioral changes: link requirement IDs, record a failing test, implement, rerun and append measured evidence. Keep historical results intact. Instrumentation uses local fixtures and clears app data; use a disposable test installation.')
section('What remains device-dependent','Physical Cast handover, removable storage, PiP, Android backup/restore, tablet layouts and performance require validation at the appropriate boundary. Passing JVM tests or an older emulator run does not establish those outcomes.')
section('Keep the handbook current','Edit the Markdown sources, then run node tools/build-docs.cjs. Regenerate this PDF with tools/export-docs-summary.py using Python with reportlab installed. Inspect the resulting pages and diagrams. Source version, packaged APK version and recorded test version must be checked separately.')
section('Reference and scope','The sim-warning-app repository informed the source-linked UML and generated-PDF approach. ArchivePlayer adds an offline HTML edition. This concise PDF does not replace the full requirements, UI contracts or test evidence in the handbook.')
sources([('Getting started','docs/GETTING_STARTED.md'),('Maintenance guide','docs/MAINTENANCE.md'),('Testing strategy','TESTING.md'),('Historical test evidence','TDD_EVIDENCE.md')])

def frame(c,doc):
    c.setTitle('ArchivePlayer - Documentation Summary')
    c.setAuthor('JutlandiaTech / ArchivePlayer contributors')
    w,h=doc.pagesize
    c.setFillColor(GREEN);c.rect(0,h-9,w,9,fill=1,stroke=0)
    c.setFont('Helvetica-Bold',8);c.setFillColor(INK);c.drawString(50,h-35,'ARCHIVEPLAYER')
    c.setFont('Helvetica',8);c.setFillColor(MUTED);c.drawRightString(w-50,h-35,'DOCUMENTATION / 16 NOV 2026')
    c.setStrokeColor(colors.HexColor('#dce4e3'));c.line(50,44,w-50,44)
    c.setFont('Helvetica',8);c.drawString(50,30,'Source-led summary | Full handbook: docs/index.html');c.drawRightString(w-50,30,f'{doc.page}')

doc=SimpleDocTemplate(str(OUTPUT),pagesize=(595.28,841.89),leftMargin=50,rightMargin=50,topMargin=65,bottomMargin=62)
doc.build(story,onFirstPage=frame,onLaterPages=frame)
print(OUTPUT)
