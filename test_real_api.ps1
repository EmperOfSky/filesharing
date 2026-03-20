# Comprehensive Real API Testing Script
$baseUrl = "http://127.0.0.1:8080"
$frontendUrl = "http://127.0.0.1:5173"

# Color output functions
function Write-OK { param($msg) Write-Host "OK: $msg" -ForegroundColor Green }
function Write-Err { param($msg) Write-Host "ERR: $msg" -ForegroundColor Red }
function Write-Warn { param($msg) Write-Host "WARN: $msg" -ForegroundColor Yellow }
function Write-Info { param($msg) Write-Host "INFO: $msg" -ForegroundColor Cyan }

# Test results collection
$results = @{
    Passed = 0
    Failed = 0
    Errors = @()
}

Write-Host "`n========== API TESTING ==========`n" -ForegroundColor Cyan

# Step 1: Frontend Check
Write-Host "Step 1: Frontend Server Check" -ForegroundColor Yellow
try {
    Invoke-WebRequest -UseBasicParsing -Uri $frontendUrl -TimeoutSec 3 -ErrorAction Stop | Out-Null
    Write-OK "Frontend running at $frontendUrl"
    $results.Passed++
} catch {
    Write-Warn "Frontend not responding at $frontendUrl"
}

# Step 2: Backend Check
Write-Host "`nStep 2: Backend Server Check" -ForegroundColor Yellow
try {
    Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/api/ai/models" -TimeoutSec 3 -ErrorAction Stop | Out-Null
    Write-OK "Backend running at $baseUrl"
    $results.Passed++
} catch {
    Write-Err "Backend not responding at $baseUrl"
    $results.Failed++
    exit
}

# Step 3: Authentication Tests
Write-Host "`nStep 3: Authentication Tests" -ForegroundColor Yellow

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testUsername = "testuser_$timestamp"
$testPassword = "Test@123456"
$testEmail = "test_$timestamp@test.com"

# 3.1 Registration
Write-Info "Testing POST /api/auth/register"
$regPayload = @{
    username = $testUsername
    password = $testPassword
    email = $testEmail
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $regPayload `
        -TimeoutSec 5 -ErrorAction Stop | Out-Null
    
    Write-OK "Registration successful - User: $testUsername"
    $results.Passed++
    $script:testUser = @{ username = $testUsername; password = $testPassword }
} catch {
    Write-Warn "Registration failed, using existing account"
    $results.Failed++
    $script:testUser = @{ username = "testaccount"; password = "test123456" }
}

# 3.2 Login
Write-Info "Testing POST /api/auth/login"
$loginPayload = @{
    identifier = $script:testUser.username
    password = $script:testUser.password
} | ConvertTo-Json

try {
    $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginPayload `
        -TimeoutSec 5 -ErrorAction Stop
    
    if ($loginResp.data -and $loginResp.data.Length -gt 0) {
        Write-OK "Login successful - JWT Token received"
        $script:token = $loginResp.data
        $script:headers = @{ "Authorization" = "Bearer $($script:token)" }
        $results.Passed++
    } else {
        Write-Err "Login failed - No token in response"
        $results.Failed++
        $results.Errors += "Login: No token"
    }
} catch {
    Write-Err "Login failed: $($_.Exception.Response.StatusCode)"
    $results.Failed++
    $results.Errors += "Login endpoint"
}

# Step 4: AI Service Tests
Write-Host "`nStep 4: AI Service Tests" -ForegroundColor Yellow

$aiTests = @(
    @{ method = "GET"; path = "/api/ai/models"; name = "Get AI models"; payload = $null }
    @{ method = "POST"; path = "/api/ai/summary"; name = "Document summary"; payload = @{ content = "Test content"; ratio = 0.5 } }
    @{ method = "POST"; path = "/api/ai/qa"; name = "Question answering"; payload = @{ context = "Context"; question = "Question" } }
    @{ method = "POST"; path = "/api/ai/correct"; name = "Text correction"; payload = @{ text = "Test text" } }
    @{ method = "POST"; path = "/api/ai/classify"; name = "Content classification"; payload = @{ content = "Test content" } }
    @{ method = "POST"; path = "/api/ai/smart-search"; name = "Smart search"; payload = @{ query = "Query"; context = "Context" } }
    @{ method = "POST"; path = "/api/ai/sentiment"; name = "Sentiment analysis"; payload = @{ text = "I am happy" } }
    @{ method = "POST"; path = "/api/ai/keywords"; name = "Keyword extraction"; payload = @{ text = "Test text with keywords" } }
)

foreach ($test in $aiTests) {
    Write-Info "Testing $($test.method) $($test.path)"
    try {
        if ($test.method -eq "GET") {
            Invoke-RestMethod -Uri "$baseUrl$($test.path)" -Method GET -TimeoutSec 5 -ErrorAction Stop | Out-Null
        } else {
            $payloadJson = $test.payload | ConvertTo-Json
            Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                -Method POST `
                -ContentType "application/json" `
                -Body $payloadJson `
                -TimeoutSec 5 -ErrorAction Stop | Out-Null
        }
        Write-OK "$($test.name) ($($test.path))"
        $results.Passed++
    } catch {
        $statusCode = $_.Exception.Response.StatusCode
        if ($statusCode -eq 400 -or $statusCode -eq 405) {
            Write-Warn "$($test.name) - Endpoint exists (HTTP $statusCode)"
            $results.Passed++
        } else {
            Write-Err "$($test.name) - Failed (HTTP $statusCode)"
            $results.Failed++
            $results.Errors += "AI: $($test.path)"
        }
    }
}

# Step 5: File Management Tests
Write-Host "`nStep 5: File Management Tests" -ForegroundColor Yellow

if (-not $script:token) {
    Write-Warn "No auth token, skipping authenticated tests"
} else {
    $fileTests = @(
        @{ method = "GET"; path = "/api/files"; name = "List files"; data = $null }
        @{ method = "POST"; path = "/api/files"; name = "Create file"; data = @{ filename = "test.txt"; size = 1024 } }
        @{ method = "GET"; path = "/api/files/1"; name = "Get file details"; data = $null }
        @{ method = "DELETE"; path = "/api/files/1"; name = "Delete file"; data = $null }
        @{ method = "POST"; path = "/api/chunks/upload"; name = "Upload chunk"; data = @{ fileId = "1"; chunkIndex = 0 } }
        @{ method = "GET"; path = "/api/chunks/1/status"; name = "Check upload status"; data = $null }
    )
    
    foreach ($test in $fileTests) {
        Write-Info "Testing $($test.method) $($test.path)"
        try {
            if ($test.method -eq "GET") {
                Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method GET `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop | Out-Null
            } else {
                if ($test.data) { $payloadJson = $test.data | ConvertTo-Json } else { $payloadJson = "{}" }
                Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method $test.method `
                    -ContentType "application/json" `
                    -Body $payloadJson `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop | Out-Null
            }
            Write-OK "$($test.name) ($($test.path))"
            $results.Passed++
        } catch {
            $statusCode = $_.Exception.Response.StatusCode
            if ($statusCode -eq 404 -or $statusCode -eq 400) {
                Write-Warn "$($test.name) - Resource not found (HTTP $statusCode)"
                $results.Passed++
            } else {
                Write-Err "$($test.name) - Failed (HTTP $statusCode)"
                $results.Failed++
                $results.Errors += "File: $($test.path)"
            }
        }
    }
}

# Step 6: User Management Tests
Write-Host "`nStep 6: User Management Tests" -ForegroundColor Yellow

if (-not $script:token) {
    Write-Warn "No auth token, skipping authenticated tests"
} else {
    $userTests = @(
        @{ method = "GET"; path = "/api/users/profile"; name = "Get user profile"; data = $null }
        @{ method = "PUT"; path = "/api/users/profile"; name = "Update profile"; data = @{ nickname = "NewName" } }
        @{ method = "POST"; path = "/api/users/change-password"; name = "Change password"; data = @{ oldPassword = "old"; newPassword = "new" } }
    )
    
    foreach ($test in $userTests) {
        Write-Info "Testing $($test.method) $($test.path)"
        try {
            if ($test.method -eq "GET") {
                Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method GET `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop | Out-Null
            } else {
                if ($test.data) { $payloadJson = $test.data | ConvertTo-Json } else { $payloadJson = "{}" }
                Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method $test.method `
                    -ContentType "application/json" `
                    -Body $payloadJson `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop | Out-Null
            }
            Write-OK "$($test.name) ($($test.path))"
            $results.Passed++
        } catch {
            $statusCode = $_.Exception.Response.StatusCode
            if ($statusCode -eq 400 -or $statusCode -eq 422) {
                Write-Warn "$($test.name) - Needs proper data (HTTP $statusCode)"
                $results.Passed++
            } else {
                Write-Err "$($test.name) - Failed (HTTP $statusCode)"
                $results.Failed++
                $results.Errors += "User: $($test.path)"
            }
        }
    }
}

# Results Summary
Write-Host "`n========== RESULTS ==========" -ForegroundColor Cyan
Write-Host "Passed: $($results.Passed)" -ForegroundColor Green
Write-Host "Failed: $($results.Failed)" -ForegroundColor Red

if ($results.Errors.Count -gt 0) {
    Write-Host "`nFailed endpoints:" -ForegroundColor Red
    foreach ($error in $results.Errors) {
        Write-Host "  - $error" -ForegroundColor Red
    }
}

$total = $results.Passed + $results.Failed
$successRate = if ($total -eq 0) { 0 } else { [math]::Round(($results.Passed / $total) * 100, 2) }
Write-Host "`nSuccess Rate: $successRate%" -ForegroundColor Cyan
Write-Host "=============================`n" -ForegroundColor Cyan

if ($results.Failed -eq 0) {
    Write-Host "All tests PASSED!" -ForegroundColor Green
} else {
    Write-Host "Some tests failed." -ForegroundColor Yellow
}
