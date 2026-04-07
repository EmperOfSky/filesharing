<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useFileStore } from '@/stores/file'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const router = useRouter()
const fileStore = useFileStore()

const loading = ref(false)
const recentFiles = ref<any[]>([])

const stats = ref({
  totalFiles: 0,
  totalFolders: 0,
  sampledSize: 0,
  todayUploads: 0
})

const quickActions =[
  { title: '快传中心', route: '/dashboard/quick-transfer', icon: 'Connection', tone: 'sky' },
  { title: '文件管理', route: '/dashboard/files', icon: 'FolderOpened', tone: 'sky' },
  { title: '资源搜索', route: '/dashboard/search', icon: 'Search', tone: 'sand' },
  { title: '协作项目', route: '/dashboard/collaboration', icon: 'EditPen', tone: 'ink' }
]

const formatFileSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units =['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const getFileIcon = (extension?: string) => {
  const iconMap: Record<string, string> = {
    pdf: 'Document',
    doc: 'Document',
    docx: 'Document',
    xls: 'Tickets',
    xlsx: 'Tickets',
    ppt: 'Monitor',
    pptx: 'Monitor',
    jpg: 'Picture',
    jpeg: 'Picture',
    png: 'Picture',
    gif: 'Picture',
    mp4: 'VideoCamera',
    avi: 'VideoCamera',
    mov: 'VideoCamera',
    zip: 'Box',
    rar: 'Box',
    txt: 'Memo'
  }

  return iconMap[String(extension || '').toLowerCase()] || 'Document'
}

const go = (path: string) => {
  router.push(path)
}

const openPreview = (fileId: number) => {
  router.push(`/dashboard/preview/${fileId}`)
}

const loadDashboardData = async () => {
  loading.value = true
  try {
    const fileResponse = await fileStore.loadFiles(0, 24)
    await fileStore.loadFolders()

    const files = [...fileStore.files]
    stats.value.totalFiles = Number(fileResponse.totalElements || files.length)
    stats.value.totalFolders = fileStore.folders.length
    stats.value.sampledSize = files.reduce((sum, file) => sum + Number(file.fileSize || 0), 0)
    stats.value.todayUploads = files.filter((file) => {
      const createdAt = new Date(file.createdAt)
      const now = new Date()
      return createdAt.getFullYear() === now.getFullYear()
        && createdAt.getMonth() === now.getMonth()
        && createdAt.getDate() === now.getDate()
    }).length

    recentFiles.value = files
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 6)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载工作台数据失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<template>
  <div class="modern-dashboard">
    <!-- 顶部欢迎与统计卡片 -->
    <header class="base-card welcome-hero">
      <div class="hero-content">
        <div class="hero-text">
          <span class="kicker-tag">Overview</span>
          <h1 class="greeting">欢迎回到工作台 👋</h1>
          <p class="subtitle">在这里概览您的存储空间，快速访问近期文件或发起源文件分享。</p>
          <div class="hero-actions">
            <el-button color="#0ea5e9" type="primary" size="large" @click="go('/dashboard/quick-transfer')" round>
              <el-icon class="el-icon--left"><Connection /></el-icon>快传中心
            </el-button>
            <el-button color="#0ea5e9" type="primary" size="large" @click="go('/dashboard/files')" round>
              <el-icon class="el-icon--left"><FolderOpened /></el-icon>
              文件管理
            </el-button>
          </div>
        </div>

        <div class="hero-stats">
          <UiStatCard label="总计文件" :value="stats.totalFiles" />
          <UiStatCard label="文件夹" :value="stats.totalFolders" />
          <UiStatCard label="今日上传" :value="stats.todayUploads" />
          <UiStatCard label="已用容量" :value="formatFileSize(stats.sampledSize)" />
        </div>
      </div>
    </header>

    <!-- 快捷功能矩阵 -->
    <section class="action-grid">
      <article
        v-for="item in quickActions"
        :key="item.title"
        class="action-card"
        :class="`tone-${item.tone}`"
        @click="go(item.route)"
      >
        <div class="card-icon-wrapper">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="card-content">
          <h3>{{ item.title }}</h3>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </article>
    </section>

    <!-- 主体双栏布局 -->
    <div class="dashboard-layout">
      <!-- 左侧：最近文件列表 -->
      <section class="base-card main-panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">最近文件</h2>
            <p class="panel-desc">您最近上传或修改过的文件</p>
          </div>
          <el-button text bg @click="go('/dashboard/files')">查看全部</el-button>
        </div>

        <div class="table-container">
          <el-table :data="recentFiles" v-loading="loading" class="modern-table">
            <template #empty>
              <div class="empty-state">
                <el-empty description="暂无最近文件" />
              </div>
            </template>

            <el-table-column label="文件名称" min-width="260">
              <template #default="{ row }">
                <div class="file-cell">
                  <div class="file-icon-wrap" :class="row.extension ? '' : 'is-folder'">
                    <el-icon><component :is="getFileIcon(row.extension)" /></el-icon>
                  </div>
                  <div class="file-info">
                    <span class="file-name">{{ row.originalName }}</span>
                    <span class="file-meta">{{ row.extension?.toUpperCase() || '未知类型' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="大小" width="120">
              <template #default="{ row }">
                <span class="text-secondary">{{ formatFileSize(row.fileSize) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="上传时间" width="180">
              <template #default="{ row }">
                <span class="text-secondary">{{ formatDateTime(row.createdAt) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" fixed="right" align="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-tooltip content="预览文件" placement="top">
                    <el-button circle type="primary" plain size="small" @click="openPreview(row.id)">
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <!-- 右侧：快捷入口 -->
      <aside class="quick-jump-panel">
        <div class="base-card sticky-card">
          <div class="panel-header compact">
            <h2 class="panel-title">快捷入口</h2>
          </div>
          <nav class="pill-nav">
            <button class="nav-item" @click="go('/dashboard/shares')">
              <div class="nav-icon share-icon"><el-icon><Share /></el-icon></div>
              <span>分享管理</span>
              <el-icon class="nav-arrow"><ArrowRight /></el-icon>
            </button>
            <button class="nav-item" @click="go('/dashboard/recycle-bin')">
              <div class="nav-icon trash-icon"><el-icon><Delete /></el-icon></div>
              <span>回收站</span>
              <el-icon class="nav-arrow"><ArrowRight /></el-icon>
            </button>
            <button class="nav-item" @click="go('/dashboard/recommendations')">
              <div class="nav-icon star-icon"><el-icon><Star /></el-icon></div>
              <span>推荐资源</span>
              <el-icon class="nav-arrow"><ArrowRight /></el-icon>
            </button>
            <button class="nav-item" @click="go('/dashboard/profile')">
              <div class="nav-icon user-icon"><el-icon><User /></el-icon></div>
              <span>个人中心</span>
              <el-icon class="nav-arrow"><ArrowRight /></el-icon>
            </button>
          </nav>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* 全局变量与底色设定 */
.modern-dashboard {
  --primary-color: #0ea5e9;
  --primary-light: #e0f2fe;
  --text-main: #0f172a;
  --text-regular: #334155;
  --text-muted: #64748b;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;

  background-color: var(--bg-page);
  min-height: calc(100vh - 60px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 通用卡片样式 */
.base-card {
  background: var(--bg-card);
  border-radius: 20px;
  box-shadow: 0 4px 24px -4px rgba(0, 0, 0, 0.03), 0 2px 8px -2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.6);
  padding: 32px;
  transition: box-shadow 0.3s ease;
}

/* 顶部 Welcome Card */
.welcome-hero {
  padding: 40px;
}
.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}
.hero-text {
  flex: 1;
  max-width: 500px;
}
.kicker-tag {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 12px;
}
.greeting {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}
.subtitle {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.6;
  margin: 0 0 24px 0;
}
.hero-actions {
  display: flex;
  gap: 12px;
}
.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-width: 320px;
}

/* 快捷功能矩阵 (Action Grid) */
.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}
.action-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;
  position: relative;
  overflow: hidden;
}
.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.08);
  border-color: var(--theme-color);
}
/* 定义四个卡片的特定主题色 */
.tone-sky { --theme-color: #0ea5e9; --theme-bg: #f0f9ff; }
.tone-kelp { --theme-color: #10b981; --theme-bg: #ecfdf5; }
.tone-sand { --theme-color: #f59e0b; --theme-bg: #fffbeb; }
.tone-ink { --theme-color: #6366f1; --theme-bg: #eef2ff; }

.card-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--theme-bg);
  color: var(--theme-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: transform 0.3s ease;
}
.action-card:hover .card-icon-wrapper {
  transform: scale(1.1);
}
.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}
.card-content h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}
.arrow-icon {
  color: var(--text-muted);
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
}
.action-card:hover .arrow-icon {
  opacity: 1;
  transform: translateX(0);
  color: var(--theme-color);
}

/* 主体双栏布局 */
.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: start;
}

/* 面板通用头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.panel-header.compact {
  margin-bottom: 16px;
}
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}
.panel-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 4px 0 0 0;
}

/* 深度定制的 Element Plus 表格 */
.modern-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: #f8fafc;
}
:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  color: var(--text-muted);
  font-size: 13px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}
:deep(.el-table td.el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid #f8fafc;
}

/* 文件行样式 */
.file-cell {
  display: flex;
  align-items: center;
  gap: 16px;
}
.file-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.file-icon-wrap.is-folder {
  background: #fef3c7;
  color: #f59e0b;
}
.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}
.file-meta {
  font-size: 12px;
  color: var(--text-muted);
}
.text-secondary {
  color: var(--text-muted);
  font-size: 13px;
}
.row-actions {
  opacity: 0;
  transition: opacity 0.3s;
}
:deep(.el-table__row:hover) .row-actions {
  opacity: 1;
}

/* 右侧快捷入口导航 (Pill Nav) */
.quick-jump-panel {
  position: relative;
}
.sticky-card {
  position: sticky;
  top: 24px;
  padding: 24px;
}
.pill-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.nav-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-regular);
}
.nav-item:hover {
  background: var(--bg-page);
  color: var(--primary-color);
}
.nav-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 16px;
}
.share-icon { background: #e0e7ff; color: #4f46e5; }
.trash-icon { background: #fee2e2; color: #ef4444; }
.star-icon { background: #fef3c7; color: #f59e0b; }
.user-icon { background: #dcfce7; color: #22c55e; }

.nav-arrow {
  margin-left: auto;
  color: var(--text-muted);
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s ease;
}
.nav-item:hover .nav-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
  }
  .sticky-card {
    position: static;
  }
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-stats {
    width: 100%;
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .action-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>