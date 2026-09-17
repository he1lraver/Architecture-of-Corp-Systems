param([string]$ToolsDirectory)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$Project = Split-Path $PSScriptRoot -Parent
$Tools = if ($ToolsDirectory) { [IO.Path]::GetFullPath($ToolsDirectory) } else { Join-Path $Project '.tools' }
New-Item -ItemType Directory -Force -Path $Tools, "$Tools/downloads", "$Project/evidence" | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Install-Zip($Name, $Url, $Hash, $Algorithm = 'SHA256') {
    $Marker = Join-Path $Tools "$Name.installed"
    if (Test-Path -LiteralPath $Marker) { return }
    $Archive = Join-Path "$Tools/downloads" "$Name.zip"
    if (-not (Test-Path -LiteralPath $Archive)) {
        Write-Host "Downloading $Name"
        & curl.exe -fL --retry 3 --silent --show-error $Url -o $Archive
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $Name" }
    }
    if ((Get-FileHash -LiteralPath $Archive -Algorithm $Algorithm).Hash -ne $Hash.Trim()) {
        throw "Checksum mismatch: $Name"
    }
    $Marker = Join-Path $Tools "$Name.installed"
    if (-not (Test-Path -LiteralPath $Marker)) {
        $Zip = [System.IO.Compression.ZipFile]::OpenRead($Archive)
        foreach ($Entry in $Zip.Entries) {
            $Target = [IO.Path]::GetFullPath((Join-Path $Tools $Entry.FullName))
            if (-not $Target.StartsWith([IO.Path]::GetFullPath($Tools) + [IO.Path]::DirectorySeparatorChar)) {
                throw "Unsafe archive entry: $($Entry.FullName)"
            }
        }
        $Zip.Dispose()
        [System.IO.Compression.ZipFile]::ExtractToDirectory($Archive, $Tools)
        New-Item -ItemType File -Path $Marker | Out-Null
    }
}

Install-Zip 'jdk21' 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip' 'f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e'
$MavenUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip'
Install-Zip 'maven' $MavenUrl (Invoke-RestMethod "$MavenUrl.sha1") 'SHA1'
$WildFlyUrl = 'https://repo.maven.apache.org/maven2/org/wildfly/wildfly-dist/39.0.1.Final/wildfly-dist-39.0.1.Final.zip'
Install-Zip 'wildfly' $WildFlyUrl (Invoke-RestMethod "$WildFlyUrl.sha1") 'SHA1'
$Mail = (& curl.exe -fsSL --retry 3 'https://api.github.com/repos/axllent/mailpit/releases/tags/v1.31.1') | ConvertFrom-Json
$Asset = $Mail.assets | Where-Object name -eq 'mailpit-windows-amd64.zip'
if (-not $Asset.digest.StartsWith('sha256:')) { throw 'Mailpit SHA256 not available' }
Install-Zip 'mailpit' $Asset.browser_download_url $Asset.digest.Substring(7)
Write-Host 'Portable tools installed. Run BUILD.cmd, then START.cmd.'
