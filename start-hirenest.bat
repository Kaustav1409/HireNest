@echo off
cd /d "%~dp0"
if not exist "target\hirenest-backend-0.0.1-SNAPSHOT.jar" (
  echo JAR not found. Build the project first.
  pause
  exit /b 1
)
if not exist "data" mkdir data
echo Starting HireNest at http://localhost:9090
echo Database: %~dp0data\hirenest_db.mv.db
echo Press Ctrl+C to stop. Do not force-kill java.exe.
java -jar target\hirenest-backend-0.0.1-SNAPSHOT.jar
