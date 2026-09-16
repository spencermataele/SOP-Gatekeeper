param(
    [ValidateSet('database', 'stop-database', 'backend', 'frontend', 'test-backend', 'test-frontend', 'build')]
    [string]$Action = 'frontend'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$local = Join-Path $root '.local'
$jdk = Get-ChildItem "$local/tools/jdk-*" -Directory | Select-Object -First 1
$mysql = Get-ChildItem "$local/tools/mysql-*" -Directory | Select-Object -First 1
if (!$jdk -or !$mysql) { throw 'Project-local Java 21 and MySQL are required; see DEVELOPMENT.md.' }
$env:JAVA_HOME = $jdk.FullName
$env:MAVEN_USER_HOME = "$local/maven"
$env:PATH = "$($jdk.FullName)/bin;$root/frontend/node;$env:PATH"
$env:NG_CLI_ANALYTICS = 'false'
$settingsPath = "$local/settings.json"
if (!(Test-Path $settingsPath)) {
    $settings = @{
        databasePassword = [Guid]::NewGuid().ToString('N')
        rootPassword = [Guid]::NewGuid().ToString('N')
        jwtSecret = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    }
    $settings | ConvertTo-Json | Set-Content $settingsPath
}
$settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3307/woven?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:SPRING_DATASOURCE_USERNAME = 'woven'
$env:SPRING_DATASOURCE_PASSWORD = $settings.databasePassword
$env:APP_JWT_SECRET = $settings.jwtSecret
$env:SERVER_ADDRESS = '127.0.0.1'
$env:PORT = '8080'
$env:SPRING_PROFILES_ACTIVE = 'default'
$repoOption = "-Dmaven.repo.local=$local/maven/repository"
$node = "$root/frontend/node/node.exe"
$ng = "$root/frontend/node_modules/@angular/cli/bin/ng.js"
switch ($Action) {
    'database' {
        $data = "$local/mysql-data"
        if (!(Test-Path $data)) {
            & "$($mysql.FullName)/bin/mysqld.exe" --no-defaults --initialize-insecure "--basedir=$($mysql.FullName)" "--datadir=$data" --console
            if ($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed.' }
            @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$($settings.rootPassword)';
CREATE DATABASE IF NOT EXISTS woven;
CREATE USER IF NOT EXISTS 'woven'@'127.0.0.1' IDENTIFIED BY '$($settings.databasePassword)';
CREATE USER IF NOT EXISTS 'woven'@'localhost' IDENTIFIED BY '$($settings.databasePassword)';
GRANT ALL PRIVILEGES ON woven.* TO 'woven'@'127.0.0.1';
GRANT ALL PRIVILEGES ON woven.* TO 'woven'@'localhost';
"@ | Set-Content "$local/mysql-init.sql" -Encoding ascii
        }
        if (Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue) {
            throw 'Port 3307 is already in use. Check the running database before starting another.'
        }
        $arguments = @('--no-defaults', "--basedir=`"$($mysql.FullName)`"", "--datadir=`"$data`"", '--bind-address=127.0.0.1', '--port=3307', '--mysqlx=OFF', '--console')
        if (Test-Path "$local/mysql-init.sql") { $arguments += "--init-file=`"$local/mysql-init.sql`"" }
        Start-Process "$($mysql.FullName)/bin/mysqld.exe" -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardOutput "$local/logs/mysql.out.log" -RedirectStandardError "$local/logs/mysql.err.log" | Out-Null
        Write-Host 'Database starting on 127.0.0.1:3307. Logs: .local/logs/mysql.err.log'
    }
    'stop-database' {
        $env:MYSQL_PWD = $settings.rootPassword
        try { & "$($mysql.FullName)/bin/mysqladmin.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=root shutdown }
        finally { Remove-Item Env:MYSQL_PWD }
    }
    'backend' { & ./backend/mvnw.cmd -f backend/pom.xml $repoOption spring-boot:run }
    'frontend' {
        Push-Location frontend
        try { & $node $ng serve --host localhost --proxy-config proxy.conf.json }
        finally { Pop-Location }
    }
    'test-backend' { & ./backend/mvnw.cmd -f backend/pom.xml $repoOption test }
    'test-frontend' {
        if (!$env:CHROME_BIN) { $env:CHROME_BIN = 'C:/Program Files/Google/Chrome/Application/chrome.exe' }
        Push-Location frontend
        try { & $node $ng test --watch=false --browsers=ChromeHeadless }
        finally { Pop-Location }
    }
    'build' { & ./backend/mvnw.cmd -f pom.xml $repoOption -DskipTests package }
}
if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
