# todo-cli setup script for Windows
# Run with:
# powershell -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/guranxp/todo-cli/main/setup.ps1 | iex"

$ErrorActionPreference = "Stop"
$installDir = "$env:USERPROFILE\todo"

Write-Host ""
Write-Host "=== todo-cli setup ===" -ForegroundColor Cyan

# Check Java 21+
Write-Host ""
Write-Host "Checking Java..." -ForegroundColor Yellow
$javaOk = $false
try {
    $version = (java -version 2>&1 | Select-String "version").ToString()
    if ($version -match '"(21|2[2-9]|[3-9]\d)\.') {
        Write-Host "Java 21+ found." -ForegroundColor Green
        $javaOk = $true
    }
} catch {}

if (-not $javaOk) {
    Write-Host "Java 21 not found. Installing (user scope, no admin needed)..." -ForegroundColor Yellow
    try {
        winget install Microsoft.OpenJDK.21 --scope user --accept-package-agreements --accept-source-agreements
        Write-Host "Java installed." -ForegroundColor Green

        # Refresh PATH so java is available in this session
        $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "User") + ";" + $env:PATH
    } catch {
        Write-Host "Could not install Java automatically." -ForegroundColor Red
        Write-Host "Please install Java 21 manually from https://adoptium.net and re-run this script." -ForegroundColor Yellow
        exit 1
    }
}

# Create install directory
Write-Host ""
Write-Host "Installing to $installDir ..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $installDir | Out-Null

# Download latest todo.jar
Write-Host "Downloading todo.jar..." -ForegroundColor Yellow
$jarUrl = "https://github.com/guranxp/todo-cli/releases/latest/download/todo.jar"
Invoke-WebRequest -Uri $jarUrl -OutFile "$installDir\todo.jar"

# Create todo.bat
@"
@echo off
mode con: cols=120 lines=24
java -jar "%~dp0todo.jar"
"@ | Set-Content "$installDir\todo.bat"

# Create Start menu shortcut
Write-Host "Creating Start menu shortcut..." -ForegroundColor Yellow
$startMenu = "$env:APPDATA\Microsoft\Windows\Start Menu\Programs"
$shortcutPath = "$startMenu\todo.lnk"
$wsh = New-Object -ComObject WScript.Shell
$shortcut = $wsh.CreateShortcut($shortcutPath)

$wtExe = (Get-Command wt.exe -ErrorAction SilentlyContinue)?.Source
if ($wtExe) {
    $shortcut.TargetPath = $wtExe
    $shortcut.Arguments = "cmd /c `"$installDir\todo.bat`""
} else {
    $shortcut.TargetPath = "cmd.exe"
    $shortcut.Arguments = "/c `"$installDir\todo.bat`""
}
$shortcut.WorkingDirectory = $installDir
$shortcut.Description = "todo-cli"
$shortcut.Save()

Write-Host ""
Write-Host "Done! todo-cli installed to $installDir" -ForegroundColor Green
Write-Host "Start menu shortcut created — search for 'todo' in the Start menu." -ForegroundColor Green
Write-Host ""
