"""
文件共享系统 - API接口综合测试脚本
"""

import requests
import json
import sys
from datetime import datetime
from typing import List, Dict, Tuple

BASE_URL = "http://localhost:8080/api"

class APITester:
    def __init__(self):
        self.results = []
        self.token = None
        self.headers = {'Content-Type': 'application/json'}
        
    def test_endpoint(self, method: str, endpoint: str, name: str, data=None, 
                     needs_auth=False, expected_status=None) -> bool:
        """测试单个端点"""
        url = f"{BASE_URL}{endpoint}"
        headers = self.headers.copy()
        
        if needs_auth and self.token:
            headers['Authorization'] = f"Bearer {self.token}"
            
        try:
            if method.upper() == 'GET':
                resp = requests.get(url, headers=headers, timeout=5)
            elif method.upper() == 'POST':
                resp = requests.post(url, headers=headers, json=data, timeout=5)
            elif method.upper() == 'PUT':
                resp = requests.put(url, headers=headers, json=data, timeout=5)
            elif method.upper() == 'DELETE':
                resp = requests.delete(url, headers=headers, timeout=5)
            else:
                return False
                
            success = expected_status is None or resp.status_code == expected_status
            status = f"✓ {resp.status_code}" if success else f"✗ {resp.status_code}"
            
            self.results.append({
                'name': name,
                'method': method,
                'endpoint': endpoint,
                'status': resp.status_code,
                'success': success
            })
            
            print(f"  {status} - {name}")
            return success
            
        except requests.exceptions.ConnectionError:
            print(f"  ✗ 连接失败 - {name}")
            self.results.append({
                'name': name,
                'method': method,
                'endpoint': endpoint,
                'status': 'CONNECTION_ERROR',
                'success': False
            })
            return False
        except Exception as e:
            print(f"  ✗ 错误 - {name}: {str(e)}")
            self.results.append({
                'name': name,
                'method': method,
                'endpoint': endpoint,
                'status': 'ERROR',
                'success': False
            })
            return False
    
    def run_all_tests(self):
        """运行所有测试"""
        print("=" * 70)
        print("文件共享系统 - API接口综合测试")
        print("=" * 70)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"基础URL: {BASE_URL}")
        print("=" * 70)
        print()
        
        # 1. 认证相关接口
        print("[1] 认证服务接口")
        self.test_endpoint('POST', '/auth/register', '用户注册', 
                          data={
                              'username': 'testapi' + datetime.now().strftime('%s'),
                              'email': f'testapi{datetime.now().timestamp()}@example.com',
                              'password': 'test123456'
                          })
        
        self.test_endpoint('POST', '/auth/login', '用户登录',
                          data={'identifier': 'demo', 'password': 'demo123456'})
        
        self.test_endpoint('GET', '/auth/me', '获取当前用户信息', needs_auth=True)
        
        print()
        
        # 2. 文件服务接口
        print("[2] 文件服务接口")
        self.test_endpoint('GET', '/files', '获取文件列表', needs_auth=True)
        self.test_endpoint('GET', '/files/1', '获取单个文件', needs_auth=True)
        self.test_endpoint('GET', '/files/search?keyword=test', '搜索文件', needs_auth=True)
        self.test_endpoint('DELETE', '/files/1', '删除文件', needs_auth=True)
        
        print()
        
        # 3. 文件夹服务接口
        print("[3] 文件夹服务接口")
        self.test_endpoint('GET', '/folders', '获取文件夹列表', needs_auth=True)
        self.test_endpoint('POST', '/folders', '创建文件夹',
                          data={'folderName': 'TestFolder' + datetime.now().strftime('%s')},
                          needs_auth=True)
        self.test_endpoint('GET', '/folders/1', '获取单个文件夹', needs_auth=True)
        
        print()
        
        # 4. AI服务接口
        print("[4] AI服务接口")
        self.test_endpoint('GET', '/ai/models', 'AI模型列表')
        self.test_endpoint('POST', '/ai/document-summary', 'AI文档总结',
                          data={'content': 'This is test content for summarization'})
        self.test_endpoint('POST', '/ai/answer-question', 'AI问答系统',
                          data={'question': 'What is this?', 'context': 'This is a test'})
        self.test_endpoint('POST', '/ai/text-correction', 'AI文本纠正',
                          data={'content': 'This is a tets'})
        self.test_endpoint('POST', '/ai/classify-content', 'AI内容分类',
                          data={'content': 'Technology news about AI'})
        self.test_endpoint('POST', '/ai/analyze-file', 'AI文件分析',
                          data={'fileId': 1})
        self.test_endpoint('POST', '/ai/recommend-tags', 'AI标签推荐',
                          data={'fileId': 1})
        self.test_endpoint('POST', '/ai/smart-search', 'AI智能搜索',
                          data={'query': 'important documents', 'limit': 10})
        self.test_endpoint('POST', '/ai/analyze-sentiment', 'AI情感分析',
                          data={'content': 'I love this amazing system!'})
        self.test_endpoint('POST', '/ai/extract-keywords', 'AI关键词提取',
                          data={'content': 'Artificial Intelligence and Machine Learning'})
        
        print()
        
        # 5. 分享服务接口
        print("[5] 分享服务接口")
        self.test_endpoint('GET', '/shares', '获取分享列表', needs_auth=True)
        self.test_endpoint('POST', '/shares', '创建分享',
                          data={'fileId': 1, 'shareType': 'PUBLIC'},
                          needs_auth=True)
        self.test_endpoint('GET', '/shares/1', '获取单个分享', needs_auth=True)
        
        print()
        
        # 6. 云存储接口
        print("[6] 云存储服务接口")
        self.test_endpoint('GET', '/cloud-storage/configs', '获取云存储配置', needs_auth=True)
        self.test_endpoint('GET', '/cloud-storage/configs/enabled', '获取启用的云存储', needs_auth=True)
        
        print()
        
        # 7. 监控服务接口
        print("[7] 监控和统计接口")
        self.test_endpoint('GET', '/monitoring/health', '健康检查')
        self.test_endpoint('GET', '/monitoring/metrics', '获取监控指标')
        self.test_endpoint('GET', '/monitoring/statistics', '获取统计信息')
        
        print()
        
        # 8. 备份服务接口
        print("[8] 备份服务接口")
        self.test_endpoint('GET', '/backup', '获取备份列表', needs_auth=True)
        self.test_endpoint('POST', '/backup', '创建备份', needs_auth=True)
        
        print()
        
        # 9. 文件预览接口
        print("[9] 文件预览接口")
        self.test_endpoint('GET', '/files/preview/1', '文件预览', needs_auth=True)
        self.test_endpoint('GET', '/files/download/1', '文件下载', needs_auth=True)
        
        print()
        
        # 10. 推荐服务接口
        print("[10] 推荐服务接口")
        self.test_endpoint('GET', '/recommendation/recommend', '获取推荐', needs_auth=True)
        
        print()
        
        # 生成报告
        self._generate_report()
    
    def _generate_report(self):
        """生成测试报告"""
        total = len(self.results)
        success_count = sum(1 for r in self.results if r['success'])
        
        print("=" * 70)
        print("测试汇总")
        print("=" * 70)
        print(f"总计: {total} 个接口")
        print(f"成功: {success_count} 个 ({success_count*100//total if total > 0 else 0}%)")
        print(f"失败: {total - success_count} 个")
        print("=" * 70)
        print()
        
        # 按状态分类显示
        success_results = [r for r in self.results if r['success']]
        failed_results = [r for r in self.results if not r['success']]
        
        if success_results:
            print("✓ 成功的接口:")
            for r in success_results:
                print(f"  {r['method']:6} {r['endpoint']:40} ({r['name']})")
        
        if failed_results:
            print()
            print("✗ 失败的接口:")
            for r in failed_results:
                print(f"  {r['method']:6} {r['endpoint']:40} ({r['name']})")
        
        print()
        print("=" * 70)
        
        # 保存报告到文件
        report_file = r"C:\Users\Admin\Desktop\filesharing\api_test_report.txt"
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("=" * 70 + "\n")
            f.write("文件共享系统 - API接口测试报告\n")
            f.write("=" * 70 + "\n")
            f.write(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"基础URL: {BASE_URL}\n")
            f.write(f"总计: {total} 个接口\n")
            f.write(f"成功: {success_count} 个 ({success_count*100//total if total > 0 else 0}%)\n")
            f.write(f"失败: {total - success_count} 个\n")
            f.write("=" * 70 + "\n\n")
            
            if success_results:
                f.write("✓ 成功的接口:\n")
                for r in success_results:
                    f.write(f"  {r['method']:6} {r['endpoint']:40} ({r['name']})\n")
            
            if failed_results:
                f.write("\n✗ 失败的接口:\n")
                for r in failed_results:
                    f.write(f"  {r['method']:6} {r['endpoint']:40} ({r['name']})\n")
        
        print(f"报告已保存到: {report_file}")
        input("\n按Enter键退出...")

if __name__ == '__main__':
    tester = APITester()
    try:
        tester.run_all_tests()
    except KeyboardInterrupt:
        print("\n\n测试已中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n测试出错: {str(e)}")
        sys.exit(1)
