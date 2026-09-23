$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$mysqlTool = Get-ChildItem "$projectRoot/.local/tools/mysql-*" -Directory | Select-Object -First 1
if (!$mysqlTool) { throw 'Project-local MySQL tools are required; see DEVELOPMENT.md.' }
$settingsFile = "$projectRoot/.local/settings.json"
if (!(Test-Path -LiteralPath $settingsFile)) { throw 'Local development settings are missing.' }
$settings = Get-Content -LiteralPath $settingsFile -Raw | ConvertFrom-Json
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $settings.databasePassword
    Get-Content -LiteralPath "$PSScriptRoot/migration-preflight.sql" -Raw |
        & "$($mysqlTool.FullName)/bin/mysql.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=woven --database=woven --table --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw 'Preflight failed. Check that the local database is running.' }
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $previousPassword }
}
