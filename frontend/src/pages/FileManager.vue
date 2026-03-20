<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFileStore } from '@/stores/file'
import { ElMessage, ElMessageBox } from 'element-plus'
import fileService from '@/services/fileService'

const fileStore = useFileStore()
const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const fileList = ref<any[]>([])

// 上传状态
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0) // 0-100

// 分享状态
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

const triggerFileSelect = () => {
  fileInput.value?.click()
}

const onFileSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!beforeUpload(file)) {
    input.value = ''
    return
  }
  try {
    uploading.value = true
    uploadProgress.value = 0
    const resp = await fileService.chunkUpload(file, {
      chunkSize: 5 * 1024 * 1024,
      concurrency: 3,
      maxRetries: 3,
      onProgress: (p: number) => {
        uploadProgress.value = p
      },
      folderId: fileStore.currentFolderId
    })
    ElMessage.success('文件上传并合并完成')
    // 如果后端返回 sha256，显示
    if (resp && resp.sha256) {
      ElMessage.info('文件 SHA-256: ' + resp.sha256)
    }
    await loadFileData()
  } catch (error: any) {
    console.error(error)
    const resp = error?.response?.data
    const backendMessage = (typeof resp?.data === 'string' && resp.data) || resp?.message || resp?.error || error?.message
    ElMessage.error(backendMessage || '分片上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    input.value = ''
  }
}

// 计算属性
const breadcrumbItems = computed(() => {
  const items: Array<{ name: string; id: number | null }> = [{ name: '根目录', id: null }]
  if (fileStore.currentFolderId !== null) {
    const currentFolder = fileStore.folders.find(f => f.id === fileStore.currentFolderId)
    if (currentFolder) {
      items.push({ name: currentFolder.name, id: currentFolder.id })
    }
  }
  return items
})

const currentPath = computed(() => {
  return breadcrumbItems.value.map(item => item.name).join(' / ')
})

const beforeUpload = (file: File): boolean => {
  const isLtMax = file.size / 1024 / 1024 < 1024 * 10 // 10GB safeguard on client-side
  if (!isLtMax) {
    ElMessage.error('文件大小超出前端限制，请选择更小的文件')
    return false
  }
  return true
}

// 文件操作
const loadFileData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fileStore.loadFiles(),
      fileStore.loadFolders()
    ])
    fileList.value = [
      ...fileStore.currentFolders.map(folder => ({
        ...folder,
        isFolder: true,
        type: 'folder'
      })),
      ...fileStore.currentFiles.map(file => ({
        ...file,
        isFolder: false,
        type: 'file'
      }))
    ]
  } catch (error: any) {
    ElMessage.error(error.message || '加载文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleFolderClick = (folder: any) => {
  if (folder.isFolder) {
    fileStore.navigateToFolder(folder.id)
    loadFileData()
  }
}

const handleFileClick = (file: any) => {
  if (!file.isFolder) {
    // 可以添加文件预览逻辑
    ElMessage.info(`点击了文件: ${file.originalName}`)
  }
}

const openShareDialog = (file: any) => {
  currentShareFile.value = file
  shareResult.value = null
  shareForm.value = {
    title: file.originalName || '',
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
    ElMessage.error('未选择可分享的文件')
    return
  }

  try {
    shareSubmitting.value = true

    let expireTime: string | null = null
    if (shareForm.value.expireHours > 0) {
      const d = new Date(Date.now() + shareForm.value.expireHours * 60 * 60 * 1000)
      // 转成后端 LocalDateTime 可解析格式：yyyy-MM-ddTHH:mm:ss
      expireTime = d.toISOString().slice(0, 19)
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
    ElMessage.success('短链分享创建成功')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '创建分享失败'
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
    ElMessage.success('短链已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const goToShareManagePage = () => {
  router.push('/dashboard/shares')
}

const handleDelete = async (item: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除${item.isFolder ? '文件夹' : '文件'} "${item.name || item.originalName}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (item.isFolder) {
      await fileStore.deleteFolder(item.id)
    } else {
      await fileStore.deleteFile(item.id)
    }
    
    ElMessage.success('删除成功')
    loadFileData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleCreateFolder = async () => {
  try {
    const folderName = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    if (folderName.value) {
      await fileStore.createFolder(folderName.value)
      ElMessage.success('文件夹创建成功')
      loadFileData()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '创建文件夹失败')
    }
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString()
}

const getFileIcon = (extension: string) => {
  const iconMap: Record<string, string> = {
    'pdf': 'Document',
    'doc': 'Document',
    'docx': 'Document',
    'xls': 'Tickets',
    'xlsx': 'Tickets',
    'ppt': 'Monitor',
    'pptx': 'Monitor',
    'jpg': 'Picture',
    'jpeg': 'Picture',
    'png': 'Picture',
    'gif': 'Picture',
    'mp4': 'VideoCamera',
    'avi': 'VideoCamera',
    'mov': 'VideoCamera',
    'zip': 'Box',
    'rar': 'Box',
    'txt': 'Memo'
  }
  return iconMap[extension?.toLowerCase()] || 'Document'
}

// 面包屑导航
const handleBreadcrumbClick = (folderId: number | null) => {
  fileStore.navigateToFolder(folderId)
  loadFileData()
}

onMounted(() => {
  loadFileData()
})
</script>

<template>
  <div class="file-manager">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-section">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item 
          v-for="(item, index) in breadcrumbItems" 
          :key="index"
          @click="handleBreadcrumbClick(item.id)"
          class="breadcrumb-item"
        >
          {{ item.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      <div class="current-path">{{ currentPath }}</div>
    </div>
    
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 使用自定义分片上传：隐藏文件输入并通过 button 触发 -->
        <input ref="fileInput" type="file" style="display:none" @change="onFileSelected" />
        <el-button type="primary" :disabled="loading || uploading" @click="triggerFileSelect">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
        <div v-if="uploading" style="display:inline-block; margin-left:12px; vertical-align:middle; width:240px">
          <el-progress :percentage="uploadProgress" :text-inside="true"></el-progress>
        </div>
         
         <el-button 
           type="success" 
           @click="handleCreateFolder"
           :disabled="loading"
         >
           <el-icon><FolderAdd /></el-icon>
           新建文件夹
         </el-button>
         
         <el-button @click="loadFileData" :disabled="loading">
           <el-icon><Refresh /></el-icon>
           刷新
         </el-button>

         <el-button @click="goToShareManagePage" :disabled="loading">
           <el-icon><Share /></el-icon>
           分享管理
         </el-button>
       </div>
       
       <div class="toolbar-right">
         <el-input
           v-model="searchKeyword"
           placeholder="搜索文件..."
           style="width: 300px"
           clearable
         >
           <template #prefix>
             <el-icon><Search /></el-icon>
           </template>
         </el-input>
       </div>
     </div>

    <!-- 文件列表 -->
    <div class="file-list-container">
      <el-table 
        :data="fileList" 
        :loading="loading"
        stripe
        style="width: 100%"
        @row-click="handleFileClick"
      >
        <el-table-column prop="name" label="名称" min-width="300">
          <template #default="{ row }">
            <div class="file-item" @dblclick="handleFolderClick(row)">
              <el-icon class="file-icon" :class="{ 'folder-icon': row.isFolder }">
                <component :is="row.isFolder ? 'Folder' : getFileIcon(row.extension)" />
              </el-icon>
              <span class="file-name">{{ row.name || row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            <span v-if="!row.isFolder">{{ formatFileSize(row.fileSize) }}</span>
            <span v-else>--</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button 
              v-if="!row.isFolder"
              link 
              type="primary" 
              size="small"
              @click.stop
            >
              下载
            </el-button>
            <el-button
              v-if="!row.isFolder"
              link
              type="success"
              size="small"
              @click.stop="openShareDialog(row)"
            >
              分享
            </el-button>
            <el-button 
              link 
              type="danger" 
              size="small"
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 空状态 -->
    <div v-if="!loading && fileList.length === 0" class="empty-state">
      <el-empty description="暂无文件">
        <el-button type="primary" @click="loadFileData">刷新</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="shareDialogVisible" title="创建短链分享" width="560px">
      <el-form label-width="110px" :model="shareForm">
        <el-form-item label="分享文件">
          <span>{{ currentShareFile?.originalName || '-' }}</span>
        </el-form-item>
        <el-form-item label="分享标题">
          <el-input v-model="shareForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分享说明">
          <el-input v-model="shareForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input v-model="shareForm.password" type="password" show-password placeholder="可选，不填表示公开访问" maxlength="100" />
        </el-form-item>
        <el-form-item label="有效期(小时)">
          <el-input-number v-model="shareForm.expireHours" :min="0" :max="720" />
          <span style="margin-left: 8px; color: #999">0 表示不过期</span>
        </el-form-item>
        <el-form-item label="最大访问次数">
          <el-input-number v-model="shareForm.maxAccessCount" :min="0" :max="999999" />
          <span style="margin-left: 8px; color: #999">0 表示不限制</span>
        </el-form-item>
        <el-form-item label="允许下载">
          <el-switch v-model="shareForm.allowDownload" />
        </el-form-item>
      </el-form>

      <div v-if="shareResult" class="share-result">
        <div class="share-result-title">短链生成成功</div>
        <el-input :model-value="shareResult.shortLink" readonly>
          <template #append>
            <el-button @click="copyShareLink">复制</el-button>
          </template>
        </el-input>
      </div>

      <template #footer>
        <el-button @click="shareDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="shareSubmitting" @click="createShortShareLink">生成短链</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.file-manager {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.breadcrumb-section {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.current-path {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 6px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.file-list-container {
  margin-top: 20px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.file-icon {
  font-size: 18px;
  color: #409eff;
}

.folder-icon {
  color: #e6a23c;
}

.file-name {
  flex: 1;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.breadcrumb-item {
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: #409eff;
}

.share-result {
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: #f5f9ff;
}

.share-result-title {
  margin-bottom: 8px;
  color: #409eff;
  font-weight: 600;
}
</style>