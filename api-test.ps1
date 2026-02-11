# 文件共享系统API测试脚本
# 使用PowerShell进行API功能测试

# 基础配置
$baseUrl = "http://localhost:8080"
$headers = @{
    "Content-Type" = "application/json"
    "Accept" = "application/json"
}

Write-Host "=========================================="
Write-Host "文件共享系统API测试开始"
Write-Host "=========================================="

# 1. 健康检查
Write-Host "`n1. 健康检查测试..."
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method GET -Headers $headers -UseBasicParsing
    Write-Host "✅ 健康检查成功: $($response.StatusCode)"
} catch {
    Write-Host "❌ 健康检查失败: $($_.Exception.Message)"
}

# 2. 用户注册测试
Write-Host "`n2. 用户注册测试..."
$userData = @{
    username = "testuser$(Get-Random)"
    email = "test$(Get-Random)@example.com"
    password = "password123"
    nickname = "测试用户"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method POST -Body $userData -Headers $headers -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 用户注册成功"
    Write-Host "用户ID: $($result.data.id)"
    Write-Host "用户名: $($result.data.username)"
} catch {
    Write-Host "❌ 用户注册失败: $($_.Exception.Message)"
}

# 3. 用户登录测试
Write-Host "`n3. 用户登录测试..."
$loginData = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginData -Headers $headers -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    $token = $result.data
    Write-Host "✅ 用户登录成功"
    Write-Host "Token: $token"
    
    # 更新请求头包含认证token
    $authHeaders = $headers.Clone()
    $authHeaders.Add("Authorization", "Bearer $token")
} catch {
    Write-Host "❌ 用户登录失败: $($_.Exception.Message)"
}

# 4. 获取当前用户信息
Write-Host "`n4. 获取当前用户信息..."
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 获取用户信息成功"
    Write-Host "用户名: $($result.data.username)"
    Write-Host "邮箱: $($result.data.email)"
} catch {
    Write-Host "❌ 获取用户信息失败: $($_.Exception.Message)"
}

# 5. 文件上传测试 (创建测试文件)
Write-Host "`n5. 文件上传测试..."
$testContent = "这是一个测试文件的内容，用于验证文件上传功能。"
$testFilePath = "test-file.txt"
$testContent | Out-File -FilePath $testFilePath -Encoding UTF8

try {
    $fileBytes = [System.IO.File]::ReadAllBytes($testFilePath)
    $boundary = [System.Guid]::NewGuid().ToString()
    $authHeaders["Content-Type"] = "multipart/form-data; boundary=`"$boundary`""
    
    # 构建multipart请求体
    $bodyLines = @(
        "--$boundary",
        'Content-Disposition: form-data; name="file"; filename="test-file.txt"',
        "Content-Type: text/plain",
        "",
        $testContent,
        "--$boundary--"
    )
    
    $body = ($bodyLines -join "`r`n")
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    
    $response = Invoke-WebRequest -Uri "$baseUrl/api/files/upload" -Method POST -Body $bodyBytes -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 文件上传成功"
    Write-Host "文件ID: $($result.data.id)"
    Write-Host "文件名: $($result.data.originalName)"
} catch {
    Write-Host "❌ 文件上传失败: $($_.Exception.Message)"
}

# 6. 获取用户文件列表
Write-Host "`n6. 获取用户文件列表..."
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/files" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 获取文件列表成功"
    Write-Host "文件数量: $($result.data.Count)"
} catch {
    Write-Host "❌ 获取文件列表失败: $($_.Exception.Message)"
}

# 7. AI功能测试 - 文本分类
Write-Host "`n7. AI文本分类测试..."
$textContent = @{
    content = "这是一个技术文档，包含了Java编程和Spring框架的相关内容。"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/ai/classify-text" -Method POST -Body $textContent -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ AI文本分类成功"
    if ($result.data.success) {
        Write-Host "分类结果: $($result.data.classification.category)"
        Write-Host "置信度: $($result.data.confidence)"
    }
} catch {
    Write-Host "❌ AI文本分类失败: $($_.Exception.Message)"
}

# 8. 移动端API测试
Write-Host "`n8. 移动端API测试..."
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/mobile/files/recent?limit=5" -Method GET -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 移动端API调用成功"
    Write-Host "最近文件数量: $($result.data.Count)"
} catch {
    Write-Host "❌ 移动端API调用失败: $($_.Exception.Message)"
}

# 9. 协作功能测试
Write-Host "`n9. 协作项目创建测试..."
$projectData = @{
    projectName = "测试协作项目"
    description = "这是一个用于测试的协作项目"
    tags = "测试,协作"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/collaboration/projects" -Method POST -Body $projectData -Headers $authHeaders -UseBasicParsing
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✅ 协作项目创建成功"
    Write-Host "项目ID: $($result.data.id)"
    Write-Host "项目名称: $($result.data.projectName)"
} catch {
    Write-Host "❌ 协作项目创建失败: $($_.Exception.Message)"
}

# 清理测试文件
if (Test-Path $testFilePath) {
    Remove-Item $testFilePath -Force
}

Write-Host "`n=========================================="
Write-Host "API测试完成！"
Write-Host "=========================================="
Write-Host "系统运行地址: $baseUrl"
Write-Host "API文档地址: $baseUrl/swagger-ui.html"
Write-Host "H2控制台: $baseUrl/h2-console"