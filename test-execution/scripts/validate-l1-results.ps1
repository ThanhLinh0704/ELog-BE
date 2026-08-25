param(
    [Parameter(Mandatory = $true)][string]$CatalogPath,
    [Parameter(Mandatory = $true)][string]$ResultPath
)

$ErrorActionPreference = 'Stop'
$catalog = Get-Content -Raw $CatalogPath | ConvertFrom-Json
$ledger = Get-Content -Raw $ResultPath | ConvertFrom-Json
$expectedIds = @($catalog.cases | ForEach-Object id)
$actualIds = @($ledger.results | ForEach-Object testId)
$errors = [System.Collections.Generic.List[string]]::new()

$duplicates = @($actualIds | Group-Object | Where-Object Count -gt 1 | ForEach-Object Name)
$missing = @($expectedIds | Where-Object { $_ -notin $actualIds })
$extra = @($actualIds | Where-Object { $_ -notin $expectedIds })
if ($duplicates.Count) { $errors.Add("Duplicate IDs: $($duplicates -join ', ')") }
if ($missing.Count) { $errors.Add("Missing IDs: $($missing -join ', ')") }
if ($extra.Count) { $errors.Add("Extra IDs: $($extra -join ', ')") }

foreach ($row in $ledger.results) {
    if ($row.status -notin @('Pass', 'Fail', 'Not Run')) { $errors.Add("$($row.testId): invalid status '$($row.status)'") }
    if ($row.status -in @('Pass', 'Fail')) {
        if (!$row.evidence.surefireXml -or !$row.evidence.testcase -or !$row.evidence.jacocoXml) {
            $errors.Add("$($row.testId): executed result is missing Surefire/JaCoCo evidence")
        }
        if ([string]$row.evidence.testcase -notmatch [regex]::Escape([string]$row.testId)) {
            $errors.Add("$($row.testId): testcase evidence does not contain its catalog ID")
        }
    }
}

if ($ledger.summary.total -ne $expectedIds.Count) { $errors.Add("Summary total $($ledger.summary.total) does not equal catalog count $($expectedIds.Count)") }
if (($ledger.summary.pass + $ledger.summary.fail + $ledger.summary.notRun) -ne $ledger.summary.total) { $errors.Add('Summary status counts do not add up to total') }

if ($errors.Count) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "PASS: validated $($expectedIds.Count) unique L1 result rows"
