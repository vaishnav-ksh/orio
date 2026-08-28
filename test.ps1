# Set Java and Maven Paths
$env:JAVA_HOME = "C:\Users\Admin\.jdks\openjdk-25"
$mvnPath = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"

Write-Host "Running JUnit 5 Test Suite..." -ForegroundColor Cyan
& $mvnPath test
