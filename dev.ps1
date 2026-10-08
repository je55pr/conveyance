param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments = @('assemble')
)

$ErrorActionPreference = 'Stop'
$taskPreviousJavaHome = $env:JAVA_HOME
$taskLocalJdk = Get-ChildItem -LiteralPath "$PSScriptRoot\.tools\java" -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath "$($_.FullName)\bin\javac.exe" } |
    Select-Object -First 1

try {
    if ($taskLocalJdk) {
        $env:JAVA_HOME = $taskLocalJdk.FullName
    }
    Push-Location $PSScriptRoot
    try {
        & "$PSScriptRoot\gradlew.bat" @GradleArguments
        $taskExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $taskPreviousJavaHome
}
exit $taskExitCode

