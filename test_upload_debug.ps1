# 调试文件上传错误的脚本
$apiUrl = "http://localhost:8080/api/files/upload"
$testFile = "c:\Users\Admin\Desktop\filesharing\temp\test_upload.txt"

# 创建测试文件
Write-Host "创建测试文件..."
if (-not (Test-Path "c:\Users\Admin\Desktop\filesharing\temp")) {
    New-Item -ItemType Directory -Path "c:\Users\Admin\Desktop\filesharing\temp" -Force | Out-Null
}

"这是一个测试文件用于上传功能调试" | Out-File -FilePath $testFile -Encoding UTF8

# 获取测试用户的Token（假设已登录）
Write-Host "获取认证token..."
$loginUrl = "http://localhost:8080/api/users/login"
$loginData = @{
    username = "testuser"
    password = "testpass123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginData -ContentType "application/json" -ErrorAction SilentlyContinue

if ($loginResponse) {
    $token = $loginResponse.data.token
    Write-Host "Token获取成功: $token"
} else {
    Write-Host "无法获取token，使用默认测试token"
    $token = "test-token-for-debugging"
}

# 准备上传请求
Write-Host "准备上传文件: $testFile"
$fileContent = [System.IO.File]::ReadAllBytes($testFile)

# 使用MultipartForm上传
Write-Host "发送上传请求到: $apiUrl"
try {
    $boundary = [System.Guid]::NewGuid().ToString()
    $body = @"
--$boundary
Content-Disposition: form-data; name="file"; filename="test_upload.txt"
Content-Type: text/plain

$([System.Text.Encoding]::UTF8.GetString($fileContent))
--$boundary--
"@

    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "multipart/form-data; boundary=$boundary"
    }

    $response = Invoke-RestMethod -Uri $apiUrl `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -ErrorAction Stop

    Write-Host "上传成功!"
    Write-Host "响应: $($response | ConvertTo-Json -Depth 10)"

} catch {
    Write-Host "上传失败!"
    Write-Host "错误状态码: $($_.Exception.Response.StatusCode)"
    Write-Host "错误信息: $($_.Exception.Message)"
    
    # 尝试读取响应体
    try {
        $errorResponse = $_.Exception.Response.Content.ReadAsStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        
        Write-Host "服务器响应:"
        Write-Host $responseBody
        
        # 尝试解析JSON
        $jsonResponse = $responseBody | ConvertFrom-Json
        Write-Host "错误代码: $($jsonResponse.message)"
        Write-Host "错误详情: $($jsonResponse.data)"
    } catch {
        Write-Host "无法解析错误响应"
    }
}
