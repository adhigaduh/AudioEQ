param(
    [Parameter(ValueFromRemainingArguments)]
    [string[]]$GradleArgs
)

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\adhig\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

if ($GradleArgs.Count -eq 0) {
    Write-Host "Usage: .\setenv.ps1 <gradle-command>"
    Write-Host "Example: .\setenv.ps1 test"
    Write-Host ""
    Write-Host "JAVA_HOME: $env:JAVA_HOME"
    Write-Host "ANDROID_HOME: $env:ANDROID_HOME"
    & "$env:JAVA_HOME\bin\java.exe" -version
} else {
    $gradlewPath = Join-Path $PSScriptRoot "gradlew.bat"
    Push-Location $PSScriptRoot
    try {
        & $gradlewPath $GradleArgs
    } finally {
        Pop-Location
    }
}
