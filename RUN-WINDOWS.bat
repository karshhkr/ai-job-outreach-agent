@echo off
echo Starting AI Job Outreach Agent...
echo Ensure MySQL is running and environment variables are set.
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found on PATH.
  echo Open this folder in IntelliJ as a Maven project and run spring-boot:run from the Maven panel.
  pause
  exit /b 1
)
call mvn clean spring-boot:run
pause
