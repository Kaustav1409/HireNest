$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$cp = (Get-ChildItem "target\jar-libs\*.jar" | ForEach-Object { $_.FullName }) -join ";"
$cp = "$cp;BOOT-INF/classes;target/classes"

$srcFiles = @(
  "src/main/java/com/hirenest/backend/entity/Job.java",
  "src/main/java/com/hirenest/backend/entity/JobApplication.java",
  "src/main/java/com/hirenest/backend/repository/JobRepository.java",
  "src/main/java/com/hirenest/backend/repository/JobApplicationRepository.java",
  "src/main/java/com/hirenest/backend/repository/UserRepository.java",
  "src/main/java/com/hirenest/backend/repository/QuizAttemptRepository.java",
  "src/main/java/com/hirenest/backend/dto/JobDtos.java",
  "src/main/java/com/hirenest/backend/dto/ApplicationDtos.java",
  "src/main/java/com/hirenest/backend/mapper/ApplicationMapper.java",
  "src/main/java/com/hirenest/backend/service/JobService.java",
  "src/main/java/com/hirenest/backend/service/JobSeekerActionService.java",
  "src/main/java/com/hirenest/backend/service/ApplicationStrengthService.java",
  "src/main/java/com/hirenest/backend/controller/JobController.java",
  "src/main/java/com/hirenest/backend/controller/JobSeekerController.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobSeed.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobCatalog.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobSeeder.java"
) | ForEach-Object { Join-Path $root $_ }
Write-Host "Compiling $($srcFiles.Count) Java sources..."
New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
& javac -parameters -encoding UTF-8 -cp $cp -d "target/classes" @(
  "src/main/java/com/hirenest/backend/dto/JobDtos.java",
  "src/main/java/com/hirenest/backend/dto/ApplicationDtos.java",
  "src/main/java/com/hirenest/backend/entity/Job.java",
  "src/main/java/com/hirenest/backend/entity/JobApplication.java",
  "src/main/java/com/hirenest/backend/repository/JobRepository.java",
  "src/main/java/com/hirenest/backend/repository/JobApplicationRepository.java",
  "src/main/java/com/hirenest/backend/repository/UserRepository.java",
  "src/main/java/com/hirenest/backend/repository/QuizAttemptRepository.java",
  "src/main/java/com/hirenest/backend/mapper/ApplicationMapper.java",
  "src/main/java/com/hirenest/backend/service/JobService.java",
  "src/main/java/com/hirenest/backend/service/JobSeekerActionService.java",
  "src/main/java/com/hirenest/backend/service/ApplicationStrengthService.java",
  "src/main/java/com/hirenest/backend/controller/JobController.java",
  "src/main/java/com/hirenest/backend/controller/JobSeekerController.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobSeed.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobCatalog.java",
  "src/main/java/com/hirenest/backend/seed/PlatformJobSeeder.java"
)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Compile OK"

$dashCp = (Get-ChildItem "target\jar-libs\*.jar" | ForEach-Object { $_.FullName }) -join ";"
$dashCp = "$dashCp;target\jar-rebuild\BOOT-INF\classes;target\classes"
& javac -parameters -encoding UTF-8 -cp $dashCp -d "target/classes" `
  "src/main/java/com/hirenest/backend/util/ProfileCompletionUtil.java" `
  "src/main/java/com/hirenest/backend/service/DashboardService.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "DashboardService + ProfileCompletionUtil compile OK"

$compileOut = "target\compile-out"
New-Item -ItemType Directory -Force -Path $compileOut | Out-Null
$compileCp = (Get-ChildItem "target\jar-libs\*.jar" | ForEach-Object { $_.FullName }) -join ";"
if (Test-Path "target\jar-rebuild\BOOT-INF\classes") {
  $compileCp = "$compileCp;target\jar-rebuild\BOOT-INF\classes"
} else {
  $compileCp = "$compileCp;BOOT-INF/classes"
}
& javac -parameters -encoding UTF-8 -cp $compileCp -d $compileOut @(
  "src/main/java/com/hirenest/backend/service/AuthService.java",
  "src/main/java/com/hirenest/backend/dto/AuthDtos.java",
  "src/main/java/com/hirenest/backend/service/QuizService.java"
)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "AuthService + QuizService compile OK"

Copy-Item "src\main\resources\static\*" "target\classes\static\" -Recurse -Force -ErrorAction SilentlyContinue

$rebuild = "target\jar-rebuild"
if (-not (Test-Path "$rebuild\BOOT-INF\classes")) {
  Write-Host "Extracting base JAR into $rebuild ..."
  New-Item -ItemType Directory -Force -Path $rebuild | Out-Null
  Push-Location $rebuild
  jar xf "..\hirenest-backend-0.0.1-SNAPSHOT.jar"
  Pop-Location
  if (Test-Path "$rebuild\com") { Remove-Item "$rebuild\com" -Recurse -Force }
  if (Test-Path "$rebuild\static") { Remove-Item "$rebuild\static" -Recurse -Force }
}
$bootClasses = "$rebuild\BOOT-INF\classes"
$overlays = @(
  "entity\Job.class","entity\JobApplication.class",
  "repository\JobRepository.class","repository\JobApplicationRepository.class","repository\UserRepository.class","repository\QuizAttemptRepository.class",
  "dto\JobDtos*.class","dto\ApplicationDtos*.class",
  "mapper\ApplicationMapper.class",
  "dto\ApplicationDtos.class",
  "service\JobService.class","service\JobSeekerActionService.class","service\ApplicationStrengthService.class",
  "controller\JobController.class","controller\JobSeekerController.class",
  "seed\PlatformJobSeed.class","seed\PlatformJobCatalog.class",  "seed\PlatformJobSeeder.class",
  "util\ProfileCompletionUtil.class",
  "service\DashboardService.class"
)
foreach ($p in $overlays) {
  Get-ChildItem "target\classes\com\hirenest\backend\$p" -ErrorAction SilentlyContinue | ForEach-Object {
    $rel = $_.FullName.Substring((Resolve-Path "target\classes\com\hirenest\backend").Path.Length + 1)
    $dest = Join-Path "$bootClasses\com\hirenest\backend" $rel
    New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
    Copy-Item $_.FullName $dest -Force
  }
}
Get-ChildItem "$compileOut\com\hirenest\backend\service\AuthService*.class" -ErrorAction SilentlyContinue |
  Copy-Item -Destination "$bootClasses\com\hirenest\backend\service\" -Force
Get-ChildItem "$compileOut\com\hirenest\backend\service\QuizService*.class" -ErrorAction SilentlyContinue |
  Copy-Item -Destination "$bootClasses\com\hirenest\backend\service\" -Force
Get-ChildItem "$compileOut\com\hirenest\backend\dto\AuthDtos*.class" -ErrorAction SilentlyContinue |
  Copy-Item -Destination "$bootClasses\com\hirenest\backend\dto\" -Force
Copy-Item "src\main\resources\static\*" "$bootClasses\static\" -Recurse -Force
Push-Location $rebuild
jar -cfM0 "..\hirenest-backend-0.0.1-SNAPSHOT.jar" *
Pop-Location
Write-Host "Repacked target\hirenest-backend-0.0.1-SNAPSHOT.jar (BOOT-INF/classes only)"
