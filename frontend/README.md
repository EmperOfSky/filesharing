# Vue3 文件共享系统前端

这是一个基于Vue3 + Element Plus的现代化文件共享系统前端界面。

## 🚀 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **TypeScript** - 类型安全的JavaScript超集
- **Element Plus** - Vue3组件库
- **Vue Router 4** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP客户端
- **Vite** - 构建工具

## 📁 项目结构

```
src/
├── assets/          # 静态资源
├── components/      # 可复用组件
├── composables/     # Composition API函数
├── layouts/         # 页面布局
├── pages/           # 页面组件
├── router/          # 路由配置
├── services/        # API服务
├── stores/          # 状态管理
├── styles/          # 样式文件
├── types/           # TypeScript类型定义
├── utils/           # 工具函数
├── App.vue          # 根组件
└── main.ts          # 入口文件
```

## 🛠️ 开发环境搭建

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
在 `.env` 文件中配置：
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 代理配置
Vite配置了API代理，解决开发环境跨域问题：
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 🎯 功能特性

### 已实现功能
- ✅ 用户认证（登录/注册）
- ✅ 仪表板展示
- ✅ 文件管理（上传/下载/删除）
- ✅ 文件夹管理
- ✅ 文件搜索
- ✅ 响应式布局
- ✅ 路由权限控制

### 待完善功能
- 文件预览功能
- 分享链接生成
- 批量操作
- 文件拖拽上传
- 移动端适配优化

## 📱 界面预览

### 登录页面
- 现代化设计风格
- 表单验证
- 错误提示

### 仪表板
- 数据统计卡片
- 最近文件列表
- 快捷操作按钮

### 文件管理
- 文件列表展示
- 文件夹导航
- 上传功能
- 搜索功能

## 🔒 安全特性

- JWT Token认证
- 路由守卫
- 请求拦截器
- 错误处理

## 🎨 样式规范

- 使用Element Plus组件库
- 统一的颜色主题
- 响应式设计
- 一致的交互体验

## 📞 开发支持

如有问题或建议，请联系开发团队。

---
**注意**: 这是React前端的Vue3重构版本，保持了相同的API接口和功能逻辑。