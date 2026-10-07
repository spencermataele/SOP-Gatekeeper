$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$settings = Get-Content "$root/.local/settings.json" -Raw | ConvertFrom-Json
$mysql = (Get-ChildItem "$root/.local/tools/mysql-*/bin/mysql.exe" | Select-Object -First 1).FullName
$credentialPath = "$root/.local/workflow-demo.json"
$previousPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $settings.databasePassword
function Sql([string]$query) {
    $output = $query | & $mysql --no-defaults --host=127.0.0.1 --port=3307 --user=woven --database=woven --batch --skip-column-names --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw 'Local demo SQL failed.' }
    return $output
}
function TextSql([string]$value) {
    if (!$value) { return "''" }
    return 'CONVERT(0x' + [Convert]::ToHexString([Text.Encoding]::UTF8.GetBytes($value)) + ' USING utf8mb4)'
}
try {
    $client = Sql 'SELECT org_id FROM client_configuration WHERE configuration_id=1;'
    if ($client -and "$client" -ne '1') { throw 'This local database is configured for another client. No changes made.' }
    if ((Sql "SELECT COUNT(*) FROM department d JOIN org_group g USING(org_group_id) WHERE d.department_id=10 AND g.org_id=1;") -ne '1') { throw 'Expected local development hierarchy is missing.' }
    if (!(Test-Path $credentialPath)) {
        if ((Sql "SELECT COUNT(*) FROM users WHERE username IN ('demo.author','demo.owner','demo.manager','demo.admin');") -ne '0') { throw 'Demo usernames already exist without the local credential file. Refusing to overwrite accounts.' }
        @{ password = [Guid]::NewGuid().ToString('N'); users = @('demo.author','demo.owner','demo.manager','demo.admin') } | ConvertTo-Json | Set-Content $credentialPath
    }
    $credentials = Get-Content $credentialPath -Raw | ConvertFrom-Json
    foreach ($role in @('author','owner','manager','admin')) {
        $username = "demo.$role"
        if ((Sql "SELECT COUNT(*) FROM users WHERE username='$username';") -eq '0') {
            $body = @{username=$username; password=$credentials.password; email="$username@example.invalid"; fullName="Practice $role"} | ConvertTo-Json
            Invoke-RestMethod http://localhost:8080/auth/register -Method Post -ContentType application/json -Body $body | Out-Null
        }
        $login = @{username=$username; password=$credentials.password} | ConvertTo-Json
        Invoke-RestMethod http://localhost:8080/auth/login -Method Post -ContentType application/json -Body $login | Out-Null
    }
    Sql @'
START TRANSACTION;
INSERT IGNORE INTO client_configuration VALUES (1,1);
UPDATE users SET role='ADMIN' WHERE username='demo.admin';
SET @owner=(SELECT id FROM users WHERE username='demo.owner');
SET @manager=(SELECT id FROM users WHERE username='demo.manager');
INSERT INTO user_reporting_line (user_id,manager_user_id) SELECT @owner,@manager WHERE NOT EXISTS (SELECT 1 FROM user_reporting_line WHERE user_id=@owner);
INSERT INTO business_process_family (business_process_family_name,department_id,dept_subgroup_id)
SELECT 'Gatekeeper learning',10,(SELECT MIN(dept_subgroup_id) FROM dept_subgroup WHERE department_id=10) WHERE NOT EXISTS (SELECT 1 FROM business_process_family WHERE business_process_family_name='Gatekeeper learning' AND department_id=10);
SET @family=(SELECT MIN(business_process_family_id) FROM business_process_family WHERE business_process_family_name='Gatekeeper learning' AND department_id=10);
INSERT INTO business_process (business_process_name,business_process_family_id)
SELECT 'Practice workflows',@family WHERE NOT EXISTS (SELECT 1 FROM business_process WHERE business_process_name='Practice workflows' AND business_process_family_id=@family);
SET @process=(SELECT MIN(business_process_id) FROM business_process WHERE business_process_name='Practice workflows' AND business_process_family_id=@family);
INSERT INTO process_governance (business_process_id,owner_user_id) SELECT @process,@owner WHERE NOT EXISTS (SELECT 1 FROM process_governance WHERE business_process_id=@process);
COMMIT;
'@ | Out-Null
    $process = Sql "SELECT MIN(p.business_process_id) FROM business_process p JOIN business_process_family f USING(business_process_family_id) WHERE p.business_process_name='Practice workflows' AND f.business_process_family_name='Gatekeeper learning' AND f.department_id=10;"
    $admin = Sql "SELECT id FROM users WHERE username='demo.admin';"
    $guides = Get-Content "$root/docs/tutorials/workflow-guides.json" -Raw | ConvertFrom-Json
    foreach ($guide in $guides) {
        $key = TextSql $guide.key; $title = TextSql $guide.title; $purpose = TextSql $guide.purpose; $steps = TextSql ($guide.content | ConvertTo-Json -Depth 10 -Compress)
        Sql @"
START TRANSACTION;
INSERT INTO sop_document (org_id,business_process_id,document_kind,product_key)
SELECT 1,$process,'PRODUCT_GUIDE',$key WHERE NOT EXISTS (SELECT 1 FROM sop_document WHERE product_key=$key);
SET @doc=(SELECT document_id FROM sop_document WHERE product_key=$key FOR UPDATE);
SET @checksum=SHA2(CONVERT(JSON_ARRAY($title,$purpose,$steps,2) USING utf8mb4),256);
INSERT INTO sop_revision (document_id,submitted_by_id,title,description,details,content_schema_version,content_checksum,provenance,source_label)
SELECT @doc,$admin,$title,$purpose,$steps,2,@checksum,'PRODUCT_RELEASE','Structured template preview 2026-09'
WHERE NOT EXISTS (SELECT 1 FROM sop_document d JOIN sop_revision r ON r.revision_id=d.current_revision_id WHERE d.document_id=@doc AND r.content_checksum=@checksum);
SET @revision=(SELECT MAX(revision_id) FROM sop_revision WHERE document_id=@doc);
SET @position=(SELECT COALESCE(MAX(ordering_key),0)+1024 FROM revision_history_position WHERE document_id=@doc);
INSERT INTO revision_history_position (document_id,revision_id,ordering_key)
SELECT @doc,@revision,@position WHERE NOT EXISTS (SELECT 1 FROM revision_history_position WHERE revision_id=@revision);
UPDATE sop_document SET current_revision_id=@revision WHERE document_id=@doc;
COMMIT;
"@ | Out-Null
    }
    Write-Host 'Local workflow practice is ready. Accounts: demo.author, demo.owner, demo.manager, demo.admin.'
    Write-Host "Password is in $credentialPath (ignored by Git). Existing MVP content is preserved."
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue } else { $env:MYSQL_PWD = $previousPassword }
}
