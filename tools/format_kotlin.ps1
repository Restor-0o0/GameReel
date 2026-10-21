param(
    [Parameter(Mandatory = $true)]
    [string]$FormatterJar,
    [switch]$Check
)

$ErrorActionPreference = 'Stop'
$jar = (Resolve-Path -LiteralPath $FormatterJar).Path
$projectRoot = Split-Path -Parent $PSScriptRoot
$excludedFiles = @(
    'CuratedDescriptions.kt',
    'CuratedReferences.kt',
    'ReferenceArt.kt',
    'TagShortNames.kt'
)

$sourceFiles = foreach ($module in @('app', 'core')) {
    Get-ChildItem -LiteralPath (Join-Path $projectRoot $module) -Recurse -File |
        Where-Object {
            $_.Extension -in @('.kt', '.kts') -and
            $_.FullName -notmatch '[\\/]build[\\/]' -and
            $_.Name -notin $excludedFiles
        } |
        Select-Object -ExpandProperty FullName
}

$sourceFiles += Join-Path $projectRoot 'build.gradle.kts'
$sourceFiles += Join-Path $projectRoot 'settings.gradle.kts'
$formatterArguments = @('--kotlinlang-style', '--do-not-remove-unused-imports')

if ($Check) {
    $formatterArguments += @('--dry-run', '--set-exit-if-changed')
}

& java -jar $jar @formatterArguments @sourceFiles
exit $LASTEXITCODE
