$ErrorActionPreference = 'Stop'
$Project = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$Practice = Get-Content -LiteralPath "$Project/practice.json" -Raw | ConvertFrom-Json
$Tools = Join-Path $Project '.tools'
if ($env:PRACTICE_TOOLS) { $Tools = [IO.Path]::GetFullPath($env:PRACTICE_TOOLS) }
elseif (-not (Test-Path -LiteralPath "$Tools/jdk-21.0.12.1+1/bin/java.exe")) {
    $SharedTools = Join-Path (Split-Path $Project -Parent) '.tools'
    if (Test-Path -LiteralPath "$SharedTools/jdk-21.0.12.1+1/bin/java.exe") { $Tools = $SharedTools }
}
$env:JAVA_HOME = Join-Path $Tools 'jdk-21.0.12.1+1'
$Java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$MavenHome = Join-Path $Tools 'apache-maven-3.9.12'
$WildFly = Join-Path $Tools 'wildfly-39.0.1.Final'
$Repo = Join-Path $Tools 'm2'
if (-not (Test-Path -LiteralPath $Java)) { throw 'Run SETUP.cmd first (Java tools are missing).' }

function Invoke-Maven([string[]]$Goals) {
    $Classworlds = (Get-ChildItem -LiteralPath "$MavenHome/boot" -Filter 'plexus-classworlds-*.jar' | Select-Object -First 1).FullName
    # Java is invoked directly: no cmd.exe re-parsing of Cyrillic paths or spaces.
    & $Java "-Dmaven.home=$MavenHome" "-Dmaven.multiModuleProjectDirectory=$Project" "-Dclassworlds.conf=$MavenHome/bin/m2.conf" '-Dfile.encoding=UTF-8' -cp $Classworlds org.codehaus.plexus.classworlds.launcher.Launcher "-Dmaven.repo.local=$Repo" -B @Goals
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed. See evidence/build.log.' }
}

function Invoke-Admin([string[]]$Arguments) {
    & $Java '-Dfile.encoding=UTF-8' "-Djboss.cli.config=$WildFly/bin/jboss-cli.xml" "-Djboss.home.dir=$WildFly" '-jar' "$WildFly/bin/client/jboss-cli-client.jar" '--connect' "--controller=127.0.0.1:$($Practice.managementPort)" @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'WildFly administration command failed.' }
}

function Get-OwnedProcess([string]$Name) {
    $PidFile = "$Project/runtime/$Name.pid"
    if (-not (Test-Path -LiteralPath $PidFile)) { return $null }
    $Saved = Get-Content -LiteralPath $PidFile -Raw | ConvertFrom-Json
    $Running = Get-Process -Id $Saved.id -ErrorAction SilentlyContinue
    if ($Running -and  $Running.StartTime.ToUniversalTime().Ticks -eq ([datetime]$Saved.started).ToUniversalTime().Ticks) { return $Running }
    return $null
}

function Start-LocalProcess([string]$Name, [string]$Exe, [string[]]$Arguments) {
    if (Get-OwnedProcess $Name) { return }
    # ArgumentList is joined by Start-Process on Windows; quote each value explicitly.
    $Quoted = @($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' '
    $Process = Start-Process -FilePath $Exe -ArgumentList $Quoted -WorkingDirectory $Project -WindowStyle Hidden -PassThru -RedirectStandardOutput "$Project/evidence/$Name.log" -RedirectStandardError "$Project/evidence/$Name-error.log"
    @{id=$Process.Id; started=$Process.StartTime.ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath "$Project/runtime/$Name.pid"
}

function Assert-FreePort([int]$Port) {
    $Listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
    try { $Listener.Start() } catch { throw "Port $Port is occupied. Stop the other application or change practice.json." }
    finally { $Listener.Stop() }
}

function Wait-Url([string]$Url, [string]$ProcessName) {
    for ($Attempt=0; $Attempt -lt 150; $Attempt++) {
        if (-not (Get-OwnedProcess $ProcessName)) { throw "$ProcessName stopped. Read evidence/$ProcessName-error.log." }
        try { $Response=Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2; if ($Response.StatusCode -lt 400) { return } } catch {}
        Start-Sleep -Milliseconds 400
    }
    throw "Server did not start: $Url. See evidence logs."
}
