param(
    [string]$CatalogDir = 'test-execution\catalog',
    [string]$Results = 'test-execution\results\all-results.json',
    [string]$EvidenceRoot = 'test-execution'
)

$ErrorActionPreference = 'Stop'
node "$PSScriptRoot\validate-results.mjs" `
    --catalog-dir $CatalogDir `
    --results $Results `
    --evidence-root $EvidenceRoot
exit $LASTEXITCODE
