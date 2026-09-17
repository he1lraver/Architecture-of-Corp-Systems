. "$PSScriptRoot/env.ps1"
if ((Get-OwnedProcess 'spring') -or (Get-OwnedProcess 'wildfly')) { throw 'Stop this practice with STOP.cmd before rebuilding.' }
New-Item -ItemType Directory -Force -Path "$Project/evidence" | Out-Null
Push-Location $Project
try { Invoke-Maven @('clean','verify') | Tee-Object -FilePath "$Project/evidence/build.log" }
finally { Pop-Location }
