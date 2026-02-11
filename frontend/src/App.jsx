import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import 'dayjs/locale/zh-cn';

// 页面组件
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import Dashboard from './pages/Dashboard';
import FileManager from './pages/FileManager';
import FilePreview from './pages/FilePreview';
import UserProfile from './pages/UserProfile';
import SearchPage from './pages/SearchPage';

// 布局组件
import MainLayout from './components/layout/MainLayout';
import AuthLayout from './components/layout/AuthLayout';

// 状态管理
import { useAuthStore } from './stores/authStore';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <ConfigProvider locale={zhCN}>
      <Router>
        <div className="App">
          <Routes>
            {/* 认证路由 */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
            </Route>

            {/* 受保护的路由 */}
            <Route element={<MainLayout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/files" element={<FileManager />} />
              <Route path="/files/:id" element={<FilePreview />} />
              <Route path="/profile" element={<UserProfile />} />
              <Route path="/search" element={<SearchPage />} />
            </Route>
          </Routes>
        </div>
      </Router>
    </ConfigProvider>
  );
}

export default App;