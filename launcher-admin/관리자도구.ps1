param(
    [Parameter(Position = 0)]
    [ValidateSet('도움말', '검사', '설정적용', '초기화', '동기화', '배포생성', '배포검증', '런처검사', '런처빌드')]
    [string]$명령 = '도움말'
)

$ErrorActionPreference = 'Stop'
$AdminRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $AdminRoot
$LauncherRoot = Join-Path $WorkspaceRoot 'launcher'
$NebulaRoot = Join-Path $WorkspaceRoot 'launcher-tools\nebula'
$PackRoot = Join-Path $AdminRoot 'pack'
$ConfigPath = Join-Path $AdminRoot 'admin-config.json'
$JavaPath = Join-Path $WorkspaceRoot '.toolchains\jdk8\jdk8u504-b01\bin\java.exe'

function Read-AdminConfig {
    $config = Get-Content -LiteralPath $ConfigPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ([string]$config.server.id -notmatch '^[a-zA-Z0-9._-]+$') {
        throw 'server.id에는 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.'
    }
    return $config
}

function Invoke-Npm {
    param([string[]]$Arguments, [string]$WorkingDirectory)
    Push-Location $WorkingDirectory
    try {
        & npm @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "npm 명령 실패 (종료 코드: $LASTEXITCODE)"
        }
    }
    finally {
        Pop-Location
    }
}

function Write-NebulaEnvironment {
    $config = Read-AdminConfig
    $baseUrl = [string]$config.hosting.baseUrl
    if (-not $baseUrl.EndsWith('/')) { $baseUrl += '/' }
    $content = @(
        "JAVA_EXECUTABLE=$JavaPath"
        "ROOT=$PackRoot"
        "BASE_URL=$baseUrl"
        "HELIOS_DATA_FOLDER=$AdminRoot\dev-data"
    ) -join [Environment]::NewLine
    Set-Content -LiteralPath (Join-Path $NebulaRoot '.env') -Value $content -Encoding UTF8
}

function Initialize-Pack {
    $config = Read-AdminConfig
    $serverFolder = Join-Path $PackRoot "servers\$($config.server.id)-$($config.server.minecraftVersion)"
    if (-not (Test-Path (Join-Path $PackRoot 'meta\distrometa.json'))) {
        Write-NebulaEnvironment
        Invoke-Npm -WorkingDirectory $NebulaRoot -Arguments @('run', 'start', '--', 'init', 'root')
    }
    if (-not (Test-Path $serverFolder)) {
        Write-NebulaEnvironment
        Invoke-Npm -WorkingDirectory $NebulaRoot -Arguments @(
            'run', 'start', '--', 'generate', 'server',
            $config.server.id,
            $config.server.minecraftVersion,
            '--forge', $config.server.forgeVersion
        )
    }
    Update-PackMetadata
}

function Update-PackMetadata {
    $config = Read-AdminConfig
    $metaDir = Join-Path $PackRoot 'meta'
    $serverFolder = Join-Path $PackRoot "servers\$($config.server.id)-$($config.server.minecraftVersion)"
    New-Item -ItemType Directory -Force -Path $metaDir, $serverFolder | Out-Null

    $distroMeta = [ordered]@{
        meta = [ordered]@{}
    }
    $distroMeta | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $metaDir 'distrometa.json') -Encoding UTF8

    $serverMeta = [ordered]@{
        meta = [ordered]@{
            version = [string]$config.server.packVersion
            name = [string]$config.server.name
            description = [string]$config.server.description
            icon = ''
            address = [string]$config.server.address
            mainServer = $true
            autoconnect = [bool]($config.server.autoConnect -and $config.server.addressConfigured)
            javaOptions = [ordered]@{
                supported = '8.x'
                suggestedMajor = 8
                distribution = 'TEMURIN'
                ram = [ordered]@{
                    minimum = [int]$config.server.minimumMemoryMb
                    recommended = [int]$config.server.recommendedMemoryMb
                }
            }
        }
        forge = [ordered]@{
            version = [string]$config.server.forgeVersion
        }
        untrackedFiles = @(
            [ordered]@{
                appliesTo = @('files')
                patterns = @('options.txt', 'servers.dat')
            }
        )
    }
    $serverMeta | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $serverFolder 'servermeta.json') -Encoding UTF8
}

function Sync-DirectoryContents {
    param([string]$Source, [string]$Destination, [string]$Filter = '*')
    $resolvedPackRoot = [IO.Path]::GetFullPath($PackRoot).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $resolvedDestination = [IO.Path]::GetFullPath($Destination).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedDestination.StartsWith($resolvedPackRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "배포 작업 폴더 밖의 경로는 동기화할 수 없습니다: $Destination"
    }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Get-ChildItem -LiteralPath $Destination -File -Recurse -ErrorAction SilentlyContinue | Remove-Item -Force
    if (Test-Path $Source) {
        Get-ChildItem -LiteralPath $Source -File -Recurse -Filter $Filter | ForEach-Object {
            $relative = [IO.Path]::GetRelativePath($Source, $_.FullName)
            $target = Join-Path $Destination $relative
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
            Copy-Item -LiteralPath $_.FullName -Destination $target -Force
        }
    }
}

function Sync-PackContent {
    Initialize-Pack
    $config = Read-AdminConfig
    $serverFolder = Join-Path $PackRoot "servers\$($config.server.id)-$($config.server.minecraftVersion)"
    Sync-DirectoryContents -Source (Join-Path $AdminRoot 'mods\required') -Destination (Join-Path $serverFolder 'forgemods\required') -Filter '*.jar'
    Sync-DirectoryContents -Source (Join-Path $AdminRoot 'mods\optional-on') -Destination (Join-Path $serverFolder 'forgemods\optionalon') -Filter '*.jar'
    Sync-DirectoryContents -Source (Join-Path $AdminRoot 'mods\optional-off') -Destination (Join-Path $serverFolder 'forgemods\optionaloff') -Filter '*.jar'
    Sync-DirectoryContents -Source (Join-Path $AdminRoot 'files') -Destination (Join-Path $serverFolder 'files')
    Remove-Item -LiteralPath (Join-Path $serverFolder 'files\README.md') -Force -ErrorAction SilentlyContinue
    Write-Host '모드와 클라이언트 파일 동기화 완료'
}

switch ($명령) {
    '도움말' {
        Write-Host '1억 기부 프로젝트 런처 관리자 도구'
        Write-Host '  검사       관리자 설정의 오류와 미설정 항목 확인'
        Write-Host '  설정적용   런처 이름·주소·로그인 ID 등을 소스에 적용'
        Write-Host '  초기화     배포 폴더와 Forge 1.12.2 서버 정의 생성'
        Write-Host '  동기화     mods/files 내용을 배포 작업 폴더로 복사'
        Write-Host '  배포생성   Forge 라이브러리와 파일 해시를 포함한 distribution.json 생성'
        Write-Host '  배포검증   매니페스트의 모든 파일 크기와 MD5 검사'
        Write-Host '  런처검사   코드 스타일 및 관리자 설정 검사'
        Write-Host '  런처빌드   Windows 설치 프로그램 생성'
    }
    '검사' {
        & node (Join-Path $AdminRoot 'scripts\validate-admin.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    '설정적용' {
        & node (Join-Path $AdminRoot 'scripts\apply-config.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    '초기화' { Initialize-Pack }
    '동기화' { Sync-PackContent }
    '배포생성' {
        Sync-PackContent
        Write-NebulaEnvironment
        $distributionPath = Join-Path $PackRoot 'distribution.json'
        Remove-Item -LiteralPath $distributionPath -Force -ErrorAction SilentlyContinue
        Invoke-Npm -WorkingDirectory $NebulaRoot -Arguments @('run', 'start', '--', 'generate', 'distro')
        if (-not (Test-Path $distributionPath)) {
            throw '배포 도구가 distribution.json을 생성하지 못했습니다. 위 오류 로그를 확인하세요.'
        }
        Write-Host "배포 매니페스트 생성 완료: $distributionPath"
        & node (Join-Path $AdminRoot 'scripts\create-online-feed.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    '배포검증' {
        & node (Join-Path $AdminRoot 'scripts\verify-distribution.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    '런처검사' {
        & node (Join-Path $AdminRoot 'scripts\validate-admin.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & node (Join-Path $AdminRoot 'scripts\apply-config.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Invoke-Npm -WorkingDirectory $LauncherRoot -Arguments @('run', 'lint')
    }
    '런처빌드' {
        & node (Join-Path $AdminRoot 'scripts\apply-config.cjs')
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Invoke-Npm -WorkingDirectory $LauncherRoot -Arguments @('run', 'dist:win')
        Write-Host "Windows 설치 프로그램 생성 완료: $LauncherRoot\dist"
    }
}
