param([string]$Version = "0.12.0")
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (git status --porcelain) { throw 'Commit changes before requesting a signed build.' }
$revision = git rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Cannot read committed source.' }
git push
if ($LASTEXITCODE -ne 0) { throw 'Source push failed.' }
gh workflow run edition-signing.yml --repo axelsarassamit/gearelec-gx12-companion -f project=RideDeck -f revision=$revision -f version=$Version
if ($LASTEXITCODE -ne 0) { throw 'Signing request failed.' }
Write-Host 'Signing requested. Download the RideDeck-signed artifact from the workflow run:'
Write-Host 'https://github.com/axelsarassamit/gearelec-gx12-companion/actions/workflows/edition-signing.yml'
