param(
    [ValidateSet('Local', 'Preview', 'Play', 'All')]
    [string]$Variant = 'All',
    [switch]$RequireUploadKey
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if ($RequireUploadKey) {
        foreach ($name in @('GAMEREEL_UPLOAD_STORE', 'GAMEREEL_UPLOAD_STORE_PASSWORD', 'GAMEREEL_UPLOAD_KEY_ALIAS', 'GAMEREEL_UPLOAD_KEY_PASSWORD')) {
            if (-not [Environment]::GetEnvironmentVariable($name)) { throw "Missing signing configuration: $name" }
        }
        if (-not (Test-Path -LiteralPath $env:GAMEREEL_UPLOAD_STORE -PathType Leaf)) { throw 'Upload keystore not found' }
    }
    if ($Variant -in @('Local', 'All')) {
        & .\gradlew.bat :app:androidApp:assembleRelease --console=plain --max-workers=2
        if ($LASTEXITCODE) { throw 'Local build failed' }
    }
    if ($Variant -ne 'Local') {
        # Compose resource copy tasks can retain files removed by a new exclusion rule.
        # Only the isolated build/play directories are cleaned; Local outputs stay intact.
        & .\gradlew.bat :app:shared:clean :app:androidApp:clean -PplayRelease=true --console=plain
        if ($LASTEXITCODE) { throw 'Public output cleanup failed' }
        $tasks = @()
        if ($Variant -in @('Preview', 'All')) { $tasks += ':app:androidApp:assemblePreview' }
        if ($Variant -in @('Play', 'All')) { $tasks += ':app:androidApp:bundlePlay' }
        & .\gradlew.bat @tasks -PplayRelease=true --console=plain --max-workers=2
        if ($LASTEXITCODE) { throw 'Public build failed' }
        if ($Variant -in @('Preview', 'All')) {
            & python tools/check_play_artifact.py app/androidApp/build/play/outputs/apk/preview/androidApp-preview.apk
            if ($LASTEXITCODE) { throw 'Preview content verification failed' }
        }
        if ($Variant -in @('Play', 'All')) {
            & python tools/check_play_artifact.py app/androidApp/build/play/outputs/bundle/play/androidApp-play.aab
            if ($LASTEXITCODE) { throw 'Play content verification failed' }
        }
    }
} finally { Pop-Location }
