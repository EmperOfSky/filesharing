@echo off
REM MySQL数据库初始化脚本
REM 这个脚本使用密码 123456 创建filesharing数据库和所有表

set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe
set SQL_FILE=C:\Users\Admin\Desktop\filesharing\setup_mysql.sql

echo 正在创建MySQL数据库和表...
"%MYSQL_PATH%" -h localhost -u root -p123456 < "%SQL_FILE%"

if errorlevel 1 (
    echo.
    echo 使用密码123456失败，尝试密码123...
    "%MYSQL_PATH%" -h localhost -u root -p123 < "%SQL_FILE%"
) else (
    echo.
    echo ✓ 数据库创建成功！
    echo 已创建数据库: filesharing
    echo 密码: 123456
    pause
    exit /b 0
)

if errorlevel 1 (
    echo.
    echo ✗ 数据库创建失败！
    echo 请检查MySQL是否运行和密码是否正确
    pause
    exit /b 1
) else (
    echo.
    echo ✓ 数据库创建成功！
    echo 已创建数据库: filesharing
    echo 密码: 123
    pause
    exit /b 0
)
