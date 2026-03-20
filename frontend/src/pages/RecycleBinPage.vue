<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import fileService from '@/services/file'
import type { ExpiringRecycleItem, RecycleBinItem, RecycleBinStats } from '@/types'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const loading = ref(false)
const actionLoading = ref(false)
const selectedRows = ref<RecycleBinItem[]>([])

const list = ref<RecycleBinItem[]>([])
const stats = ref<RecycleBinStats>({
  totalItems: 0,
  fileCount: 0,
  folderCount: 0,
  expiredCount: 0,
  recoverableCount: 0
})
const expiringItems = ref<ExpiringRecycleItem[]>([])

const filters = reactive({
  keyword: '',
  itemType: 'ALL'
})

const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

const formatDateTime = (value?: string) => {
  if (!value) return '--'
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

const loadList = async () => {
  loading.value = true
  try {
    const result = filters.keyword.trim()
      ? await fileService.searchRecycleBin(filters.keyword.trim(), pagination.page, pagination.size)
      : await fileService.getRecycleBin(pagination.page, pagination.size, filters.itemType)
    list.value = result.content || []
    pagination.total = Number(result.totalElements || 0)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载回收站失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    stats.value = await fileService.getRecycleBinStats()
  } catch {
    // stats 用于辅助展示，失败时不阻断页面
  }
}

const loadExpiringItems = async () => {
  try {
    expiringItems.value = await fileService.getRecycleBinExpiring(48)
  } catch {
    expiringItems.value = []
  }
}

const refreshAll = async () => {
  await Promise.all([loadList(), loadStats(), loadExpiringItems()])
}

const resetFilters = () => {
  filters.keyword = ''
  filters.itemType = 'ALL'
  pagination.page = 0
  loadList()
}

const restoreOne = async (item: RecycleBinItem) => {
  actionLoading.value = true
  try {
    const result = await fileService.restoreRecycleBinItem(item.id)
    ElMessage.success(result.message || '已恢复项目')
    refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '恢复失败'
    ElMessage.error(message)
  } finally {
    actionLoading.value = false
  }
}

const deleteOne = async (item: RecycleBinItem) => {
  try {
    await ElMessageBox.confirm(`确认永久删除 ${item.originalName} 吗？该操作不可撤销。`, '永久删除', { type: 'warning' })
    actionLoading.value = true
    await fileService.permanentlyDeleteRecycleBinItem(item.id)
    ElMessage.success('项目已永久删除')
    refreshAll()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '永久删除失败'
    ElMessage.error(message)
  } finally {
    actionLoading.value = false
  }
}

const batchRestore = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要恢复的项目')
    return
  }
  actionLoading.value = true
  try {
    const result = await fileService.batchRestoreRecycleBinItems(selectedIds.value)
    ElMessage.success(`已恢复 ${result.successCount} 项`)
    refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '批量恢复失败'
    ElMessage.error(message)
  } finally {
    actionLoading.value = false
  }
}

const batchDelete = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要永久删除的项目')
    return
  }
  try {
    await ElMessageBox.confirm(`确认永久删除选中的 ${selectedIds.value.length} 项吗？`, '批量删除', { type: 'warning' })
    actionLoading.value = true
    const result = await fileService.batchDeleteRecycleBinItems(selectedIds.value)
    ElMessage.success(`已删除 ${result.successCount} 项`)
    refreshAll()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '批量删除失败'
    ElMessage.error(message)
  } finally {
    actionLoading.value = false
  }
}

const emptyRecycle = async () => {
  try {
    await ElMessageBox.confirm('确认清空整个回收站吗？该操作不可撤销。', '清空回收站', { type: 'warning' })
    actionLoading.value = true
    await fileService.emptyRecycleBin()
    ElMessage.success('回收站已清空')
    refreshAll()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '清空回收站失败'
    ElMessage.error(message)
  } finally {
    actionLoading.value = false
  }
}

const onSelectionChange = (rows: RecycleBinItem[]) => {
  selectedRows.value = rows
}

const onPageChange = (page: number) => {
  pagination.page = page - 1
  loadList()
}

const onSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 0
  loadList()
}

onMounted(() => {
  refreshAll()
})
</script>

<template>
  <div class="modern-workspace">
    <header class="base-card workspace-header">
      <div class="header-content">
        <div class="header-intro">
          <div class="kicker-tag">Recycle Bin</div>
          <h1 class="title">回收站中心</h1>
          <p class="description">存放已删除的文件与目录，支持批量恢复、永久删除与一键清空。</p>
          <div class="header-actions">
            <el-button type="primary" color="#0ea5e9" size="large" :loading="actionLoading" @click="batchRestore" round>批量恢复</el-button>
            <el-button size="large" :loading="actionLoading" @click="batchDelete" round plain>批量删除</el-button>
            <el-button size="large" type="danger" :loading="actionLoading" @click="emptyRecycle" round plain>清空回收站</el-button>
          </div>
        </div>

        <div class="header-stats">
          <UiStatCard label="总项目" :value="stats.totalItems" />
          <UiStatCard label="可恢复" :value="stats.recoverableCount" />
          <UiStatCard label="已过期" :value="stats.expiredCount" />
          <UiStatCard label="已选中" :value="selectedRows.length" />
        </div>
      </div>
    </header>

    <main class="base-card workspace-main">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="filters.keyword" placeholder="搜索文件名..." class="search-input" clearable @keyup.enter="loadList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filters.itemType" style="width: 140px">
            <el-option label="所有类型" value="ALL" />
            <el-option label="文件" value="FILE" />
            <el-option label="文件夹" value="FOLDER" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="pagination.page = 0; loadList()" round>查询</el-button>
          <el-button @click="resetFilters" round>重置</el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="list" v-loading="loading" class="modern-table" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column label="名称" min-width="300">
            <template #default="{ row }">
              <div class="file-item">
                <div class="file-icon" :class="{ 'is-folder': row.itemType === 'FOLDER' }">
                  <el-icon><component :is="row.itemType === 'FOLDER' ? 'Folder' : 'Document'" /></el-icon>
                </div>
                <div class="file-info">
                  <span class="file-name">{{ row.originalName }}</span>
                  <span class="file-meta">{{ row.originalPath || '/' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="120">
            <template #default="{ row }">
              <span class="text-secondary">{{ formatSize(row.fileSize) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="删除人" width="140" prop="deletedByName" />
          <el-table-column label="删除时间" width="200">
            <template #default="{ row }">
              <span class="text-secondary">{{ formatDateTime(row.deletedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.isRecoverable ? 'success' : 'warning'" effect="light" round>
                {{ row.isRecoverable ? '可恢复' : '已过期' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="right" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-tooltip content="恢复" placement="top">
                  <el-button circle size="small" type="primary" plain :disabled="!row.isRecoverable" @click="restoreOne(row)">
                    <el-icon><RefreshRight /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="永久删除" placement="top">
                  <el-button circle size="small" type="danger" plain @click="deleteOne(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, sizes, prev, pager, next" :current-page="pagination.page + 1" :page-size="pagination.size" :total="pagination.total" @current-change="onPageChange" @size-change="onSizeChange" />
      </div>
    </main>
  </div>
</template>

<style scoped>
.modern-workspace {
  --primary-color: #0ea5e9;
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

.base-card {
  background: var(--bg-card);
  border-radius: 20px;
  box-shadow: 0 4px 24px -4px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--border-color);
  padding: 32px;
}

.header-content { display: flex; justify-content: space-between; align-items: center; gap: 40px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0f2fe; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 700; margin: 12px 0; }
.header-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; min-width: 350px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.search-input { width: 300px; }

.modern-table { --el-table-header-bg-color: #f8fafc; }
:deep(.el-table td.el-table__cell) { padding: 12px 0; }

.file-item { display: flex; align-items: center; gap: 16px; }
.file-icon { width: 40px; height: 40px; border-radius: 10px; background: #e0f2fe; display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--primary-color); }
.file-icon.is-folder { background: #fef3c7; color: #f59e0b; }
.file-info { display: flex; flex-direction: column; gap: 4px; }
.file-name { font-weight: 600; color: #0f172a; }
.file-meta { font-size: 12px; color: #64748b; }

.row-actions { opacity: 0; transition: opacity 0.2s; display: flex; gap: 8px; justify-content: flex-end; }
:deep(.el-table__row:hover) .row-actions { opacity: 1; }

.pagination-wrapper { margin-top: 24px; display: flex; justify-content: flex-end; }
</style>