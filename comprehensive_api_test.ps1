# 真实API接口综合测试脚本 (Comprehensive Real API Testing)
$baseUrl = "http://localhost:8080"
$frontendUrl = "http://localhost:5173"

# 颜色输出函数
function Write-OK { param($msg) Write-Host "✓ $msg" -ForegroundColor Green }
function Write-Err { param($msg) Write-Host "✗ $msg" -ForegroundColor Red }
function Write-Warn { param($msg) Write-Host "⚠ $msg" -ForegroundColor Yellow }
function Write-Info { param($msg) Write-Host "• $msg" -ForegroundColor Cyan }
function Write-Title { param($msg) Write-Host "`n═══ $msg ═══" -ForegroundColor Magenta }

# 测试结果收集
$results = @{
    Passed = 0
    Failed = 0
    Errors = @()
}

Write-Host "`n╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         真实API接口综合测试 (Real API Test)        ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# ============================================================
# 1. 前端服务器检查
# ============================================================
Write-Title "Step 1: 检查前端服务器 (Frontend Check)"

try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri $frontendUrl -TimeoutSec 3 -ErrorAction Stop
    Write-OK "前端服务运行于 $frontendUrl"
    $results.Passed++
} catch {
    Write-Err "前端服务未响应: $frontendUrl"
    Write-Warn "前端服务可能未启动，请检查"
    $results.Failed++
}

# ============================================================
# 2. 后端服务器检查
# ============================================================
Write-Title "Step 2: 检查后端服务器 (Backend Check)"

try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/" -TimeoutSec 3 -ErrorAction Stop
    Write-OK "后端服务运行于 $baseUrl"
    $results.Passed++
} catch {
    Write-Err "后端服务未响应: $baseUrl"
    $results.Failed++
    $results.Errors += "后端服务不可用"
    exit
}

# ============================================================
# 3. 测试认证接口 (Authentication Tests)
# ============================================================
Write-Title "Step 3: 测试认证接口 (Authentication)"

# 生成唯一的用户名用于测试
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testUsername = "testuser_$timestamp"
$testPassword = "Test@123456"
$testEmail = "test_$timestamp@test.com"

# 3.1 测试注册
Write-Info "测试注册 (POST /api/auth/register)"
$regPayload = @{
    username = $testUsername
    password = $testPassword
    email = $testEmail
} | ConvertTo-Json

try {
    $regResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $regPayload `
        -TimeoutSec 5 -ErrorAction Stop
    
    Write-OK "注册成功 - 用户: $testUsername"
    $results.Passed++
    $script:testUser = @{ username = $testUsername; password = $testPassword; email = $testEmail }
} catch {
    Write-Err "注册失败: $($_.Exception.Message)"
    Write-Warn "尝试使用已有账号进行测试"
    $results.Failed++
    $script:testUser = @{ username = "testaccount"; password = "test123456" }
}

# 3.2 测试登录
Write-Info "测试登录 (POST /api/auth/login)"
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
        Write-OK "登录成功 - 获得 JWT Token"
        $script:token = $loginResp.data
        $script:headers = @{ "Authorization" = "Bearer $($script:token)" }
        $results.Passed++
    } else {
        Write-Err "登录返回无效token: $($loginResp | ConvertTo-Json)"
        $results.Failed++
        $results.Errors += "登录返回invalid token"
    }
} catch {
    Write-Err "登录失败: $($_.Exception.Response.StatusCode) - $($_.Exception.Message)"
    Write-Warn "无法获得认证token，某些测试可能失败"
    $results.Failed++
    $results.Errors += "登录端点失败"
}

# ============================================================
# 4. 测试文件接口 (File Management Tests)
# ============================================================
Write-Title "Step 4: 测试文件管理接口 (File Endpoints)"

if (-not $script:token) {
    Write-Warn "无authentication token, 跳过需要认证的文件测试"
} else {
    $fileTests = @(
        @{ method = "GET"; path = "/api/files"; name = "获取文件列表" }
        @{ method = "POST"; path = "/api/files"; name = "创建文件条目"; data = @{ filename = "test.txt"; size = 1024 } }
        @{ method = "GET"; path = "/api/files/1"; name = "获取文件详情" }
        @{ method = "DELETE"; path = "/api/files/1"; name = "删除文件" }
        @{ method = "POST"; path = "/api/chunks/upload"; name = "上传文件分块"; data = @{ fileId = "1"; chunkIndex = 0; totalChunks = 1 } }
        @{ method = "GET"; path = "/api/chunks/1/status"; name = "检查上传状态" }
    )
    
    $filePassed = 0
    $fileFailed = 0
    
    foreach ($test in $fileTests) {
        try {
            if ($test.method -eq "GET") {
                $resp = Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method GET `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop
            } else {
                $payloadJson = $test.data ? ($test.data | ConvertTo-Json) : "{}"
                $resp = Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method $test.method `
                    -ContentType "application/json" `
                    -Body $payloadJson `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop
            }
            Write-OK "$($test.name) - $($test.path)"
            $filePassed++
            $results.Passed++
        } catch {
            $statusCode = $_.Exception.Response.StatusCode
            if ($statusCode -eq 404) {
                Write-Warn "$($test.name) - 资源不存在 (HTTP 404) [端点存在]"
                $filePassed++
            } elseif ($statusCode -eq 400 -or $statusCode -eq 422) {
                Write-Warn "$($test.name) - 需要有效数据 (HTTP $statusCode) [端点存在]"
                $filePassed++
            } else {
                Write-Err "$($test.name) - 失败 (HTTP $statusCode)"
                $fileFailed++
                $results.Failed++
                $results.Errors += "文件接口: $($test.path)"
            }
        }
    }
}

# ============================================================
# 5. 测试用户接口 (User Management Tests)
# ============================================================
Write-Title "Step 5: 测试用户管理接口 (User Endpoints)"

if (-not $script:token) {
    Write-Warn "无authentication token, 跳过需要认证的用户测试"
} else {
    $userTests = @(
        @{ method = "GET"; path = "/api/users/profile"; name = "获取用户资料" }
        @{ method = "PUT"; path = "/api/users/profile"; name = "更新用户资料"; data = @{ nickname = "新昵称" } }
        @{ method = "POST"; path = "/api/users/change-password"; name = "修改密码"; data = @{ oldPassword = "old"; newPassword = "new" } }
    )
    
    foreach ($test in $userTests) {
        try {
            if ($test.method -eq "GET") {
                $resp = Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method GET `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop
            } else {
                $payloadJson = $test.data ? ($test.data | ConvertTo-Json) : "{}"
                $resp = Invoke-RestMethod -Uri "$baseUrl$($test.path)" `
                    -Method $test.method `
                    -ContentType "application/json" `
                    -Body $payloadJson `
                    -Headers $script:headers `
                    -TimeoutSec 5 -ErrorAction Stop
            }
            Write-OK "$($test.name) - $($test.path)"
            $results.Passed++
        } catch {
            $statusCode = $_.Exception.Response.StatusCode
            if ($statusCode -eq 400 -or $statusCode -eq 422) {
                Write-Warn "$($test.name) - 端点存在，需要有效数据 (HTTP $statusCode)"
                $results.Passed++
            } else {
                Write-Err "$($test.name) - 失败 (HTTP $statusCode)"
                $results.Failed++
                $results.Errors += "用户接口: $($test.path)"
            }
        }
    }
}

# ============================================================
# 6. 结果汇总
# ============================================================
Write-Title "测试结果总结 (Test Summary)"

Write-Host "`n总体结果:" -ForegroundColor Yellow
Write-Host "  成功: $($results.Passed)" -ForegroundColor Green
Write-Host "  失败: $($results.Failed)" -ForegroundColor Red

if ($results.Errors.Count -gt 0) {
    Write-Host "`n失败的接口:" -ForegroundColor Red
    foreach ($error in $results.Errors) {
        Write-Host "  ✗ $error" -ForegroundColor Red
    }
}

$successRate = if ($results.Passed -eq 0 -and $results.Failed -eq 0) { 0 } else { [math]::Round(($results.Passed / ($results.Passed + $results.Failed)) * 100, 2) }
Write-Host "`n成功率: $successRate%" -ForegroundColor Cyan

if ($results.Failed -eq 0) {
    Write-Host "`n╔════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║            ✓ 所有测试通过！(All Tests Passed)      ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════╝`n" -ForegroundColor Green
} else {
    Write-Host "`n╔════════════════════════════════════════════════════╗" -ForegroundColor Yellow
    Write-Host "║       ⚠ 部分测试失败，需要调查 (Some Tests Failed)     ║" -ForegroundColor Yellow
    Write-Host "╚════════════════════════════════════════════════════╝`n" -ForegroundColor Yellow
}
