# Runs the Gradle wrapper from the project root with plain console output.
# Example: .\tools\run-gradle.ps1 :core:test
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArguments
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:ANDROID_HOME) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $defaultSdk) { $env:ANDROID_HOME = $defaultSdk }
}
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @GradleArguments --console=plain
    exit $LASTEXITCODE
} finally { Pop-Location }
