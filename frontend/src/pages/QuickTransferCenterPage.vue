<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useFileStore } from '@/stores/file'
import UiStatCard from '@/components/ui/UiStatCard.vue'

interface HistoryItem {
  id: string
}

const router = useRouter()
const fileStore = useFileStore()
const authStore = useAuthStore()

const loading = ref(false)
const fileCount = ref(0)
const folderCount = ref(0)
const sampledFileSize = ref(0)
const sendCount = ref(0)
const pickupCount = ref(0)

const SEND_HISTORY_KEY = 'fcb_send_history'
const PICKUP_HISTORY_KEY = 'fcb_pickup_history'

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const centerActions = computed(() => {
  const base = [
    { title: '文件管理', route: '/dashboard/files', icon: 'FolderOpened', tone: 'sky' },
    { title: '发起快传', route: '/dashboard/quick-transfer/share', icon: 'Promotion', tone: 'kelp' },
    { title: '取件空间', route: '/dashboard/pickup-space', icon: 'FolderChecked', tone: 'sand' },
    { title: '资源搜索', route: '/dashboard/search', icon: 'Search', tone: 'ink' }
  ]

  if (isAdmin.value) {
    base.push(
      { title: '快传配置', route: '/dashboard/quick-transfer/config', icon: 'Setting', tone: 'sky' },
      { title: '快传记录', route: '/dashboard/quick-transfer/records', icon: 'Tickets', tone: 'kelp' }
    )
  }

  return base
})

const safeJsonParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const formatSize = (size: number) => {
  if (!size || size <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  return `${(size / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const loadCenterData = async () => {
  loading.value = true
  try {
    const fileResp = await fileStore.loadFiles(0, 100)
    await fileStore.loadFolders()

    fileCount.value = Number(fileResp.totalElements || fileStore.files.length)
    folderCount.value = fileStore.folders.length
    sampledFileSize.value = fileStore.files.reduce((sum, file) => sum + Number(file.fileSize || 0), 0)

    const sendHistory = safeJsonParse<HistoryItem[]>(localStorage.getItem(SEND_HISTORY_KEY), [])
    const pickupHistory = safeJsonParse<HistoryItem[]>(localStorage.getItem(PICKUP_HISTORY_KEY), [])
    sendCount.value = sendHistory.length
    pickupCount.value = pickupHistory.length
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载快传中心数据失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const go = (path: string) => {
  router.push(path)
}

onMounted(() => {
  loadCenterData()
})
</script>

<template>
  <div class="modern-transfer-hub" v-loading="loading">
    <!-- 顶部 Hero 区域 -->
    <header class="base-card hub-hero">
      <div class="hero-content">
        <div class="hero-intro">
          <span class="kicker-tag">Quick Transfer</span>
          <h1 class="title">快传中心</h1>
          <p class="description">高效的文件临时中转与分发枢纽，支持秒传、大文件切片与端到端安全传输。</p>
          <div class="hero-actions">
            <el-button color="#0ea5e9" type="primary" size="large" @click="go('/dashboard/quick-transfer/share')" round>
              <el-icon class="el-icon--left"><Promotion /></el-icon>发起快传
            </el-button>
            <el-button size="large" @click="go('/dashboard/pickup-space')" round plain>取件空间</el-button>
          </div>
        </div>
        <div class="hero-stats">
          <UiStatCard label="文件总计" :value="fileCount" />
          <UiStatCard label="目录索引" :value="folderCount" />
          <UiStatCard label="发件记录" :value="sendCount" />
          <UiStatCard label="取件记录" :value="pickupCount" />
        </div>
      </div>
    </header>

    <!-- 扩展统计条 -->
    <section class="hub-strip">
      <div class="strip-item base-card">
        <span class="label">存储占用</span>
        <span class="value">{{ formatSize(sampledFileSize) }}</span>
      </div>
      <div class="strip-item base-card">
        <span class="label">当前功能集</span>
        <span class="value">{{ centerActions.length }} 个可用模块</span>
      </div>
      <div class="strip-item base-card">
        <span class="label">权限身份</span>
        <span class="value">{{ isAdmin ? '管理员 (Admin)' : '普通用户 (User)' }}</span>
      </div>
    </section>

    <!-- 核心功能矩阵 -->
    <section class="action-grid">
      <article
        v-for="item in centerActions"
        :key="item.title"
        class="action-card"
        :class="`tone-${item.tone}`"
        @click="go(item.route)"
      >
        <div class="card-icon-wrapper">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="card-footer">
          <h3>{{ item.title }}</h3>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.modern-transfer-hub {
  --primary-color: #0ea5e9;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;
  
  background-color: var(--bg-page);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: calc(100vh - 60px);
}

.base-card {
  background: var(--bg-card);
  border-radius: 20px;
  box-shadow: 0 4px 24px -4px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--border-color);
  padding: 32px;
}

/* Hero Section */
.hub-hero { padding: 40px; }
.hero-content { display: flex; justify-content: space-between; align-items: center; gap: 40px; }
.hero-intro { flex: 1; max-width: 500px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0f2fe; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 32px; font-weight: 700; margin: 16px 0; color: #0f172a; }
.description { font-size: 15px; color: #64748b; line-height: 1.6; margin-bottom: 24px; }
.hero-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; min-width: 320px; }

/* Strip Section */
.hub-strip { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.strip-item { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; }
.strip-item .label { color: #64748b; font-size: 14px; }
.strip-item .value { font-weight: 700; color: #0f172a; font-size: 16px; }

/* Action Grid */
.action-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; }
.action-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;
  position: relative;
}
.action-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px -8px rgba(0,0,0,0.1); }
.card-icon-wrapper {
  width: 48px; height: 48px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; }
.card-footer h3 { margin: 0; font-size: 16px; font-weight: 600; color: #0f172a; }
.arrow-icon { color: #94a3b8; opacity: 0; transition: all 0.3s ease; }
.action-card:hover .arrow-icon { opacity: 1; transform: translateX(5px); }

/* Colors */
.tone-sky { --theme-color: #0ea5e9; }
.tone-kelp { --theme-color: #10b981; }
.tone-sand { --theme-color: #f59e0b; }
.tone-ink { --theme-color: #6366f1; }
.action-card[class*="tone-"] .card-icon-wrapper { background: color-mix(in srgb, var(--theme-color) 15%, transparent); color: var(--theme-color); }
.action-card[class*="tone-"]:hover { border-color: var(--theme-color); }

@media (max-width: 900px) {
  .hero-content, .hub-strip { grid-template-columns: 1fr; }
}
</style>