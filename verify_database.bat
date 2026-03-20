@echo off
REM 验证MySQL数据库是否成功创建

setlocal enabledelayedexpansion

set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe

echo ============================================================
echo        验证 filesharing 数据库
echo ============================================================
echo.

if not exist "!MYSQL_BIN!" (
    echo 错误: MySQL 不存在于 !MYSQL_BIN!
    pause
    exit /b 1
)

REM 尝试连接到数据库并显示所有表
"!MYSQL_BIN!" -h localhost -u root -p123456 filesharing -e "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='filesharing' ORDER BY TABLE_NAME;"

if errorlevel 1 (
    echo.
    echo 尝试使用密码 123...
    "!MYSQL_BIN!" -h localhost -u root -p123 filesharing -e "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='filesharing' ORDER BY TABLE_NAME;"
)

echo.
echo.
if errorlevel 1 (
    echo ✗ 连接失败！
    echo 请检查:
    echo   1. MySQL 是否运行
    echo   2. MySQL 密码是否正确
) else (
    echo ✓ 数据库验证成功！
)

pause
