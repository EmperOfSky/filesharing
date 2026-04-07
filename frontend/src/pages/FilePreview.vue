<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'
import fileService from '@/services/fileService'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const route = useRoute()

const loading = ref(false)
const error = ref('')
const fileInfo = ref<any | null>(null)
const previewBlobUrl = ref('')
const officeTextPreview = ref('')

const fileId = computed<number | null>(() => {
  const raw = route.params.id
  const first = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(first)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const previewUrl = computed(() => (fileId.value ? `/api/files/${fileId.value}/preview` : ''))
const fileName = computed(() => fileInfo.value?.originalName || fileInfo.value?.name || '未命名文件')
const fileIdDisplay = computed(() => (fileId.value ? `#${fileId.value}` : '--'))
const isImageFile = computed(() => {
  const contentType = (fileInfo.value?.contentType || '').toLowerCase()
  return contentType.startsWith('image/')
})
const isWordFile = computed(() => {
  const contentType = (fileInfo.value?.contentType || '').toLowerCase()
  const name = (fileInfo.value?.originalName || '').toLowerCase()
  return contentType.includes('msword')
    || contentType.includes('wordprocessingml')
    || name.endsWith('.doc')
    || name.endsWith('.docx')
})

const revokePreviewBlobUrl = () => {
  if (previewBlobUrl.value) {
    window.URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
}

const loadPreviewBlob = async () => {
  if (!fileId.value || !isImageFile.value) {
    revokePreviewBlobUrl()
    return
  }

  try {
    const blob = await fileService.getPreviewBlob(fileId.value)
    revokePreviewBlobUrl()
    previewBlobUrl.value = window.URL.createObjectURL(blob)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '加载图片预览失败'
  }
}

const loadOfficeTextPreview = async () => {
  if (!fileId.value || !isWordFile.value) {
    officeTextPreview.value = ''
    return
  }

  try {
    officeTextPreview.value = await fileService.getPreviewText(fileId.value)
  } catch (e: any) {
    officeTextPreview.value = ''
    error.value = e?.response?.data?.message || e?.message || '加载 Word 预览失败'
  }
}

const loadFile = async () => {
  if (!fileId.value) {
    error.value = '缺少文件 ID'
    return
  }

  loading.value = true
  error.value = ''

  try {
    fileInfo.value = await fileService.getFileById(fileId.value)
    await loadPreviewBlob()
    await loadOfficeTextPreview()
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
    // 兜底：若下载接口异常，复用当前已加载的预览二进制内容下载。
    if (previewBlobUrl.value) {
      try {
        const fallbackResp = await fetch(previewBlobUrl.value)
        const fallbackBlob = await fallbackResp.blob()
        const fallbackUrl = window.URL.createObjectURL(fallbackBlob)
        const anchor = document.createElement('a')
        anchor.href = fallbackUrl
        anchor.download = fileName.value || `file-${fileId.value}`
        document.body.appendChild(anchor)
        anchor.click()
        document.body.removeChild(anchor)
        window.URL.revokeObjectURL(fallbackUrl)
        ElMessage.warning('下载接口异常，已使用预览内容完成下载')
        return
      } catch {
        // ignore and use original error
      }
    }
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

onBeforeUnmount(() => {
  revokePreviewBlobUrl()
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
          <UiStatCard label="文件 ID" :value="fileIdDisplay" mono />
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
        <img
          v-if="isImageFile && previewBlobUrl"
          :src="previewBlobUrl"
          class="preview-image"
          :alt="fileName"
        />
        <div v-else-if="isWordFile" class="word-preview">
          <div class="word-preview-title">Word 文档文本预览</div>
          <pre class="word-preview-content">{{ officeTextPreview || '文档内容为空或暂不支持提取' }}</pre>
        </div>
        <iframe
          v-else
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

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.word-preview {
  width: 100%;
  height: 100%;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.word-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.word-preview-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
  flex: 1;
  font-size: 14px;
  line-height: 1.65;
  color: #0f172a;
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