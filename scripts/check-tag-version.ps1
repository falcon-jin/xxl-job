param(
    [Parameter(Mandatory = $true)]
    [string]$Tag
)

$pomPath = Join-Path $PSScriptRoot "..\pom.xml"
if (-not (Test-Path $pomPath)) {
    Write-Error "Root pom.xml not found at $pomPath"
    exit 1
}

[xml]$pom = Get-Content -Path $pomPath
$projectVersion = $pom.project.version

if ([string]::IsNullOrWhiteSpace($projectVersion)) {
    Write-Error "Unable to read project version from $pomPath"
    exit 1
}

$normalizedTag = $Tag
if ($normalizedTag.StartsWith("refs/tags/")) {
    $normalizedTag = $normalizedTag.Substring(10)
}
if ($normalizedTag.StartsWith("v")) {
    $normalizedTag = $normalizedTag.Substring(1)
}

if ([string]::IsNullOrWhiteSpace($normalizedTag)) {
    Write-Error "Tag '$Tag' does not contain a usable version"
    exit 1
}

if ($normalizedTag -ne $projectVersion) {
    Write-Error "Tag version '$normalizedTag' does not match project version '$projectVersion'"
    exit 1
}

Write-Output "Tag version '$normalizedTag' matches project version '$projectVersion'"
