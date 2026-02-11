# 简化版API测试脚本
Write-Host "=== 文件共享系统API测试 ===" -ForegroundColor Green

$baseUrl = "http://localhost:8080"
$headers = @{
    "Content-Type" = "application/json"
    "Accept" = "application/json"
}

# 测试1: 健康检查
Write-Host "`n1. 测试健康检查..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/actuator/health" -Method GET -Headers $headers -UseBasicParsing
    Write-Host "✓ 健康检查成功" -ForegroundColor Green
    Write-Host "响应状态: $($response.StatusCode)"
} catch {
    Write-Host "✗ 健康检查失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试2: 用户注册
Write-Host "`n2. 测试用户注册..." -ForegroundColor Yellow
$userData = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
    department = "IT部门"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method POST -Body $userData -Headers $headers -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ 用户注册成功" -ForegroundColor Green
    Write-Host "用户ID: $($result.data.id)"
} catch {
    Write-Host "✗ 用户注册失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试3: 用户登录
Write-Host "`n3. 测试用户登录..." -ForegroundColor Yellow
$loginData = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginData -Headers $headers -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    $token = $result.data.token
    Write-Host "✓ 用户登录成功" -ForegroundColor Green
    Write-Host "Token获取成功"
    
    # 设置认证头
    $authHeaders = $headers.Clone()
    $authHeaders.Authorization = "Bearer $token"
    
} catch {
    Write-Host "✗ 用户登录失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试4: 获取用户信息
Write-Host "`n4. 测试获取用户信息..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/users/profile" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ 获取用户信息成功" -ForegroundColor Green
    Write-Host "用户名: $($result.data.username)"
} catch {
    Write-Host "✗ 获取用户信息失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试5: 创建文件夹
Write-Host "`n5. 测试创建文件夹..." -ForegroundColor Yellow
$folderData = @{
    name = "测试文件夹"
    parentId = $null
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/files/folders" -Method POST -Body $folderData -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    $folderId = $result.data.id
    Write-Host "✓ 创建文件夹成功" -ForegroundColor Green
    Write-Host "文件夹ID: $folderId"
} catch {
    Write-Host "✗ 创建文件夹失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试6: 文件上传准备
Write-Host "`n6. 测试文件上传准备..." -ForegroundColor Yellow
$uploadData = @{
    filename = "test.txt"
    size = 1024
    contentType = "text/plain"
    folderId = $folderId
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/files/upload/initiate" -Method POST -Body $uploadData -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ 文件上传准备成功" -ForegroundColor Green
    Write-Host "上传ID: $($result.data.uploadId)"
} catch {
    Write-Host "✗ 文件上传准备失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试7: 搜索功能
Write-Host "`n7. 测试搜索功能..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/search?keyword=测试" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ 搜索功能成功" -ForegroundColor Green
    Write-Host "找到结果数: $($result.data.totalElements)"
} catch {
    Write-Host "✗ 搜索功能失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试8: 统计信息
Write-Host "`n8. 测试统计信息..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/statistics/user" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ 获取统计信息成功" -ForegroundColor Green
    Write-Host "总文件数: $($result.data.totalFiles)"
} catch {
    Write-Host "✗ 获取统计信息失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试9: AI文本分类
Write-Host "`n9. 测试AI文本分类..." -ForegroundColor Yellow
$textData = @{
    text = "这是一个技术文档"
    categories = @("技术", "商务", "个人")
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/ai/classify-text" -Method POST -Body $textData -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ AI文本分类成功" -ForegroundColor Green
    if ($result.data.success) {
        Write-Host "分类结果: $($result.data.category)"
    }
} catch {
    Write-Host "✗ AI文本分类失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green
Write-Host "如果所有测试都显示绿色勾号，则系统运行正常！" -ForegroundColor Cyan