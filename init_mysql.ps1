# MySQL数据库初始化脚本
# 这个脚本会执行SQL文件来创建数据库和所有表

$mysqlPath = "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"
$sqlFile = "C:\Users\Admin\Desktop\filesharing\setup_mysql.sql"
$host = "localhost"
$user = "root"

# 提示输入MySQL root密码
$password = Read-Host "请输入MySQL root用户密码（如果没有密码，直接按Enter）" -AsSecureString

# 转换secure string为纯文本用于传递给mysql
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($password)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringUni($bstr)

# 构建命令
if ([string]::IsNullOrEmpty($plainPassword)) {
    # 没有密码的情况
    Write-Host "正在执行数据库初始化..." -ForegroundColor Green
    & $mysqlPath -h $host -u $user < $sqlFile
} else {
    # 有密码的情况  
    Write-Host "正在执行数据库初始化..." -ForegroundColor Green
    & $mysqlPath -h $host -u $user -p$plainPassword < $sqlFile
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ 数据库初始化成功！" -ForegroundColor Green
    Write-Host "已创建数据库: filesharing" -ForegroundColor Green
    Write-Host "请使用以下配置连接您的应用:" -ForegroundColor Green
    Write-Host "  Host: localhost" -ForegroundColor Green
    Write-Host "  Port: 3306" -ForegroundColor Green
    Write-Host "  Database: filesharing" -ForegroundColor Green
    Write-Host "  User: root" -ForegroundColor Green
} else {
    Write-Host "✗ 数据库初始化失败！" -ForegroundColor Red
    Write-Host "请检查:" -ForegroundColor Red
    Write-Host "  1. MySQL是否运行" -ForegroundColor Red
    Write-Host "  2. MySQL密码是否正确" -ForegroundColor Red
    Write-Host "  3. SQL文件是否存在: $sqlFile" -ForegroundColor Red
}

# 清理敏感数据
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
