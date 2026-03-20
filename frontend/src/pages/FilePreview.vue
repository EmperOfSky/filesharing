<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import fileService from '@/services/fileService'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const route = useRoute()

const loading = ref(false)
const error = ref('')
const fileInfo = ref<any | null>(null)

const fileId = computed(() => route.params.id)
const previewUrl = computed(() => (fileId.value ? `/api/files/${fileId.value}/preview` : ''))
const fileName = computed(() => fileInfo.value?.originalName || fileInfo.value?.name || `文件 #${fileId.value || ''}`)

const loadFile = async () => {
  if (!fileId.value) {
    error.value = '缺少文件 ID'
    return
  }

  loading.value = true
  error.value = ''

  try {
    fileInfo.value = await fileService.getFileById(fileId.value)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载文件信息失败'
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  loadFile()
}

const downloadCurrentFile = async () => {
  if (!fileId.value) return
  try {
    const blob = await fileService.downloadFile(fileId.value)
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName.value || `file-${fileId.value}`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '下载失败')
  }
}

watch(
  () => route.params.id,
  () => {
    loadFile()
  }
)

onMounted(() => {
  loadFile()
})
</script>

<template>
  <div class="modern-workspace file-preview-page">
    <!-- 顶部 Header 卡片 -->
    <header class="workspace-header base-card">
      <div class="header-content">
        <div class="header-intro">
          <div class="kicker-tag">Preview Panel</div>
          <h1 class="title">{{ fileName }}</h1>
          <p class="description">在线预览文件内容，支持原文档查看与快速下载，体验流畅的无缝协作。</p>
          <div class="header-actions" style="margin-top: 24px;">
            <el-button color="#0ea5e9" type="primary" size="large" @click="refresh" round>
              <el-icon class="el-icon--left"><Refresh /></el-icon>刷新预览
            </el-button>
            <el-button size="large" @click="downloadCurrentFile" :disabled="!fileId" round plain>
              <el-icon class="el-icon--left"><Download /></el-icon>下载文件
            </el-button>
          </div>
        </div>

        <div class="header-stats">
          <UiStatCard label="文件ID" :value="Array.isArray(fileId) ? fileId.join(',') : (fileId || '--')" />
          <UiStatCard label="预览状态" :value="error ? '异常' : '就绪'" />
        </div>
      </div>
    </header>

    <!-- 主体预览区域 -->
    <main class="workspace-main base-card preview-shell" v-loading="loading">
      <div v-if="error" class="empty-state">
        <el-empty :description="error" image-size="120" />
      </div>

      <div v-else-if="!fileId" class="empty-state">
        <el-empty description="缺少文件 ID" image-size="120" />
      </div>

      <div v-else class="preview-viewport">
        <iframe
          :src="previewUrl"
          class="preview-frame"
          frameborder="0"
          title="file-preview"
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 全局变量一致性 */
.modern-workspace {
  --primary-color: #0ea5e9;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --text-main: #0f172a;
  --text-secondary: #64748b;
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

/* Header */
.header-content { display: flex; justify-content: space-between; align-items: center; gap: 40px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0f2fe; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 700; margin: 12px 0; color: var(--text-main); }
.header-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; min-width: 350px; }

/* Preview Workspace */
.preview-shell {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 70vh; /* 给予高度 */
  overflow: hidden;
  background: #f1f5f9; /* 区分背景 */
}

.preview-viewport {
  width: 100%;
  height: 100%;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1); /* 增加悬浮纸张感 */
}

.empty-state {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

@media (max-width: 900px) {
  .header-content { flex-direction: column; align-items: flex-start; }
  .header-stats { width: 100%; }
}
</style>