@echo off
REM 自动创建MySQL数据库和所有表的快速脚本

setlocal enabledelayedexpansion

set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe
set SQL_FILE=C:\Users\Admin\Desktop\filesharing\setup_mysql.sql

echo ============================================================
echo        MySQL 数据库初始化脚本
echo ============================================================
echo.
echo 此脚本将创建:
echo   - 数据库: filesharing
echo   - 所有必要的表
echo.

if not exist "!MYSQL_BIN!" (
    echo 错误: MySQL 不存在于 !MYSQL_BIN!
    pause
    exit /b 1
)

if not exist "!SQL_FILE!" (
    echo 错误: SQL 文件不存在于 !SQL_FILE!
    pause
    exit /b 1
)

echo 正在执行数据库初始化...
echo.

"!MYSQL_BIN!" -h localhost -u root -p123456 < "!SQL_FILE!"

if errorlevel 1 (
    echo.
    echo 密码 123456 失败，尝试 123...
    "!MYSQL_BIN!" -h localhost -u root -p123 < "!SQL_FILE!"
)

if errorlevel 1 (
    echo.
    echo ✗ 数据库初始化失败！请检查:
    echo   1. MySQL 是否运行 (检查Windows服务)
    echo   2. MySQL 密码是否正确 (实际密码可能不同)
    echo   3. SQL 文件是否完整
    pause
    exit /b 1
)

echo.
echo ✓ 数据库初始化成功！
echo.
echo 已创建以下表:
echo   • users            - 用户账户
echo   • folders          - 文件夹
echo   • files            - 文件
echo   • file_versions    - 文件版本
echo   • file_tags        - 文件标签
echo   • shares           - 文件分享
echo   • file_statistics  - 文件统计
echo   • chunk_upload_records - 分块上传
echo   • operation_logs   - 操作日志
echo   • notifications    - 通知
echo   • ai_analysis_records - AI分析
echo   • ai_models        - AI模型
echo   • batch_operations - 批量操作
echo.
echo 数据库连接信息:
echo   Host: localhost
echo   Port: 3306
echo   Database: filesharing
echo   User: root
echo   Password: 123456 (或 123)
echo.
pause
