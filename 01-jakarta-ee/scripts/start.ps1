. "$PSScriptRoot/env.ps1"
New-Item -ItemType Directory -Force -Path "$Project/runtime/db", "$Project/evidence" | Out-Null
$ProcessName = if ($Practice.number -eq 2) { 'spring' } else { 'wildfly' }
$Extension = if ($Practice.number -eq 2) { 'jar' } else { 'war' }
if (-not (Test-Path -LiteralPath "$Project/target/library.$Extension")) { & "$PSScriptRoot/build.ps1" }
$AppUrl = if ($Practice.number -eq 2) { "http://127.0.0.1:$($Practice.httpPort)/" } else { "http://127.0.0.1:$($Practice.httpPort)/library/ui" }
if (Get-OwnedProcess $ProcessName) {
    Wait-Url $AppUrl $ProcessName
    Write-Host "READY (already running): $AppUrl"
    return
}
Assert-FreePort $Practice.httpPort
if ($Practice.number -ne 2) { Assert-FreePort $Practice.managementPort }
if ($Practice.number -eq 4 -and -not (Get-OwnedProcess 'mailpit')) {
    Assert-FreePort $Practice.mailPort
    Assert-FreePort $Practice.smtpPort
}
$Driver = if ($Practice.number -eq 2) {
    (Get-ChildItem "$Repo/com/h2database/h2/*/h2-*.jar" | Sort-Object FullName | Select-Object -Last 1).FullName
} else { (Get-ChildItem "$WildFly/modules/system/layers/base/com/h2database/h2/main/h2-*.jar").FullName }
if (-not $Driver) { throw 'H2 driver is missing. Run BUILD.cmd first.' }
# Relative URLs remain valid when a stopped practice is moved to another directory.
$DbUrl = 'jdbc:h2:file:./runtime/db/library;DB_CLOSE_ON_EXIT=FALSE'
if (-not (Test-Path -LiteralPath "$Project/runtime/db/initialized")) {
    $Sql = (Get-Content -LiteralPath "$Project/database/schema.sql" -Raw) + "`n" + (Get-Content -LiteralPath "$Project/database/seed.sql" -Raw)
    [IO.File]::WriteAllText("$Project/runtime/initialize.sql", $Sql, [Text.UTF8Encoding]::new($false))
    Push-Location $Project
    try {
        & $Java -cp $Driver org.h2.tools.RunScript -url $DbUrl -user sa -password practice -script 'runtime/initialize.sql'
        if ($LASTEXITCODE -ne 0) { throw 'Database initialization failed.' }
        [IO.File]::WriteAllText("$Project/runtime/db/initialized", 'initialized')
    } finally { Pop-Location }
}
if ($Practice.number -eq 2) {
    Start-LocalProcess 'spring' $Java @('-Xms64m','-Xmx384m','-Dfile.encoding=UTF-8','-jar',"$Project/target/library.jar","--server.port=$($Practice.httpPort)")
} else {
    if (-not (Test-Path -LiteralPath "$Project/runtime/server/configuration/standalone.xml")) {
        New-Item -ItemType Directory -Force -Path "$Project/runtime/server/configuration" | Out-Null
        Get-ChildItem -LiteralPath "$Project/server" -File | Copy-Item -Destination "$Project/runtime/server/configuration"
    }
    $ServerArgs = @('-Xms128m','-Xmx512m','-Dfile.encoding=UTF-8',"-Djboss.home.dir=$WildFly", "-Djboss.server.base.dir=$Project/runtime/server")
    if ($Practice.number -eq 4) {
        Start-LocalProcess 'mailpit' "$Tools/mailpit.exe" @('--listen',"127.0.0.1:$($Practice.mailPort)",'--smtp',"127.0.0.1:$($Practice.smtpPort)",'--database',"$Project/runtime/mailpit.db")
        $Mail = Get-Content -LiteralPath "$Project/mail.properties" | Where-Object { $_ -match '^library\.mail\.' }
        $ServerArgs += @($Mail | ForEach-Object { '-D' + $_ })
        $ServerArgs += "-Dlibrary.mail.port=$($Practice.smtpPort)"
    }
    $ServerArgs += @('-jar',"$WildFly/jboss-modules.jar",'-mp',"$WildFly/modules",'org.jboss.as.standalone','-c','standalone.xml','-b','127.0.0.1','-bmanagement','127.0.0.1',"-Djboss.socket.binding.port-offset=$($Practice.offset)")
    Start-LocalProcess 'wildfly' $Java $ServerArgs
    Wait-Url "http://127.0.0.1:$($Practice.httpPort)/" 'wildfly'
    $Commands = @"
if (outcome != success) of /subsystem=datasources/data-source=LibraryDS:read-resource
  data-source add --name=LibraryDS --jndi-name=java:/jdbc/LibraryDS --driver-name=h2 --connection-url="$DbUrl" --user-name=sa --password=practice
end-if
"@
    if ($Practice.number -eq 4) {
        $Commands += @"

if (outcome != success) of /subsystem=messaging-activemq/server=default/jms-topic=LibraryChanges:read-resource
  /subsystem=messaging-activemq/server=default/jms-topic=LibraryChanges:add(entries=["java:/jms/topic/LibraryChanges"])
end-if
"@
    }
    $Commands += "`ndeploy `"target/library.war`" --force`n"
    [IO.File]::WriteAllText("$Project/runtime/configure.cli", $Commands, [Text.UTF8Encoding]::new($false))
    Push-Location $Project
    try { Invoke-Admin @('--file=runtime/configure.cli') }
    finally { Pop-Location }
}
Wait-Url $AppUrl $ProcessName
Write-Host "READY: $AppUrl"
if ($Practice.number -eq 4) { Write-Host "Local mail: http://127.0.0.1:$($Practice.mailPort)/" }
