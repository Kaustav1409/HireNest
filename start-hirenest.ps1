# Always start HireNest from the project folder so the database file stays in ./data/
$ProjectRoot = $PSScriptRoot
Set-Location $ProjectRoot

$Jar = Join-Path $ProjectRoot "target\hirenest-backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $Jar)) {
    Write-Error "JAR not found: $Jar"
    exit 1
}

New-Item -ItemType Directory -Force -Path (Join-Path $ProjectRoot "data") | Out-Null

Write-Host "Starting HireNest at http://localhost:9090"
Write-Host "Database file: $ProjectRoot\data\hirenest_db.mv.db"
Write-Host "Press Ctrl+C to stop the server (do not kill java.exe from Task Manager)."
Write-Host ""

java -jar $Jar
