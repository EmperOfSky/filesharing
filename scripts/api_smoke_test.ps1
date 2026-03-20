param(
  [string]$EndpointFile = 'api_endpoints.tsv',
  [string]$BackendBase = 'http://localhost:8080',
  [string]$ProxyBase = 'http://localhost:3001',
  [string]$OutputCsv = 'api_smoke_results.csv',
  [int]$RequestTimeoutSec = 2
)

$ErrorActionPreference = 'Continue'

if (-not (Test-Path $EndpointFile)) {
  Write-Output "ENDPOINT_FILE_NOT_FOUND $EndpointFile"
  exit 1
}

function Invoke-Test {
  param(
    [string]$Url,
    [string]$Method,
    [string]$Body
  )

  try {
    if ($Method -in @('POST', 'PUT', 'PATCH')) {
      $resp = Invoke-WebRequest -Uri $Url -Method $Method -ContentType 'application/json' -Body $Body -TimeoutSec $RequestTimeoutSec -UseBasicParsing
    } else {
      $resp = Invoke-WebRequest -Uri $Url -Method $Method -TimeoutSec $RequestTimeoutSec -UseBasicParsing
    }
    return [int]$resp.StatusCode
  } catch {
    if ($_.Exception.Response) {
      try { return [int]$_.Exception.Response.StatusCode.value__ } catch {}
      try { return [int]$_.Exception.Response.StatusCode } catch {}
    }
    return -1
  }
}

$rows = Get-Content $EndpointFile | Where-Object { $_.Trim() -ne '' }
$results = @()

foreach ($line in $rows) {
  $parts = $line -split "`t"
  if ($parts.Count -lt 4) { continue }

  $method = $parts[0].Trim()
  $pathRaw = $parts[1].Trim()
  $controller = $parts[2].Trim()
  $source = $parts[3].Trim()

  $path = [Regex]::Replace($pathRaw, '\{[^}]+\}', '1')
  $body = '{}'

  $backendUrl = $BackendBase + $path
  $proxyUrl = $ProxyBase + $path

  $backendStatus = Invoke-Test -Url $backendUrl -Method $method -Body $body
  $proxyStatus = Invoke-Test -Url $proxyUrl -Method $method -Body $body

  $backendReachable = ($backendStatus -ne -1 -and $backendStatus -ne 404)
  $proxyReachable = ($proxyStatus -ne -1 -and $proxyStatus -ne 404)

  $results += [PSCustomObject]@{
    Method = $method
    Path = $pathRaw
    TestedPath = $path
    Controller = $controller
    Source = $source
    BackendStatus = $backendStatus
    ProxyStatus = $proxyStatus
    BackendReachable = $backendReachable
    ProxyReachable = $proxyReachable
    BothReachable = ($backendReachable -and $proxyReachable)
  }
}

$results | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8

$total = $results.Count
$both = ($results | Where-Object { $_.BothReachable }).Count
$backendOnly = ($results | Where-Object { $_.BackendReachable -and -not $_.ProxyReachable }).Count
$proxyOnly = ($results | Where-Object { -not $_.BackendReachable -and $_.ProxyReachable }).Count
$none = ($results | Where-Object { -not $_.BackendReachable -and -not $_.ProxyReachable }).Count
$notBoth = $results | Where-Object { -not $_.BothReachable }

Write-Output "TOTAL $total"
Write-Output "BOTH_REACHABLE $both"
Write-Output "BACKEND_ONLY $backendOnly"
Write-Output "PROXY_ONLY $proxyOnly"
Write-Output "NONE $none"

if ($notBoth.Count -gt 0) {
  Write-Output "NOT_BOTH_SAMPLE_BEGIN"
  $notBoth | Select-Object -First 20 Method, Path, BackendStatus, ProxyStatus, BackendReachable, ProxyReachable | Format-Table -AutoSize | Out-String | Write-Output
  Write-Output "NOT_BOTH_SAMPLE_END"
}
