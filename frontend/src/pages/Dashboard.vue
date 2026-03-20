<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useFileStore } from '@/stores/file'
import { ElMessage } from 'element-plus'

const fileStore = useFileStore()
const loading = ref(false)

const stats = ref({
  totalFiles: 0,
  totalFolders: 0,
  totalSize: 0,
  todayUploads: 0
})

const recentFiles = ref<any[]>([])

const loadDashboardData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fileStore.loadFiles(0, 5),
      fileStore.loadFolders()
    ])
    
    // 计算统计数据
    stats.value.totalFiles = fileStore.files.length
    stats.value.totalFolders = fileStore.folders.length
    stats.value.totalSize = fileStore.files.reduce((sum, file) => sum + file.fileSize, 0)
    
    // 获取最近文件
    recentFiles.value = [...fileStore.files]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  loadDashboardData()
})
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-blue">
              <el-icon size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalFiles }}</div>
              <div class="stat-label">文件总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-green">
              <el-icon size="24"><Folder /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalFolders }}</div>
              <div class="stat-label">文件夹数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-orange">
              <el-icon size="24"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ formatFileSize(stats.totalSize) }}</div>
              <div class="stat-label">总存储空间</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-purple">
              <el-icon size="24"><Upload /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.todayUploads }}</div>
              <div class="stat-label">今日上传</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card class="recent-files-card">
          <template #header>
            <div class="card-header">
              <span>最近文件</span>
              <el-button link @click="$router.push('/dashboard/files')">查看更多</el-button>
            </div>
          </template>
          
          <el-table 
            :data="recentFiles" 
            :loading="loading"
            style="width: 100%"
          >
            <el-table-column prop="originalName" label="文件名">
              <template #default="{ row }">
                <div class="file-name-cell">
                  <el-icon class="file-icon">
                    <component :is="getFileIcon(row.extension)" />
                  </el-icon>
                  <span>{{ row.originalName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="180">
              <template #default="{ row }">
                {{ new Date(row.createdAt).toLocaleDateString() }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default>
                <el-button link type="primary" size="small">下载</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="quick-actions-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          
          <div class="quick-actions">
            <el-button 
              type="primary" 
              size="large" 
              @click="$router.push('/dashboard/files')"
              class="action-button"
            >
              <el-icon><Upload /></el-icon>
              上传文件
            </el-button>
            
            <el-button 
              type="success" 
              size="large"
              @click="$router.push('/dashboard/files')"
              class="action-button"
            >
              <el-icon><FolderAdd /></el-icon>
              新建文件夹
            </el-button>
            
            <el-button 
              type="info" 
              size="large"
              @click="$router.push('/dashboard/search')"
              class="action-button"
            >
              <el-icon><Search /></el-icon>
              搜索文件
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts">
// 这部分放在script setup外部，因为需要在模板中使用
export default {
  methods: {
    getFileIcon(extension: string) {
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
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.bg-blue { background-color: #409eff; }
.bg-green { background-color: #67c23a; }
.bg-orange { background-color: #e6a23c; }
.bg-purple { background-color: #722ed1; }

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.content-row {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recent-files-card,
.quick-actions-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #409eff;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
}
</style>