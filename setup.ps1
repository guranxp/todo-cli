# todo-cli setup script for Windows
# Run with: irm https://raw.githubusercontent.com/guranxp/todo-cli/main/setup.ps1 | iex

$ErrorActionPreference = "Stop"
$installDir = "$env:USERPROFILE\todo"

Write-Host ""
Write-Host "=== todo-cli setup ===" -ForegroundColor Cyan

# Check Java
Write-Host ""
Write-Host "Checking Java..." -ForegroundColor Yellow
$javaOk = $false
try {
    $version = (java -version 2>&1 | Select-String "version").ToString()
    if ($version -match '"(2[1-9]|[3-9]\d)\.' -or $version -match '"21') {
        Write-Host "Java 21+ found." -ForegroundColor Green
        $javaOk = $true
    }
} catch {}

if (-not $javaOk) {
    Write-Host "Java 21 not found. Installing via winget..." -ForegroundColor Yellow
    winget install Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements
    Write-Host "Java installed. Please restart this script in a new terminal." -ForegroundColor Green
    exit 0
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

Write-Host ""
Write-Host "Done! todo-cli installed to $installDir" -ForegroundColor Green
Write-Host ""
Write-Host "To run: open Windows Terminal and type:" -ForegroundColor Cyan
Write-Host "  $installDir\todo.bat" -ForegroundColor White
Write-Host ""
