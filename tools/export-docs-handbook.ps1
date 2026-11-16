# Windows full-handbook export. Uses Edge only as the Mermaid image-rendering engine.
param(
    [string]$Python = 'python',
    [string]$Edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path -LiteralPath $Edge)) { throw "Edge not found: $Edge. Pass -Edge with its executable path." }
Push-Location $projectRoot
try {
    & $Python -c 'import reportlab, PIL'
    if ($LASTEXITCODE -ne 0) { throw 'Install PDF dependencies: python -m pip install reportlab Pillow' }
    $renderRoot = Join-Path $projectRoot 'tmp/docs-render'
    $pdfRoot = Join-Path $projectRoot 'tmp/pdfs'
    New-Item -ItemType Directory -Force $renderRoot, $pdfRoot | Out-Null
    $lockSource = Join-Path $PSScriptRoot 'docs-render/package-lock.json'
    $lockTarget = Join-Path $renderRoot 'package-lock.json'
    $sameLock = (Test-Path -LiteralPath $lockTarget) -and ((Get-FileHash $lockSource).Hash -eq (Get-FileHash $lockTarget).Hash)
    $cli = Join-Path $renderRoot 'node_modules/.bin/mmdc.cmd'
    Copy-Item -LiteralPath $lockSource -Destination $lockTarget
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'docs-render/package.json') -Destination (Join-Path $renderRoot 'package.json')
    if (-not $sameLock -or -not (Test-Path -LiteralPath $cli)) {
        # Puppeteer's browser download is unnecessary: Edge is already installed.
        & npm.cmd ci --prefix $renderRoot --ignore-scripts --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'Mermaid dependency installation failed.' }
    }
    $browserConfig = @{executablePath=$Edge;headless=$true} | ConvertTo-Json
    [System.IO.File]::WriteAllText((Join-Path $pdfRoot 'puppeteer.json'), $browserConfig)
    & node tools/build-docs.cjs --pdf-input
    if ($LASTEXITCODE -ne 0) { throw 'Markdown token export failed.' }
    & node tools/prepare-pdf-diagrams.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Diagram preparation failed.' }
    & $cli -i tmp/pdfs/uml-input.md -o tmp/pdfs/architecture.md -e png -s 2 -w 1800 -p tmp/pdfs/puppeteer.json -c tmp/pdfs/mermaid-print.json
    if ($LASTEXITCODE -ne 0) { throw 'UML rendering failed.' }
    & $Python tools/export-docs-handbook.py
    if ($LASTEXITCODE -ne 0) { throw 'PDF export failed.' }
} finally { Pop-Location }
