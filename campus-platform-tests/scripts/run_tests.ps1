param(
  [string]$TargetBaseUrl = "",
  [string]$ApiBaseUrl = "",
  [ValidateSet("", "smoke", "blackbox", "contract", "deployment", "negative", "frontend", "business", "api", "rbac", "security", "performance", "full")]
  [string]$Profile = "",
  [switch]$Strict
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if ($TargetBaseUrl) {
  $env:TARGET_BASE_URL = $TargetBaseUrl
}
if ($ApiBaseUrl) {
  $env:API_BASE_URL = $ApiBaseUrl
}
if ($Strict) {
  $env:STRICT_MODE = "true"
}

$ProfileTargets = @{
  smoke = @("tests/test_00_smoke.py", "tests/test_smoke_matrix.py")
  blackbox = @("tests/test_blackbox_deployment.py")
  contract = @("tests/test_contract_quality.py")
  deployment = @("tests/test_http_quality.py")
  negative = @("tests/test_negative_inputs.py")
  frontend = @("tests/test_frontend_routes.py")
  business = @("tests/test_business_modules.py")
  api = @("tests/test_api_public_contract.py", "tests/test_api_authenticated.py")
  rbac = @("tests/test_rbac_permissions.py")
  security = @("tests/test_security_baseline.py")
  performance = @("tests/test_performance_baseline.py", "tests/test_resilience_baseline.py")
  full = @("tests")
}

$TestTargets = @("tests")
if ($Profile) {
  $TestTargets = $ProfileTargets[$Profile]
}

if (!(Test-Path ".venv")) {
  python -m venv .venv
}

& ".\.venv\Scripts\python.exe" -m pip install -r requirements.txt

$ProfileDir = if ($Profile) { $Profile } else { "full" }
$ReportDir = Join-Path (Join-Path $Root "reports") $ProfileDir
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$ReportPath = Join-Path $ReportDir ("campus-test-report-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".html")

$ArgsList = @()
$ArgsList += $TestTargets
$ArgsList += @("-p", "campus_tests.pytest_dashboard", "--html=$ReportPath", "--self-contained-html")

& ".\.venv\Scripts\python.exe" -m pytest @ArgsList
Write-Host "HTML report: $ReportPath"
Write-Host "Visual dashboard: $ReportDir\latest-dashboard.html"
Write-Host "Live progress page: $ReportDir\latest-live.html"
Write-Host "Reports index: $Root\reports\index.html"
