<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFileStore } from '@/stores/file'
import fileService from '@/services/fileService'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const fileStore = useFileStore()
const router = useRouter()

const loading = ref(false)
const searchKeyword = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)

const shareDialogVisible = ref(false)
const shareSubmitting = ref(false)
const currentShareFile = ref<any | null>(null)
const shareResult = ref<any | null>(null)
const shareForm = ref({
  title: '',
  description: '',
  password: '',
  expireHours: 24,
  maxAccessCount: 0,
  allowDownload: true
})

const breadcrumbItems = computed(() => {
  const items: Array<{ name: string; id: number | null }> = [{ name: '全部文件', id: null }]
  if (fileStore.currentFolderId !== null) {
    const currentFolder = fileStore.folders.find((folder) => folder.id === fileStore.currentFolderId)
    if (currentFolder) {
      items.push({ name: currentFolder.name, id: currentFolder.id })
    }
  }
  return items
})

const mergedList = computed(() =>[
  ...fileStore.currentFolders.map((folder) => ({
    ...folder,
    displayName: folder.name,
    isFolder: true,
    type: 'folder'
  })),
  ...fileStore.currentFiles.map((file) => ({
    ...file,
    displayName: file.originalName,
    isFolder: false,
    type: 'file'
  }))
])

const visibleList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return mergedList.value
  return mergedList.value.filter((item) => String(item.displayName || '').toLowerCase().includes(keyword))
})

const stats = computed(() => ({
  currentItems: visibleList.value.length,
  folders: fileStore.currentFolders.length,
  files: fileStore.currentFiles.length,
  sampledSize: fileStore.currentFiles.reduce((sum, file) => sum + Number(file.fileSize || 0), 0)
}))

const beforeUpload = (file: File) => {
  const isWithinLimit = file.size / 1024 / 1024 < 1024 * 10
  if (!isWithinLimit) {
    ElMessage.error('文件体积过大，请选择更小的文件')
    return false
  }
  return true
}

const formatFileSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units =['B', 'KB', 'MB', 'GB', 'TB']
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

const loadFileData = async () => {
  loading.value = true
  try {
    await Promise.all([fileStore.loadFiles(0, 100), fileStore.loadFolders()])
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载文件列表失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const triggerFileSelect = () => {
  fileInput.value?.click()
}

const onFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!beforeUpload(file)) {
    input.value = ''
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const response = await fileService.chunkUpload(file, {
      chunkSize: 5 * 1024 * 1024,
      concurrency: 3,
      maxRetries: 3,
      folderId: fileStore.currentFolderId,
      onProgress: (progress: number) => {
        uploadProgress.value = progress
      }
    })

    ElMessage.success('文件上传完成')
    if (response?.sha256) {
      ElMessage.info(`SHA-256: ${response.sha256}`)
    }
    await loadFileData()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '分片上传失败'
    ElMessage.error(message)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    input.value = ''
  }
}

const navigateToFolder = async (folderId: number | null) => {
  fileStore.navigateToFolder(folderId)
  await loadFileData()
}

const handleBreadcrumbClick = async (folderId: number | null) => {
  await navigateToFolder(folderId)
}

const handleRowEnter = async (row: any) => {
  if (row.isFolder) {
    await navigateToFolder(row.id)
    return
  }
  router.push(`/dashboard/preview/${row.id}`)
}

const handleDownload = async (row: any) => {
  try {
    const blob = await fileService.downloadFile(row.id)
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = row.originalName || row.name || `file-${row.id}`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '下载失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要将${row.isFolder ? '文件夹' : '文件'}“${row.name || row.originalName}”移入回收站吗？`,
      '移入回收站',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (row.isFolder) {
      await fileStore.deleteFolder(row.id)
    } else {
      await fileStore.deleteFile(row.id)
    }

    ElMessage.success('已移入回收站')
    await loadFileData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '删除失败')
    }
  }
}

const handleCreateFolder = async () => {
  try {
    const folderName = await ElMessageBox.prompt('请输入新文件夹名称', '新建文件夹', {
      confirmButtonText: '创建',
      cancelButtonText: '取消'
    })

    if (!folderName.value) return

    await fileStore.createFolder(folderName.value)
    ElMessage.success('文件夹创建成功')
    await loadFileData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '创建文件夹失败')
    }
  }
}

const openShareDialog = (row: any) => {
  currentShareFile.value = row
  shareResult.value = null
  shareForm.value = {
    title: row.originalName || '',
    description: '',
    password: '',
    expireHours: 24,
    maxAccessCount: 0,
    allowDownload: true
  }
  shareDialogVisible.value = true
}

const normalizeShareLink = (link: string) => {
  if (!link) return ''
  try {
    const parsed = new URL(link)
    if (parsed.pathname.startsWith('/s/')) {
      return `${window.location.origin}${parsed.pathname}`
    }
    return link
  } catch {
    if (link.startsWith('/s/')) {
      return `${window.location.origin}${link}`
    }
    return link
  }
}

const createShortShareLink = async () => {
  if (!currentShareFile.value?.id) {
    ElMessage.error('请选择需要分享的文件')
    return
  }

  shareSubmitting.value = true

  try {
    let expireTime: string | null = null
    if (shareForm.value.expireHours > 0) {
      const date = new Date(Date.now() + shareForm.value.expireHours * 60 * 60 * 1000)
      expireTime = date.toISOString().slice(0, 19)
    }

    const data = await fileService.getFileShareLink(currentShareFile.value.id, {
      title: shareForm.value.title,
      description: shareForm.value.description,
      password: shareForm.value.password,
      expireTime,
      maxAccessCount: shareForm.value.maxAccessCount,
      allowDownload: shareForm.value.allowDownload
    })

    shareResult.value = {
      ...data,
      shortLink: normalizeShareLink(data?.shortLink || '')
    }
    ElMessage.success('短链创建成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '创建分享失败'
    ElMessage.error(message)
  } finally {
    shareSubmitting.value = false
  }
}

const copyShareLink = async () => {
  const link = shareResult.value?.shortLink || ''
  if (!link) {
    ElMessage.warning('暂无可复制的短链')
    return
  }

  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('短链已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  loadFileData()
})
</script>

<template>
  <div class="modern-workspace">
    <!-- 顶部卡片：介绍与核心数据 -->
    <header class="workspace-header base-card">
      <div class="header-content">
        <div class="header-intro">
          <div class="kicker-tag">File Workspace</div>
          <h1 class="title">我的文件工作区</h1>
          <div class="header-actions">
            <el-button color="#0ea5e9" type="primary" size="large" @click="triggerFileSelect" :disabled="uploading || loading" round>
              <el-icon class="el-icon--left"><UploadFilled /></el-icon> 上传文件
            </el-button>
            <el-button size="large" @click="handleCreateFolder" :disabled="loading" round>
              新建文件夹
            </el-button>
            <el-button size="large" @click="router.push('/dashboard/shares')" round plain>
              分享管理
            </el-button>
          </div>
          <!-- 上传进度条优化展示 -->
          <div v-if="uploading" class="upload-progress-wrap">
            <div class="progress-info">
              <span>文件上传中...</span>
              <span>{{ uploadProgress }}%</span>
            </div>
            <el-progress :percentage="uploadProgress" :stroke-width="8" :show-text="false" color="#0ea5e9" />
          </div>
        </div>
        
        <!-- 侧边统计数据块 -->
        <div class="header-stats">
          <UiStatCard label="当前项数" :value="stats.currentItems" hint="包含目录与文件" />
          <UiStatCard label="文件夹" :value="stats.folders" hint="当前层级目录" />
          <UiStatCard label="文件" :value="stats.files" hint="支持预览/下载" />
          <UiStatCard label="样本容量" :value="formatFileSize(stats.sampledSize)" hint="当前页面加载大小" />
        </div>
      </div>
    </header>

    <!-- 主体内容卡片：文件列表与操作 -->
    <main class="workspace-main base-card">
      <!-- 操作控制栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-breadcrumb separator="/" class="modern-breadcrumb">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbItems" :key="`${item.name}_${index}`">
              <span class="breadcrumb-item" :class="{ 'is-active': index === breadcrumbItems.length - 1 }" @click="handleBreadcrumbClick(item.id)">
                {{ item.name }}
              </span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="toolbar-right">
          <input ref="fileInput" type="file" class="hidden-file-input" @change="onFileSelected" />
          <el-input
            v-model="searchKeyword"
            clearable
            placeholder="搜索当前目录..."
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <div class="action-icons">
            <el-tooltip content="刷新列表" placement="top">
              <el-button circle @click="loadFileData" :disabled="loading">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="回收站" placement="top">
              <el-button circle @click="router.push('/dashboard/recycle-bin')">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 文件列表表格 -->
      <div class="table-container">
        <el-table
          :data="visibleList"
          :loading="loading"
          style="width: 100%"
          class="modern-table"
          @row-dblclick="handleRowEnter"
        >
          <template #empty>
            <div class="empty-state">
              <el-empty description="此文件夹为空">
                <el-button type="primary" plain @click="triggerFileSelect">上传首个文件</el-button>
              </el-empty>
            </div>
          </template>

          <el-table-column label="名称" min-width="350">
            <template #default="{ row }">
              <div class="file-item">
                <div class="file-icon" :class="{ 'is-folder': row.isFolder }">
                  <el-icon><component :is="row.isFolder ? 'Folder' : getFileIcon(row.extension)" /></el-icon>
                </div>
                <div class="file-info">
                  <span class="file-name">{{ row.name || row.originalName }}</span>
                  <span class="file-meta">{{ row.isFolder ? '文件夹' : `${row.extension?.toUpperCase() || '未知'} 文件` }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="大小" width="150">
            <template #default="{ row }">
              <span class="text-secondary">{{ row.isFolder ? '--' : formatFileSize(row.fileSize) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="修改日期" width="200">
            <template #default="{ row }">
              <span class="text-secondary">{{ formatDate(row.updatedAt || row.createdAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" fixed="right" align="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button v-if="row.isFolder" size="small" type="primary" plain round @click="handleRowEnter(row)">进入</el-button>
                <template v-else>
                  <el-tooltip content="预览" placement="top">
                    <el-button circle size="small" @click="router.push(`/dashboard/preview/${row.id}`)">
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="下载" placement="top">
                    <el-button circle size="small" type="success" plain @click="handleDownload(row)">
                      <el-icon><Download /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="分享" placement="top">
                    <el-button circle size="small" type="warning" plain @click="openShareDialog(row)">
                      <el-icon><Share /></el-icon>
                    </el-button>
                  </el-tooltip>
                </template>
                <el-tooltip content="删除" placement="top">
                  <el-button circle size="small" type="danger" plain @click="handleDelete(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>

    <!-- 弹窗样式优化 -->
    <el-dialog v-model="shareDialogVisible" title="🌟 创建专属短链分享" width="560px" class="modern-dialog" destroy-on-close>
      <div class="dialog-subtitle">分享当前文件：<span class="highlight">{{ currentShareFile?.originalName || '-' }}</span></div>
      
      <el-form label-position="top" :model="shareForm" class="modern-form">
        <el-form-item label="分享标题">
          <el-input v-model="shareForm.title" maxlength="100" placeholder="给您的分享起个名字" />
        </el-form-item>
        
        <el-form-item label="描述说明 (可选)">
          <el-input v-model="shareForm.description" type="textarea" :rows="3" placeholder="添加一些描述信息..." resize="none" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="提取密码 (留空则公开)" class="flex-1">
            <el-input v-model="shareForm.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-form-item label="有效期 (小时, 0为永久)" class="flex-1">
            <el-input-number v-model="shareForm.expireHours" :min="0" :max="720" class="full-width" controls-position="right"/>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="最大访问次数 (0为不限)" class="flex-1">
            <el-input-number v-model="shareForm.maxAccessCount" :min="0" :max="999999" class="full-width" controls-position="right"/>
          </el-form-item>
          <el-form-item label="允许对方下载" class="flex-1 flex-center-y">
            <el-switch v-model="shareForm.allowDownload" active-color="#0ea5e9" />
          </el-form-item>
        </div>

        <transition name="el-zoom-in-top">
          <div v-if="shareResult" class="share-result-card">
            <div class="result-header">
              <el-icon color="#10b981"><CircleCheckFilled /></el-icon>
              <span>短链生成成功</span>
            </div>
            <el-input :model-value="shareResult.shortLink" readonly class="copy-input">
              <template #append>
                <el-button type="primary" @click="copyShareLink">复制链接</el-button>
              </template>
            </el-input>
          </div>
        </transition>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="shareDialogVisible = false" round>取消</el-button>
          <el-button type="primary" color="#0ea5e9" :loading="shareSubmitting" @click="createShortShareLink" round>
            生成链接
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 全局变量与底色设定 */
.modern-workspace {
  --primary-color: #0ea5e9;
  --primary-light: #e0f2fe;
  --folder-color: #f59e0b;
  --folder-light: #fef3c7;
  --text-main: #0f172a;
  --text-regular: #334155;
  --text-muted: #64748b;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;
  
  background-color: var(--bg-page);
  min-height: calc(100vh - 60px); /* 假设有顶导，没有则改为 100vh */
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 隐藏的文件输入框 */
.hidden-file-input {
  display: none;
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

/* 顶部 Header */
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}

.header-intro {
  flex: 1;
  max-width: 600px;
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

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.header-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-width: 320px;
}

/* 进度条优化 */
.upload-progress-wrap {
  margin-top: 24px;
  background: #f1f5f9;
  padding: 16px 20px;
  border-radius: 12px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
  font-weight: 500;
}

/* 主体区域 */
.workspace-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
}

/* 控制台栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input {
  width: 280px;
}
:deep(.search-input .el-input__wrapper) {
  border-radius: 20px;
  background: #f8fafc;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.3s ease;
}
:deep(.search-input .el-input__wrapper.is-focus) {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light) !important;
}

.action-icons {
  display: flex;
  gap: 8px;
}

/* 面包屑美化 */
.modern-breadcrumb .breadcrumb-item {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}
.modern-breadcrumb .breadcrumb-item:hover {
  color: var(--primary-color);
  background: var(--primary-light);
}
.modern-breadcrumb .breadcrumb-item.is-active {
  color: var(--text-main);
  font-weight: 600;
  cursor: default;
  background: none;
}

/* 表格深度定制 (覆写 Element Plus 默认样式) */
.table-container {
  flex: 1;
}
.modern-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f1f5f9;
}
:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  color: var(--text-muted);
  font-size: 13px;
  padding: 12px 0;
}
:deep(.el-table td.el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}
:deep(.el-table__row) {
  transition: background-color 0.2s ease, transform 0.2s ease;
}
:deep(.el-table__row:hover) {
  transform: translateY(-1px);
}

/* 列表文件行视觉 */
.file-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.file-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: var(--primary-light);
  color: var(--primary-color);
}
.file-icon.is-folder {
  background: var(--folder-light);
  color: var(--folder-color);
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
  transition: color 0.2s;
}
.file-item:hover .file-name {
  color: var(--primary-color);
}
.file-meta {
  font-size: 12px;
  color: var(--text-muted);
}
.text-secondary {
  color: var(--text-muted);
  font-size: 14px;
}
.row-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
  opacity: 0.3;
  transition: opacity 0.3s ease;
}
:deep(.el-table__row:hover) .row-actions {
  opacity: 1;
}

.empty-state {
  padding: 60px 0;
}

/* 弹窗及表单优化 */
:deep(.modern-dialog) {
  border-radius: 20px;
  overflow: hidden;
}
:deep(.modern-dialog .el-dialog__header) {
  background: #f8fafc;
  margin-right: 0;
  padding: 24px;
  border-bottom: 1px solid var(--border-color);
}
:deep(.modern-dialog .el-dialog__title) {
  font-weight: 700;
  font-size: 18px;
}
:deep(.modern-dialog .el-dialog__body) {
  padding: 24px;
}

.dialog-subtitle {
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--text-muted);
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 8px;
}
.dialog-subtitle .highlight {
  color: var(--primary-color);
  font-weight: 600;
}

.modern-form .form-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}
.modern-form .flex-1 {
  flex: 1;
}
.full-width {
  width: 100%;
}
.flex-center-y {
  display: flex;
  align-items: center;
  height: 32px;
}

.share-result-card {
  margin-top: 24px;
  padding: 20px;
  border-radius: 12px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #065f46;
  font-weight: 600;
  margin-bottom: 12px;
}

/* 响应式调整 */
@media (max-width: 900px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-stats {
    width: 100%;
    grid-template-columns: 1fr 1fr;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .toolbar-right {
    flex-wrap: wrap;
    justify-content: space-between;
  }
  .search-input {
    width: 100%;
    flex: 1;
  }
}
</style>
