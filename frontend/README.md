# 文件共享系统前端

这是一个现代化的文件共享系统前端应用，基于React 18和Ant Design构建。

## 🚀 技术栈

- **React 18** - 前端框架
- **Ant Design 5** - UI组件库
- **React Router 6** - 路由管理
- **Axios** - HTTP客户端
- **Zustand** - 状态管理
- **Vite** - 构建工具
- **Tailwind CSS** - 样式框架

## 📁 项目结构

```
src/
├── components/          # 公共组件
│   └── layout/         # 布局组件
├── pages/              # 页面组件
├── services/           # API服务
├── stores/             # 状态管理
├── utils/              # 工具函数
├── assets/             # 静态资源
├── hooks/              # 自定义Hook
├── App.jsx             # 根组件
└── main.jsx            # 入口文件
```

## 🎯 主要功能

### 用户认证
- [x] 用户登录/注册
- [x] JWT Token管理
- [x] 自动登录状态保持

### 文件管理
- [x] 文件上传/下载
- [x] 文件夹管理
- [x] 拖拽上传
- [x] 文件预览
- [x] 批量操作

### 协作功能
- [ ] 团队协作
- [ ] 文件分享
- [ ] 权限管理

### 搜索功能
- [ ] 全文搜索
- [ ] 标签筛选
- [ ] 高级搜索

## 🛠️ 开发指南

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 预览生产构建
```bash
npm run preview
```

## 🔧 配置说明

### 环境变量
创建 `.env` 文件：
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=文件共享系统
```

### 代理配置
在 `vite.config.js` 中配置API代理：
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

## 📱 响应式设计

应用支持多种设备屏幕：
- 桌面端 (≥1024px)
- 平板端 (768px-1023px)
- 移动端 (<768px)

## 🔒 安全特性

- JWT Token认证
- CSRF防护
- XSS防护
- 权限控制

## 🎨 主题定制

可以通过修改Ant Design主题变量来自定义外观：

```javascript
// 在App.jsx中
<ConfigProvider
  theme={{
    token: {
      colorPrimary: '#1890ff',
      borderRadius: 6,
    },
  }}
>
```

## 🚀 部署

### 静态部署
```bash
npm run build
# 将 dist 目录部署到静态服务器
```

### Docker部署
```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 📊 性能优化

- 代码分割和懒加载
- 图片懒加载
- HTTP缓存策略
- 打包体积优化

## 🐛 调试工具

- React DevTools
- Redux DevTools (如果使用Redux)
- 浏览器开发者工具
- 网络请求监控

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 📄 许可证

MIT License

## 📞 支持

如有问题，请联系：support@filesharing.com