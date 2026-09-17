. "$PSScriptRoot/env.ps1"
foreach ($Name in 'wildfly','spring','mailpit') {
    $Process = Get-OwnedProcess $Name
    if (-not $Process) { continue }
    if ($Name -eq 'wildfly') {
        Invoke-Admin @('--command=:shutdown')
        if (-not $Process.WaitForExit(30000)) { throw 'WildFly has not stopped yet. See evidence logs.' }
    } else {
        Stop-Process -Id $Process.Id
        $Process.WaitForExit()
    }
    Remove-Item -LiteralPath "$Project/runtime/$Name.pid" -ErrorAction SilentlyContinue
}
Write-Host 'This practice is stopped. Its database and evidence are preserved.'
