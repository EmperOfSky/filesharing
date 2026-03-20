@echo off
REM API接口综合测试脚本

setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/api
set RESULTS_FILE=C:\Users\Admin\Desktop\filesharing\api_test_results.txt

echo. > "!RESULTS_FILE!"
echo ============================================================ >> "!RESULTS_FILE!"
echo 文件共享系统 - API接口测试报告 >> "!RESULTS_FILE!"
echo ============================================================ >> "!RESULTS_FILE!"
echo 测试时间: %date% %time% >> "!RESULTS_FILE!"
echo 基础URL: %BASE_URL% >> "!RESULTS_FILE!"
echo. >> "!RESULTS_FILE!"

REM 定义颜色变量
for /F %%A in ('copy /Z "%~f0" nul') do set "BS=%%A"

echo.
echo ============================================================
echo 文件共享系统 - API接口测试
echo ============================================================
echo.

REM 测试健康检查
echo [1/15] 测试健康检查... 
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/monitoring/health" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 健康检查失败
) else (
    echo ✓ 健康检查
)

REM 测试监控指标
echo [2/15] 测试监控指标...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/monitoring/metrics" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 监控指标失败
) else (
    echo ✓ 监控指标
)

REM 测试用户注册
echo [3/15] 测试用户注册...
curl -s -o nul -w "HTTP %%{http_code}" -X POST "%BASE_URL%/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"testapi\",\"email\":\"testapi@example.com\",\"password\":\"test123456\"}" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 用户注册失败
) else (
    echo ✓ 用户注册
)

REM 测试用户登录
echo [4/15] 测试用户登录...
curl -s -o nul -w "HTTP %%{http_code}" -X POST "%BASE_URL%/auth/login" -H "Content-Type: application/json" -d "{\"identifier\":\"testapi\",\"password\":\"test123456\"}" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 用户登录失败
) else (
    echo ✓ 用户登录
)

REM 测试获取当前用户信息
echo [5/15] 测试获取当前用户信息...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/auth/me" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 获取用户信息失败
) else (
    echo ✓ 获取用户信息
)

REM 测试文件列表
echo [6/15] 测试文件列表...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/files" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 文件列表失败
) else (
    echo ✓ 文件列表
)

REM 测试创建文件夹
echo [7/15] 测试创建文件夹...
curl -s -o nul -w "HTTP %%{http_code}" -X POST "%BASE_URL%/folders" -H "Authorization: Bearer dummy_token" -H "Content-Type: application/json" -d "{\"folderName\":\"TestFolder\"}" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 创建文件夹失败
) else (
    echo ✓ 创建文件夹
)

REM 测试获取文件夹列表
echo [8/15] 测试获取文件夹列表...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/folders" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 文件夹列表失败
) else (
    echo ✓ 文件夹列表
)

REM 测试AI服务 - 获取模型列表
echo [9/15] 测试AI服务 - 获取模型列表...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/ai/models" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ AI模型列表失败
) else (
    echo ✓ AI模型列表
)

REM 测试AI服务 - 文档总结
echo [10/15] 测试AI服务 - 文档总结...
curl -s -o nul -w "HTTP %%{http_code}" -X POST "%BASE_URL%/ai/document-summary" -H "Content-Type: application/json" -d "{\"content\":\"Test content\"}" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ AI文档总结失败
) else (
    echo ✓ AI文档总结
)

REM 测试分享服务
echo [11/15] 测试分享服务...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/shares" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 分享服务失败
) else (
    echo ✓ 分享服务
)

REM 测试云存储配置
echo [12/15] 测试云存储配置...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/cloud-storage/configs" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 云存储配置失败
) else (
    echo ✓ 云存储配置
)

REM 测试文件预览
echo [13/15] 测试文件预览...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/files/preview/1" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 文件预览失败
) else (
    echo ✓ 文件预览
)

REM 测试备份服务
echo [14/15] 测试备份服务...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/backup" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 备份服务失败
) else (
    echo ✓ 备份服务
)

REM 测试推荐服务
echo [15/15] 测试推荐服务...
curl -s -o nul -w "HTTP %%{http_code}" "%BASE_URL%/recommendation/recommend" -H "Authorization: Bearer dummy_token" >> "!RESULTS_FILE!"
if errorlevel 1 (
    echo ✗ 推荐服务失败
) else (
    echo ✓ 推荐服务
)

echo.
echo ============================================================
echo 测试完成！结果已保存到:
echo %RESULTS_FILE%
echo ============================================================
echo.

type "!RESULTS_FILE!"
pause
