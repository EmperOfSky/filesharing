<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import fileCodeBoxService, { type FileCodeBoxSelectResult } from '@/services/fileCodeBox'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const route = useRoute()
const router = useRouter()

const PICKUP_HISTORY_KEY = 'fcb_pickup_history'
const PICKUP_CODE_LENGTH = 32

const pickupCode = ref('')
const selecting = ref(false)
const selected = ref<FileCodeBoxSelectResult | null>(null)
const histories = ref<PickupHistoryItem[]>([])

interface PickupHistoryItem {
  id: string
  code: string
  name: string
  size: number
  createdAt: string
  isFile: boolean
}

const selectedIsDownload = computed(() => {
  if (!selected.value) return false
  return typeof selected.value.text === 'string' && selected.value.text.startsWith('/api/public/share/download')
})

const selectedDownloadUrl = computed(() => {
  if (!selected.value || !selectedIsDownload.value) return ''
  return `${window.location.origin}${selected.value.text}`
})

const selectedTextPreview = computed(() => {
  if (!selected.value || selectedIsDownload.value) return ''
  const text = selected.value.text || ''
  return text.length > 1200 ? `${text.slice(0, 1200)}...` : text
})

const latestHistory = computed(() => histories.value[0] || null)
const fileHistoryCount = computed(() => histories.value.filter((item) => item.isFile).length)
const textHistoryCount = computed(() => histories.value.filter((item) => !item.isFile).length)

const safeJsonParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const loadHistory = () => {
  histories.value = safeJsonParse<PickupHistoryItem[]>(localStorage.getItem(PICKUP_HISTORY_KEY), [])
}

const saveHistory = () => {
  localStorage.setItem(PICKUP_HISTORY_KEY, JSON.stringify(histories.value.slice(0, 30)))
}

const clearHistory = () => {
  histories.value = []
  saveHistory()
}

const pushHistory = (code: string, detail: FileCodeBoxSelectResult) => {
  const item: PickupHistoryItem = {
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    code,
    name: detail.name,
    size: detail.size,
    createdAt: new Date().toISOString(),
    isFile: typeof detail.text === 'string' && detail.text.startsWith('/api/public/share/download')
  }

  histories.value = [item, ...histories.value.filter((entry) => entry.code !== item.code)]
  saveHistory()
}

const formatDateTime = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const copyCode = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('取件码已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const useHistoryCode = (code: string) => {
  pickupCode.value = code
}

const selectByCode = async () => {
  const code = pickupCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入取件码')
    return
  }
  if (code.length !== PICKUP_CODE_LENGTH) {
    ElMessage.warning(`取件码必须为 ${PICKUP_CODE_LENGTH} 位`)
    return
  }

  selecting.value = true

  try {
    const detail = await fileCodeBoxService.selectByCode(code)
    selected.value = detail
    pushHistory(code, detail)
    router.replace({ path: route.path, query: { code } })
    ElMessage.success('取件成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '取件失败'
    ElMessage.error(message)
  } finally {
    selecting.value = false
  }
}

const downloadFile = () => {
  if (!selected.value || !selectedIsDownload.value) return
  window.open(selectedDownloadUrl.value, '_blank')
}

onMounted(async () => {
  loadHistory()
  const queryCode = route.query.code
  if (typeof queryCode === 'string' && queryCode.trim()) {
    pickupCode.value = queryCode.trim()
    await selectByCode()
  }
})
</script>

<template>
  <div class="modern-workspace">
    <!-- 顶部卡片 -->
    <header class="base-card welcome-hero">
      <div class="hero-content">
        <div class="hero-text">
          <span class="kicker-tag">Pickup Space</span>
          <h1 class="title">快传取件空间</h1>
          <p class="description">输入取件码，一键获取团队共享的文件或文本内容。</p>
        </div>
        <div class="hero-stats">
          <UiStatCard label="总记录" :value="histories.length" />
          <UiStatCard label="文件记录" :value="fileHistoryCount" />
          <UiStatCard label="文本记录" :value="textHistoryCount" />
          <UiStatCard label="最近代码" :value="latestHistory?.code || '--'" />
        </div>
      </div>
    </header>

    <!-- 主体布局 -->
    <div class="workspace-layout">
      <main class="main-content">
        <!-- 取件码输入区域 -->
        <section class="base-card pickup-card">
          <div class="panel-header">
            <h2 class="panel-title">输入取件码</h2>
            <el-button v-if="histories.length" text type="danger" @click="clearHistory">清空历史</el-button>
          </div>
          
          <div class="entry-box">
            <el-input
              v-model="pickupCode"
              size="large"
              placeholder="请输入 32 位取件码..."
              @keyup.enter="selectByCode"
              class="modern-input"
            />
            <el-button type="primary" color="#6366f1" size="large" :loading="selecting" @click="selectByCode" round>验证取件</el-button>
          </div>

          <div class="history-pills" v-if="histories.length">
            <span class="pill-label">最近:</span>
            <button v-for="item in histories.slice(0, 6)" :key="item.id" class="pill-chip" @click="useHistoryCode(item.code)">
              {{ item.code }}
            </button>
          </div>
        </section>

        <!-- 结果展示区域 -->
        <section class="base-card result-panel">
          <div class="panel-header">
            <h2 class="panel-title">取件结果</h2>
            <el-button v-if="selected" plain @click="copyCode(pickupCode)" round>复制取件码</el-button>
          </div>

          <div v-if="selected" class="result-content">
            <div class="result-overview">
              <div class="stat-box"><span>名称</span><strong>{{ selected.name }}</strong></div>
              <div class="stat-box"><span>大小</span><strong>{{ formatSize(selected.size) }}</strong></div>
              <div class="stat-box"><span>类型</span><strong>{{ selectedIsDownload ? '文件' : '文本' }}</strong></div>
            </div>

            <div class="result-body">
              <template v-if="selectedIsDownload">
                <div class="file-download-zone">
                  <p>检测到文件资源，您可以立即下载。</p>
                  <el-input :model-value="selectedDownloadUrl" readonly class="url-input" />
                  <el-button type="primary" color="#6366f1" size="large" @click="downloadFile" round>
                    <el-icon><Download /></el-icon> 下载文件
                  </el-button>
                </div>
              </template>

              <template v-else>
                <el-input :model-value="selectedTextPreview" type="textarea" :rows="12" readonly class="text-preview" />
              </template>
            </div>
          </div>
          <el-empty v-else description="输入取件码后，结果将在此展示" image-size="120" />
        </section>
      </main>

      <!-- 侧边栏：历史记录 -->
      <aside class="sidebar-panel">
        <div class="base-card sticky-card">
          <h2 class="panel-title">取件历史</h2>
          <div v-if="histories.length" class="history-list custom-scrollbar">
            <div v-for="item in histories" :key="item.id" class="history-card" @click="useHistoryCode(item.code)">
              <div class="card-head">
                <span class="mono-badge">{{ item.code }}</span>
                <el-tag size="small" :type="item.isFile ? 'success' : 'info'" round>{{ item.isFile ? '文件' : '文本' }}</el-tag>
              </div>
              <div class="card-title">{{ item.name }}</div>
              <div class="card-meta">{{ formatSize(item.size) }} · {{ formatDateTime(item.createdAt) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无记录" :image-size="60" />
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.modern-workspace {
  --primary-color: #6366f1;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;
  
  background-color: var(--bg-page);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 100vh;
}

.base-card { background: var(--bg-card); border-radius: 20px; box-shadow: 0 4px 24px -4px rgba(0, 0, 0, 0.03); border: 1px solid var(--border-color); padding: 32px; }

/* Hero */
.welcome-hero { padding: 40px; }
.hero-content { display: flex; justify-content: space-between; align-items: center; gap: 40px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 700; color: #0f172a; margin: 12px 0; }
.description { color: #64748b; font-size: 14px; }
.hero-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; min-width: 320px; }

/* Layout */
.workspace-layout { display: grid; grid-template-columns: 1fr 340px; gap: 24px; align-items: start; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.panel-title { font-size: 18px; font-weight: 700; margin: 0; }

/* Entry Box */
.entry-box { display: flex; gap: 12px; }
.modern-input :deep(.el-input__wrapper) { border-radius: 12px; padding-left: 16px; }

.history-pills { margin-top: 16px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pill-label { font-size: 13px; color: var(--text-muted); }
.pill-chip { cursor: pointer; background: #f1f5f9; border: none; padding: 4px 12px; border-radius: 16px; font-size: 12px; color: #475569; transition: all 0.2s; }
.pill-chip:hover { background: var(--primary-color); color: #fff; }

/* Results */
.result-overview { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-box { background: #f8fafc; padding: 16px; border-radius: 12px; display: flex; flex-direction: column; gap: 4px; }
.stat-box span { font-size: 12px; color: #94a3b8; }
.stat-box strong { font-size: 14px; color: #0f172a; }

.file-download-zone { text-align: center; padding: 24px; border: 2px dashed #e2e8f0; border-radius: 16px; }
.url-input { margin: 16px 0; }

/* History List */
.history-card { cursor: pointer; padding: 16px; border-radius: 12px; border: 1px solid var(--border-color); transition: all 0.2s; margin-bottom: 12px; }
.history-card:hover { border-color: var(--primary-color); background: #f8fafc; }
.card-head { display: flex; justify-content: space-between; margin-bottom: 8px; }
.mono-badge { font-family: monospace; font-size: 13px; font-weight: 600; background: #e2e8f0; padding: 2px 6px; border-radius: 4px; }
.card-title { font-size: 14px; font-weight: 600; color: #0f172a; margin: 0 0 4px 0; }
.card-meta { font-size: 12px; color: #94a3b8; }

@media (max-width: 1080px) {
  .workspace-layout { grid-template-columns: 1fr; }
  .sticky-card { position: static; }
}
</style>