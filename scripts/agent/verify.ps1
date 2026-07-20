[CmdletBinding()]
param(
    [string[]]$Path,
    [switch]$Full,
    [switch]$Plan,
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$RemainingPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repoRoot

$gradlew = Join-Path $repoRoot 'gradlew.bat'

$allPaths = New-Object 'System.Collections.Generic.List[string]'
foreach ($candidate in @($Path) + @($RemainingPath)) {
    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        $allPaths.Add($candidate)
    }
}
$Path = $allPaths.ToArray()

function Add-UniqueTask {
    param(
        [System.Collections.Generic.List[string]]$TaskList,
        [string]$Task
    )

    if (-not $TaskList.Contains($Task)) {
        $TaskList.Add($Task)
    }
}

function Add-FullBuildTasks {
    param(
        [System.Collections.Generic.List[string]]$TaskList
    )

    Add-UniqueTask -TaskList $TaskList -Task 'lintDebug'
    Add-UniqueTask -TaskList $TaskList -Task 'test'
    Add-UniqueTask -TaskList $TaskList -Task 'assembleDebug'
}

$tasks = New-Object 'System.Collections.Generic.List[string]'
$needsFullBuild = $Full.IsPresent -or -not $Path -or $Path.Count -eq 0

if (-not $needsFullBuild) {
    foreach ($rawPath in $Path) {
        $normalized = ($rawPath -replace '\\', '/').TrimStart('./')

        if ($normalized -match '^(README\.md|AGENTS\.md)$' -or $normalized.StartsWith('docs/')) {
            continue
        }

        if ($normalized.StartsWith('.github/') -or $normalized.StartsWith('gradle/') -or $normalized -eq 'build.gradle.kts' -or $normalized -eq 'settings.gradle.kts' -or $normalized -eq 'gradle.properties' -or $normalized -eq 'local.properties') {
            $needsFullBuild = $true
            break
        }

        if ($normalized.StartsWith('app/')) {
            $needsFullBuild = $true
            break
        }

        if ($normalized.StartsWith('core/navigation/') -or $normalized.StartsWith('core/designsystem/') -or $normalized.StartsWith('core/database/') -or $normalized.StartsWith('core/testing/')) {
            $needsFullBuild = $true
            break
        }

        if ($normalized.StartsWith('feature/assets/') -or $normalized.StartsWith('feature/templates/') -or $normalized.StartsWith('feature/issues/') -or $normalized.StartsWith('feature/reports/')) {
            $needsFullBuild = $true
            break
        }

        if ($normalized.StartsWith('feature/dashboard/')) {
            Add-UniqueTask -TaskList $tasks -Task ':feature:dashboard:testDebugUnitTest'
            continue
        }

        if ($normalized.StartsWith('feature/inspection/')) {
            Add-UniqueTask -TaskList $tasks -Task ':feature:inspection:testDebugUnitTest'
            continue
        }

        if ($normalized.StartsWith('domain/')) {
            Add-UniqueTask -TaskList $tasks -Task ':domain:test'
            continue
        }

        if ($normalized.StartsWith('data/')) {
            Add-UniqueTask -TaskList $tasks -Task ':data:testDebugUnitTest'
            continue
        }

        $needsFullBuild = $true
        break
    }
}

if ($needsFullBuild) {
    $tasks.Clear()
    Add-FullBuildTasks -TaskList $tasks
}

if ($tasks.Count -eq 0) {
    Write-Host 'No Gradle verification selected for docs-only changes.'
    exit 0
}

if ($Plan.IsPresent) {
    Write-Host 'Selected Gradle verification:'
    foreach ($task in $tasks) {
        Write-Host "- $task"
    }
    exit 0
}

foreach ($task in $tasks) {
    Write-Host "Running $task"
    & $gradlew $task --no-daemon
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
