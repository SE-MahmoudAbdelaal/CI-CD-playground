<#
.SYNOPSIS
    Replaces the running app container with a new image and checks that it works.

.DESCRIPTION
    The CD workflow runs this on the self-hosted runner, and you can also run it
    by hand. The new version must report UP on /actuator/health and answer
    GET /api/todos before the timeout. If it does not, it is removed and the
    version that was running before is started again.

.EXAMPLE
    .\deploy.ps1 -Image ghcr.io/se-mahmoudabdelaal/ci-cd-demo:sha-24405c0
#>
param(
    [Parameter(Mandatory = $true)] [string] $Image,
    [string] $Name = "todo-api",
    [int] $Port = 8080,
    [int] $TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

# Runs docker and stops the script if docker reports a failure
function Invoke-Docker {
    & docker @args
    if ($LASTEXITCODE -ne 0) { throw "docker $($args -join ' ') failed with exit code $LASTEXITCODE" }
}

# The image of the app container (running or stopped), or nothing if there is none
function Get-AppImage {
    & docker ps --all --filter "name=^/?${Name}$" --format "{{.Image}}"
}

function Start-App([string] $AppImage) {
    # The image tag (e.g. sha-24405c0) becomes the version that GET /api/health reports
    $version = if ($AppImage -match ':([\w][\w.-]*)$') { $Matches[1] } else { $AppImage }
    Invoke-Docker run --detach --name $Name --restart unless-stopped --publish "${Port}:8080" `
        --env "APP_VERSION=$version" $AppImage | Out-Null
}

function Remove-App {
    if (Get-AppImage) { Invoke-Docker rm --force $Name | Out-Null }
}

# True once the health endpoint reports UP and the API answers; false after the timeout
function Test-App {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            # Ask for plain JSON: Windows PowerShell 5.1 returns Spring's own actuator
            # content type as raw bytes instead of text
            $health = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" -UseBasicParsing -TimeoutSec 5 `
                -Headers @{ Accept = "application/json" }
            $todos = Invoke-WebRequest -Uri "http://localhost:$Port/api/todos" -UseBasicParsing -TimeoutSec 5
            if ($health.Content -match '"status"\s*:\s*"UP"' -and $todos.StatusCode -eq 200) { return $true }
        } catch {
            # Not ready yet: try again
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

# Adds a line to the run's summary page when running inside GitHub Actions
function Write-Summary([string] $Text) {
    if ($env:GITHUB_STEP_SUMMARY) { Add-Content -Path $env:GITHUB_STEP_SUMMARY -Value $Text -Encoding Ascii }
}

$previous = Get-AppImage
Write-Host "Deploying $Image (running before: $(if ($previous) { $previous } else { 'nothing' }))"
Remove-App
try {
    Start-App $Image
    $healthy = Test-App
} catch {
    Write-Host $_.Exception.Message
    $healthy = $false
}

if ($healthy) {
    Write-Host "$Image is up at http://localhost:$Port"
    Write-Summary "### Deployed`n`n**$Image** is running on this PC: http://localhost:$Port/api/todos"
    exit 0
}

Write-Host "::error::$Image did not become healthy within $TimeoutSeconds seconds"
Write-Host "Last log lines of the new version:"
$ErrorActionPreference = "Continue"
& docker logs --tail 40 $Name
$ErrorActionPreference = "Stop"
Remove-App
if ($previous) {
    Write-Host "Rolling back to $previous"
    Start-App $previous
    if (Test-App) {
        Write-Host "::warning::Rolled back: $previous is running again"
        Write-Summary "### Deployment failed, rolled back`n`n**$Image** did not become healthy, so **$previous** was started again."
    } else {
        Write-Host "::error::The previous version $previous is not healthy either"
    }
}
exit 1
