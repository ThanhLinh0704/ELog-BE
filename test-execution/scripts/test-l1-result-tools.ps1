param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'
$fixtureRoot = Join-Path $RepositoryRoot 'target\l1-result-tools-contract'
$catalogPath = Join-Path $fixtureRoot 'catalog.json'
$surefireDir = Join-Path $fixtureRoot 'surefire-reports'
$jacocoPath = Join-Path $fixtureRoot 'jacoco.xml'
$resultPath = Join-Path $fixtureRoot 'l1.json'

New-Item -ItemType Directory -Force -Path $surefireDir | Out-Null

@'
{"level":"L1","cases":[
  {"id":"L1-ZZ-01","title":"passing behavior","sheet":"Fixture","priority":"P1"},
  {"id":"L1-ZZ-02","title":"failing behavior","sheet":"Fixture","priority":"P2"},
  {"id":"L1-ZZ-03","title":"not executed behavior","sheet":"Fixture","priority":"P3"}
]}
'@ | Set-Content -Encoding UTF8 $catalogPath

@'
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="FixtureTest" tests="2" failures="1" errors="0" skipped="0">
  <testcase name="[L1-ZZ-01] passing behavior" classname="com.elog.service.FixtureTest" time="0.010" />
  <testcase name="[L1-ZZ-02] failing behavior" classname="com.elog.service.FixtureTest" time="0.020">
    <failure message="expected 2 but was 1" type="org.opentest4j.AssertionFailedError">stack</failure>
  </testcase>
</testsuite>
'@ | Set-Content -Encoding UTF8 (Join-Path $surefireDir 'TEST-com.elog.service.FixtureTest.xml')

'<report name="fixture" />' | Set-Content -Encoding UTF8 $jacocoPath

& (Join-Path $PSScriptRoot 'export-l1-results.ps1') `
    -CatalogPath $catalogPath `
    -SurefireDirectory $surefireDir `
    -JacocoPath $jacocoPath `
    -OutputPath $resultPath
$ledger = Get-Content -Raw $resultPath | ConvertFrom-Json
if ($ledger.results.Count -ne 3) { throw "Expected 3 result rows, got $($ledger.results.Count)" }

$pass = $ledger.results | Where-Object testId -eq 'L1-ZZ-01'
$fail = $ledger.results | Where-Object testId -eq 'L1-ZZ-02'
$notRun = $ledger.results | Where-Object testId -eq 'L1-ZZ-03'
if ($pass.status -ne 'Pass' -or !$pass.evidence.surefireXml) { throw 'Pass mapping/evidence is invalid' }
if ($fail.status -ne 'Fail' -or $fail.failureMessage -notmatch 'expected 2') { throw 'Fail mapping is invalid' }
if ($notRun.status -ne 'Not Run' -or $notRun.evidence) { throw 'Not Run mapping is invalid' }

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'validate-l1-results.ps1') -CatalogPath $catalogPath -ResultPath $resultPath
if ($LASTEXITCODE -ne 0) { throw "Validator rejected complete ledger with $LASTEXITCODE" }

$ledger.results = @($ledger.results | Where-Object testId -ne 'L1-ZZ-03')
$ledger | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 $resultPath
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'validate-l1-results.ps1') -CatalogPath $catalogPath -ResultPath $resultPath
if ($LASTEXITCODE -eq 0) { throw 'Validator accepted a ledger with a missing catalog ID' }

Write-Host 'PASS: L1 result tools contract'
