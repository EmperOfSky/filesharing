<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useFileStore } from '@/stores/file'
import { ElMessage } from 'element-plus'

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
  { label: '所有类型', value: '' },
  { label: '文档', value: 'doc,pdf,txt' },
  { label: '图片', value: 'jpg,jpeg,png,gif' },
  { label: '视频', value: 'mp4,avi,mov' },
  { label: '音频', value: 'mp3,wav,flac' },
  { label: '压缩包', value: 'zip,rar,7z' }
]

const handleSearch = async () => {
  if (!searchForm.keyword.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  loading.value = true
  searchPerformed.value = true
  
  try {
    const results = await fileStore.searchFiles(
      searchForm.keyword,
      searchForm.fileType
    )
    
    searchResults.value = [
      ...results.folders.map(folder => ({
        ...folder,
        isFolder: true,
        type: 'folder'
      })),
      ...results.files.map(file => ({
        ...file,
        isFolder: false,
        type: 'file'
      }))
    ]
    
    ElMessage.success(`找到 ${searchResults.value.length} 个结果`)
  } catch (error: any) {
    ElMessage.error(error.message || '搜索失败')
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
</script>

<template>
  <div class="search-page">
    <div class="search-container">
      <div class="search-header">
        <h2>文件搜索</h2>
        <p>快速查找您需要的文件和文件夹</p>
      </div>
      
      <el-card class="search-card">
        <el-form :model="searchForm" label-position="top">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="搜索关键词">
                <el-input
                  v-model="searchForm.keyword"
                  placeholder="输入文件名、文件夹名或内容关键词"
                  size="large"
                  @keyup.enter="handleSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="文件类型">
                <el-select
                  v-model="searchForm.fileType"
                  placeholder="选择文件类型"
                  size="large"
                  style="width: 100%"
                >
                  <el-option
                    v-for="type in fileTypes"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="最小大小">
                <el-input
                  v-model="searchForm.minSize"
                  placeholder="例如: 1MB"
                  size="large"
                >
                  <template #append>Bytes</template>
                </el-input>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="最大大小">
                <el-input
                  v-model="searchForm.maxSize"
                  placeholder="例如: 100MB"
                  size="large"
                >
                  <template #append>Bytes</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          
          <div class="search-actions">
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleSearch"
            >
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            
            <el-button
              size="large"
              @click="handleReset"
            >
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </div>
        </el-form>
      </el-card>
      
      <!-- 搜索结果 -->
      <div v-if="searchPerformed" class="search-results">
        <div class="results-header">
          <h3>搜索结果 ({{ searchResults.length }} 项)</h3>
        </div>
        
        <el-card v-if="loading" class="loading-card">
          <div class="loading-content">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="p" style="width: 30%" />
                <div style="margin-top: 20px">
                  <el-skeleton-item variant="p" style="width: 100%; height: 60px" />
                  <el-skeleton-item variant="p" style="width: 100%; height: 60px; margin-top: 10px" />
                  <el-skeleton-item variant="p" style="width: 100%; height: 60px; margin-top: 10px" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </el-card>
        
        <div v-else-if="searchResults.length > 0" class="results-list">
          <el-table :data="searchResults" style="width: 100%">
            <el-table-column prop="name" label="名称" min-width="300">
              <template #default="{ row }">
                <div class="result-item">
                  <el-icon class="result-icon" :class="{ 'folder-icon': row.isFolder }">
                    <component :is="row.isFolder ? 'Folder' : getFileIcon(row.extension)" />
                  </el-icon>
                  <div class="result-info">
                    <div class="result-name">{{ row.name || row.originalName }}</div>
                    <div class="result-path" v-if="!row.isFolder">
                      文件大小: {{ formatFileSize(row.fileSize) }}
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button 
                  v-if="!row.isFolder"
                  link 
                  type="primary" 
                  size="small"
                >
                  下载
                </el-button>
                <el-button 
                  link 
                  type="primary" 
                  size="small"
                >
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <el-empty 
          v-else-if="!loading" 
          description="没有找到相关文件"
          class="no-results"
        >
          <el-button type="primary" @click="handleReset">重新搜索</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 120px);
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
}

.search-header {
  text-align: center;
  margin-bottom: 30px;
}

.search-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.search-header p {
  color: #666;
  font-size: 16px;
}

.search-card {
  margin-bottom: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.search-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 20px;
}

.search-results {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.results-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.results-header h3 {
  color: #333;
  margin: 0;
}

.loading-card {
  border: none;
  box-shadow: none;
}

.loading-content {
  padding: 20px;
}

.results-list {
  margin-top: 20px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.result-icon {
  font-size: 20px;
  color: #409eff;
}

.folder-icon {
  color: #e6a23c;
}

.result-info {
  flex: 1;
}

.result-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.result-path {
  font-size: 12px;
  color: #666;
}

.no-results {
  padding: 60px 0;
  text-align: center;
}
</style>