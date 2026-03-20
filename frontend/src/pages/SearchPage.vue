<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, FolderOpened } from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/file'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const router = useRouter()
const fileStore = useFileStore()

const loading = ref(false)
const searchResults = ref<any[]>([])
const searchPerformed = ref(false)

const searchForm = reactive({
  keyword: '',
  fileType: '',
  minSize: '',
  maxSize: ''
})

const fileTypes = [
  { label: '全部类型', value: '' },
  { label: '文档', value: 'doc,pdf,txt' },
  { label: '图片', value: 'jpg,jpeg,png,gif' },
  { label: '视频', value: 'mp4,avi,mov' },
  { label: '音频', value: 'mp3,wav,flac' },
  { label: '压缩包', value: 'zip,rar,7z' }
]

const resultStats = computed(() => ({
  total: searchResults.value.length,
  files: searchResults.value.filter((item) => !item.isFolder).length,
  folders: searchResults.value.filter((item) => item.isFolder).length
}))

const handleSearch = async () => {
  if (!searchForm.keyword.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  loading.value = true
  searchPerformed.value = true

  try {
    const results = await fileStore.searchFiles(searchForm.keyword, searchForm.fileType)
    searchResults.value = [
      ...(results.folders || []).map((folder) => ({ ...folder, isFolder: true })),
      ...(results.files || []).map((file) => ({ ...file, isFolder: false }))
    ]
    ElMessage.success(`找到 ${searchResults.value.length} 条结果`)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '搜索失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.fileType = ''
  searchForm.minSize = ''
  searchForm.maxSize = ''
  searchResults.value = []
  searchPerformed.value = false
}

const formatFileSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '--'
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return dateString
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

const openResult = (row: any) => {
  if (row.isFolder) {
    router.push('/dashboard/files')
    return
  }
  router.push(`/dashboard/preview/${row.id}`)
}
</script>

<template>
  <div class="modern-search-page">
    <!-- Header -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Discovery</span>
          <h1 class="title">全站搜索</h1>
          <p class="subtitle">通过关键词、类型与大小限制，快速检索您的文件与文件夹。</p>
        </div>
        <div class="header-actions">
          <el-button color="#6366f1" type="primary" size="large" @click="handleSearch" round>
            <el-icon class="el-icon--left"><Search /></el-icon>立刻搜索
          </el-button>
          <el-button size="large" @click="router.push('/dashboard/files')" round>
            <el-icon class="el-icon--left"><FolderOpened /></el-icon>文件管理
          </el-button>
        </div>
      </div>
      
      <div class="header-stats-strip">
        <UiStatCard label="当前状态" :value="searchPerformed ? '已完成' : '待处理'" />
        <el-divider direction="vertical" />
        <UiStatCard label="匹配结果数" :value="resultStats.total" />
        <el-divider direction="vertical" />
        <UiStatCard label="包含文件" :value="resultStats.files" />
        <el-divider direction="vertical" />
        <UiStatCard label="包含文件夹" :value="resultStats.folders" />
      </div>
    </header>

    <!-- Workspace -->
    <div class="workspace-layout">
      <!-- Sidebar/Filter -->
      <aside class="filter-column">
        <div class="base-card filter-card">
          <h2 class="widget-title">条件筛选</h2>
          <el-form :model="searchForm" label-position="top" class="filter-form">
            <el-form-item label="关键词定位">
              <el-input v-model="searchForm.keyword" size="large" placeholder="输入文件名或目录名" @keyup.enter="handleSearch">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="目标类型">
              <el-select v-model="searchForm.fileType" size="large" style="width: 100%">
                <el-option v-for="type in fileTypes" :key="type.value" :label="type.label" :value="type.value" />
              </el-select>
            </el-form-item>

            <div class="grid-2">
              <el-form-item label="尺寸下限">
                <el-input v-model="searchForm.minSize" size="large" placeholder="例如: 1MB" />
              </el-form-item>
              <el-form-item label="尺寸上限">
                <el-input v-model="searchForm.maxSize" size="large" placeholder="例如: 500MB" />
              </el-form-item>
            </div>

            <div class="filter-actions mt-16">
              <el-button type="primary" color="#6366f1" size="large" :loading="loading" @click="handleSearch" class="flex-1" round>执行查询</el-button>
              <el-button size="large" @click="handleReset" class="flex-1" round>重置</el-button>
            </div>
          </el-form>
        </div>
      </aside>

      <!-- Main/Results -->
      <main class="results-column base-card">
        <div class="results-header">
          <h2 class="widget-title">检索结果</h2>
        </div>
        
        <div class="table-container">
          <el-table v-if="searchResults.length > 0" :data="searchResults" :loading="loading" stripe class="modern-table">
            <el-table-column label="资源名称" min-width="320">
              <template #default="{ row }">
                <div class="result-row">
                  <div class="result-icon" :class="{ folder: row.isFolder }">
                    <el-icon><component :is="row.isFolder ? 'Folder' : getFileIcon(row.extension)" /></el-icon>
                  </div>
                  <div class="result-info">
                    <div class="result-name">{{ row.name || row.originalName }}</div>
                    <div class="result-meta">{{ row.isFolder ? '文件夹' : formatFileSize(row.fileSize) }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="创建日期" width="190">
              <template #default="{ row }">
                <span class="date-text">{{ formatDate(row.createdAt) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="快捷操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" class="operation-btn" @click="openResult(row)">
                  {{ row.isFolder ? '打开空间' : '预览详情' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else-if="!loading" class="empty-state">
            <el-empty :description="searchPerformed ? '未找到符合条件的资源' : '在此快速定位并检索内容'" :image-size="90">
              <el-button v-if="searchPerformed" type="primary" color="#6366f1" @click="handleReset" round>清除检索条件</el-button>
            </el-empty>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.modern-search-page {
  --primary-color: #6366f1;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  background: var(--bg-page);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 100vh;
}

.base-card { background: var(--bg-card); border-radius: 16px; border: 1px solid #e2e8f0; padding: 24px; }

/* Header */
.app-header { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: start; margin-bottom: 24px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 800; color: #0f172a; margin: 12px 0; }
.subtitle { color: #64748b; font-size: 14px; margin: 0; }
.header-stats-strip { display: flex; align-items: center; gap: 32px; border-top: 1px solid #e2e8f0; padding-top: 24px; overflow-x: auto; }

/* Layout Grid */
.workspace-layout { display: grid; grid-template-columns: 340px 1fr; gap: 24px; align-items: start; }
.filter-card { position: sticky; top: 24px; }

.widget-title { font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 20px 0; }

/* Filter Form */
.filter-form :deep(.el-form-item__label) { font-weight: 600; color: #334155; padding-bottom: 8px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.filter-actions { display: flex; gap: 12px; }
.flex-1 { flex: 1; }
.mt-16 { margin-top: 16px; }

/* Main Table Area */
.results-column { min-height: 400px; display: flex; flex-direction: column; padding: 24px 0; }
.results-header { padding: 0 24px; margin-bottom: 20px; }
.table-container { flex: 1; padding: 0 24px; }

.result-row { display: flex; align-items: center; gap: 16px; }
.result-icon {
  width: 44px; height: 44px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 14px;
  background: #e0f2fe; color: #0284c7;
  font-size: 20px;
}
.result-icon.folder { background: #fef3c7; color: #d97706; }
.result-name { color: #0f172a; font-weight: 600; font-size: 15px; margin-bottom: 4px; word-break: break-all; }
.result-meta { color: #64748b; font-size: 13px; }
.date-text { color: #475569; font-size: 14px; }
.operation-btn { font-weight: 600; }

.empty-state { padding: 60px 0; }

/* Responsive adjustments */
@media (max-width: 1024px) {
  .workspace-layout { grid-template-columns: 1fr; }
  .filter-card { position: static; }
  .header-main { flex-direction: column; gap: 20px; }
  .header-actions { width: 100%; display: flex; }
  .header-actions .el-button { flex: 1; }
}
</style>
