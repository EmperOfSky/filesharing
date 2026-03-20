@echo off
REM 启动文件共享系统后端

setlocal enabledelayedexpansion

set JAR_FILE=C:\Users\Admin\Desktop\filesharing\target\file-sharing-system-1.0.0.jar

echo ============================================================
echo        文件共享系统 - 后端应用启动
echo ============================================================
echo.

if not exist "!JAR_FILE!" (
    echo 错误: JAR文件不存在 !JAR_FILE!
    echo 请先运行构建: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo 启动后端应用...
echo.
echo 数据库配置:
echo   - Host: localhost:3306
echo   - Database: filesharing
echo   - User: root
echo.
echo 访问地址:
echo   - API: http://localhost:8080
echo   - H2控制台: http://localhost:8080/h2-console (如需要)
echo.

java -jar "!JAR_FILE!" --server.port=8080

pause
