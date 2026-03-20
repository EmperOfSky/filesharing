#!/usr/bin/env python3
"""
MySQL数据库初始化脚本
这个脚本读取SQL文件并执行MySQL命令来创建数据库和表
"""

import subprocess
import os
import sys

MYSQL_PATH = r"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"
SQL_FILE = r"C:\Users\Admin\Desktop\filesharing\setup_mysql.sql"
PASSWORDS = ["123456", "123"]

print("=" * 60)
print("文件共享系统 - MySQL数据库初始化")
print("=" * 60)

# 检查MySQL是否存在
if not os.path.exists(MYSQL_PATH):
    print(f"❌ 错误: MySQL路径不存在: {MYSQL_PATH}")
    sys.exit(1)

# 检查SQL文件是否存在
if not os.path.exists(SQL_FILE):
    print(f"❌ 错误: SQL文件不存在: {SQL_FILE}")
    sys.exit(1)

print(f"✓ 找到MySQL: {MYSQL_PATH}")
print(f"✓ 找到SQL文件: {SQL_FILE}")

# 读取SQL文件
with open(SQL_FILE, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 尝试连接和执行
success = False
for password in PASSWORDS:
    print(f"\n尝试使用密码: {'*' * len(password)}")
    try:
        # 创建MySQL连接命令
        cmd = [MYSQL_PATH, '-h', 'localhost', '-u', 'root', f'-p{password}']
        
        # 执行SQL
        process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        stdout, stderr = process.communicate(input=sql_content, timeout=30)
        
        if process.returncode == 0:
            print("✓ 数据库初始化成功！")
            print("\n===== 初始化完成 =====")
            print(f"数据库: filesharing")
            print(f"用户: root")
            print(f"密码: {password}")
            print("主机: localhost:3306")
            print("\n创建的表:")
            print("  • users - 用户表")
            print("  • folders - 文件夹表")
            print("  • files - 文件表")
            print("  • file_versions - 文件版本表")
            print("  • file_tags - 文件标签表")
            print("  • shares - 分享记录表")
            print("  • file_statistics - 文件统计表")
            print("  • chunk_upload_records - 分块上传记录表")
            print("  • operation_logs - 操作日志表")
            print("  • notifications - 通知表")
            print("  • ai_analysis_records - AI分析记录表")
            print("  • ai_models - AI模型表")
            print("  • batch_operations - 批量操作表")
            success = True
            break
        else:
            print(f"✗ 执行失败: {stderr}")
            
    except subprocess.TimeoutExpired:
        print(f"✗ 超时: 命令执行时间过长")
    except Exception as e:
        print(f"✗ 错误: {str(e)}")

if not success:
    print("\n❌ 所有密码都失败了")
    print("\n请手动执行以下命令:")
    print(f'  mysql -h localhost -u root -p < "{SQL_FILE}"')
    sys.exit(1)
else:
    print("\n应用程序可以立即启动")
    sys.exit(0)
