<template>
  <div class="file-preview-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">{{ fileName }}</h2>
          <p class="sub">文件预览</p>
        </div>
        <div class="actions">
          <el-button @click="downloadCurrentFile" :disabled="!fileId">下载</el-button>
          <el-button type="primary" @click="refresh">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading" shadow="never" class="preview-card">
      <template v-if="error">
        <el-empty :description="error" />
      </template>

      <template v-else-if="!fileId">
        <el-empty description="缺少文件ID" />
      </template>

      <template v-else>
        <iframe
          :src="previewUrl"
          class="preview-frame"
          frameborder="0"
          title="file-preview"
        />
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import fileService from '@/services/fileService'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const fileInfo = ref(null)

const fileId = computed(() => route.params.id)
const previewUrl = computed(() => (fileId.value ? `/api/files/${fileId.value}/preview` : ''))
const fileName = computed(() => fileInfo.value?.originalName || fileInfo.value?.name || `文件 #${fileId.value || ''}`)

const loadFile = async () => {
  if (!fileId.value) {
    error.value = '缺少文件ID'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fileService.getFileById(fileId.value)
    fileInfo.value = res || null
  } catch (e) {
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
    const a = document.createElement('a')
    a.href = url
    a.download = fileName.value || `file-${fileId.value}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '下载失败')
  }
}

watch(() => route.params.id, () => {
  loadFile()
})

onMounted(() => {
  loadFile()
})
</script>

<style scoped>
.file-preview-page {
  display: grid;
  gap: 16px;
}

.header-card,
.preview-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.sub {
  margin: 4px 0 0;
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 8px;
}

.preview-frame {
  width: 100%;
  min-height: 70vh;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
