param(
    [Parameter(Mandatory = $true)][string]$CatalogPath,
    [Parameter(Mandatory = $true)][string]$SurefireDirectory,
    [Parameter(Mandatory = $true)][string]$JacocoPath,
    [Parameter(Mandatory = $true)][string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$catalog = Get-Content -Raw $CatalogPath | ConvertFrom-Json
$matchesById = @{}

Get-ChildItem -Path $SurefireDirectory -Filter 'TEST-*.xml' -File | ForEach-Object {
    $xmlPath = $_.FullName
    [xml]$suite = Get-Content -Raw $xmlPath
    foreach ($testcase in $suite.SelectNodes('//testcase')) {
        $idMatches = [regex]::Matches([string]$testcase.name, 'L1-[A-Z]+-[0-9]+')
        foreach ($idMatch in $idMatches) {
            $id = $idMatch.Value
            if (!$matchesById.ContainsKey($id)) { $matchesById[$id] = @() }
            $matchesById[$id] += [pscustomobject]@{ XmlPath = $xmlPath; Testcase = $testcase }
        }
    }
}

$rows = foreach ($case in $catalog.cases) {
    $matches = @($matchesById[$case.id] | Where-Object { $null -ne $_ })
    if ($matches.Count -gt 1) {
        throw "Catalog ID $($case.id) is claimed by $($matches.Count) Surefire testcases"
    }

    if ($matches.Count -eq 0) {
        [pscustomobject]@{
            testId = $case.id
            level = 'L1'
            sheet = $case.sheet
            priority = $case.priority
            title = $case.title
            status = 'Not Run'
            durationSeconds = $null
            failureMessage = 'No one-to-one Surefire testcase containing this catalog ID was executed.'
            evidence = $null
        }
        continue
    }

    $match = $matches[0]
    $testcase = $match.Testcase
    $matchXmlPath = [string]$match.XmlPath
    if ([string]::IsNullOrWhiteSpace($matchXmlPath)) { throw "$($case.id): matched testcase has no XML path" }
    $failure = $testcase.failure
    $errorNode = $testcase.error
    $skipped = $testcase.skipped
    $status = if ($failure -or $errorNode) { 'Fail' } elseif ($skipped) { 'Not Run' } else { 'Pass' }
    $failureMessage = if ($failure) { [string]$failure.message } elseif ($errorNode) { [string]$errorNode.message } elseif ($skipped) { [string]$skipped.message } else { $null }
    $relativeXml = ((Resolve-Path -Relative $matchXmlPath) -replace '^\.\\', '').Replace('\', '/')
    $relativeJacoco = ((Resolve-Path -Relative $JacocoPath) -replace '^\.\\', '').Replace('\', '/')

    [pscustomobject]@{
        testId = $case.id
        level = 'L1'
        sheet = $case.sheet
        priority = $case.priority
        title = $case.title
        status = $status
        durationSeconds = [double]$testcase.time
        failureMessage = $failureMessage
        evidence = [pscustomobject]@{
            surefireXml = $relativeXml
            testcase = [string]$testcase.name
            classname = [string]$testcase.classname
            jacocoXml = $relativeJacoco
        }
    }
}

$output = [pscustomobject]@{
    generatedAt = (Get-Date).ToString('o')
    level = 'L1'
    catalogPath = [IO.Path]::GetFullPath($CatalogPath).Replace('\', '/')
    surefireDirectory = [IO.Path]::GetFullPath($SurefireDirectory).Replace('\', '/')
    jacocoPath = [IO.Path]::GetFullPath($JacocoPath).Replace('\', '/')
    summary = [pscustomobject]@{
        total = @($rows).Count
        pass = @($rows | Where-Object status -eq 'Pass').Count
        fail = @($rows | Where-Object status -eq 'Fail').Count
        notRun = @($rows | Where-Object status -eq 'Not Run').Count
    }
    results = @($rows)
}

$parent = Split-Path -Parent $OutputPath
if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
$output | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 $OutputPath
