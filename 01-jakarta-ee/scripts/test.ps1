$ErrorActionPreference='Stop'
$Project=Split-Path $PSScriptRoot -Parent
$PythonArgs=@()
if ($env:PRACTICE_PYTHON) {
    $Python=$env:PRACTICE_PYTHON
} else {
    $Command=Get-Command python -CommandType Application -ErrorAction SilentlyContinue |
        Where-Object { $_.Source -notmatch '\\WindowsApps\\' } | Select-Object -First 1
    if ($Command) {
        $Python=$Command.Source
    } else {
        $Command=Get-Command py -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $Command) { throw 'Python 3 is required for TEST.cmd. Install Python or set PRACTICE_PYTHON to python.exe.' }
        $Python=$Command.Source
        $PythonArgs=@('-3')
    }
}
$env:PYTHONIOENCODING='utf-8'
& $Python @PythonArgs "$PSScriptRoot/integration_test.py"
if ($LASTEXITCODE -ne 0) { throw 'HTTP acceptance checks failed. See evidence/integration-results.json.' }
