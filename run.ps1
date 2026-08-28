# Set Java and Maven Paths
$env:JAVA_HOME = "C:\Users\Admin\.jdks\openjdk-25"
$mvnPath = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting Product Inventory Management (ORIO) System...  " -ForegroundColor Cyan
Write-Host " Java: OpenJDK 25 | Database: SQLite (./data/inventory.db)" -ForegroundColor Green
Write-Host " Web UI: http://localhost:8080                          " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

& $mvnPath spring-boot:run
