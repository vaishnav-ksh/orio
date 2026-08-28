@echo off
set JAVA_HOME=C:\Users\Admin\.jdks\openjdk-25
set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"

echo ==========================================================
echo  Starting Product Inventory Management (ORIO) System...
echo  Java: OpenJDK 25 ^| Database: SQLite (./data/inventory.db)
echo  Web UI: http://localhost:8080
echo ==========================================================

%MVN_CMD% spring-boot:run
