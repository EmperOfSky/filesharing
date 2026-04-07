<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import backupService from '@/services/backup'
import type { BackupConfig, BackupInfo, BackupStatistics, BackupTask, BackupType } from '@/types'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const pageLoading = ref(false)
const creating = ref(false)
const cleanupLoading = ref(false)
const configLoading = ref(false)
const configDialogVisible = ref(false)

const backups = ref<BackupInfo[]>([])
const currentTask = ref<BackupTask | null>(null)
const taskPollingTimer = ref<number | null>(null)
const statistics = ref<BackupStatistics>({
  totalBackups: 0,
  totalBackupSize: 0,
  fullBackupCount: 0,
  incrementalBackupCount: 0
})

const createForm = reactive({
  backupName: '',
  backupType: 'FULL' as BackupType,
  includeFiles: true,
  sinceTime: ''
})

const configForm = reactive<BackupConfig>({
  backupBasePath: '',
  maxBackupSize: '',
  compressionLevel: 6,
  retentionDays: 30,
  autoBackupEnabled: false
})

const validCount = computed(() => backups.value.filter((item) => item.valid).length)
const backupReadiness = computed(() => (configForm.backupBasePath ? '可执行' : '待配置'))
const recoveryStatus = computed(() => (validCount.value > 0 ? '可恢复' : '暂无可恢复'))
const taskStatusText = computed(() => {
  const status = String(currentTask.value?.status || '').toUpperCase()
  if (status === 'RUNNING' || status === 'PENDING') return '备份中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '失败'
  return currentTask.value?.status || '--'
})
const taskStatusTagType = computed(() => {
  const status = String(currentTask.value?.status || '').toUpperCase()
  if (status === 'RUNNING' || status === 'PENDING') return 'warning'
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
})

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

const loadBackups = async () => {
  try {
    backups.value = await backupService.listBackups()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载备份列表失败'
    ElMessage.error(message)
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await backupService.getBackupStatistics()
  } catch {
    // ignore
  }
}

const loadExpiringItems = async () => {
  try {
    // 逻辑保留，expiringItems 变量根据原代码未在 template 中显式使用
  } catch {
    // ignore
  }
}

const refreshAll = async () => {
  pageLoading.value = true
  try {
    await Promise.all([loadBackups(), loadStatistics(), loadExpiringItems()])
  } finally {
    pageLoading.value = false
  }
}

const createBackup = async () => {
  if (!createForm.backupName.trim()) {
    ElMessage.warning('请输入备份名称')
    return
  }
  if (createForm.backupType === 'INCREMENTAL' && !createForm.sinceTime) {
    ElMessage.warning('增量备份需要选择起始时间')
    return
  }

  creating.value = true
  try {
    const result = await backupService.createBackupAsync({
      backupName: createForm.backupName.trim(),
      backupType: createForm.backupType,
      includeFiles: createForm.includeFiles,
      sinceTime: createForm.backupType === 'INCREMENTAL' ? createForm.sinceTime : undefined
    })
    currentTask.value = await backupService.getBackupTaskStatus(result.taskId)
    ElMessage.success(result.message || '备份任务已创建')
    refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '创建备份失败'
    ElMessage.error(message)
  } finally {
    creating.value = false
  }
}

const refreshTask = async () => {
  await refreshTaskInternal(true)
}

const refreshTaskInternal = async (showError: boolean) => {
  if (!currentTask.value?.taskId) return
  try {
    currentTask.value = await backupService.getBackupTaskStatus(currentTask.value.taskId)
  } catch (error: any) {
    if (showError) {
      const message = error?.response?.data?.message || error?.message || '刷新任务状态失败'
      ElMessage.error(message)
    }
  }
}

const stopTaskPolling = () => {
  if (taskPollingTimer.value !== null) {
    window.clearInterval(taskPollingTimer.value)
    taskPollingTimer.value = null
  }
}

const startTaskPolling = () => {
  if (!currentTask.value?.taskId) return
  stopTaskPolling()
  taskPollingTimer.value = window.setInterval(() => {
    void refreshTaskInternal(false)
  }, 2000)
}

watch(
  () => currentTask.value?.status,
  (status, prevStatus) => {
    const curr = String(status || '').toUpperCase()
    const prev = String(prevStatus || '').toUpperCase()
    const running = curr === 'RUNNING' || curr === 'PENDING'

    if (running) {
      startTaskPolling()
      return
    }

    stopTaskPolling()
    if ((prev === 'RUNNING' || prev === 'PENDING') && curr === 'FAILED' && currentTask.value?.errorMessage) {
      ElMessage.error(currentTask.value.errorMessage)
    }

    if ((prev === 'RUNNING' || prev === 'PENDING') && curr) {
      void refreshAll()
    }
  }
)

const validateBackup = async (item: BackupInfo) => {
  try {
    const result = await backupService.validateBackup(item.backupPath)
    ElMessage.success(result.message || (result.isValid ? '备份校验通过' : '备份校验失败'))
    refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '校验备份失败'
    ElMessage.error(message)
  }
}

const restoreBackup = async (item: BackupInfo) => {
  try {
    await ElMessageBox.confirm(`确认从备份 ${item.backupName} 恢复吗？`, '恢复备份', { type: 'warning' })
    const result = await backupService.restoreFromBackup(item.backupPath, item.includeFiles)
    currentTask.value = await backupService.getBackupTaskStatus(result.taskId)
    ElMessage.success(result.message || '恢复任务已提交')
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '恢复备份失败'
    ElMessage.error(message)
  }
}

const deleteBackup = async (item: BackupInfo) => {
  try {
    await ElMessageBox.confirm(`确认删除备份 ${item.backupName} 吗？`, '删除备份', { type: 'warning' })
    await backupService.deleteBackup(item.backupPath)
    ElMessage.success('备份已删除')
    refreshAll()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '删除备份失败'
    ElMessage.error(message)
  }
}

const cleanupExpired = async () => {
  cleanupLoading.value = true
  try {
    const result = await backupService.cleanupExpiredBackups(Number(configForm.retentionDays || 30))
    ElMessage.success(result.message || `已清理 ${result.deletedCount} 个过期备份`)
    refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '清理过期备份失败'
    ElMessage.error(message)
  } finally {
    cleanupLoading.value = false
  }
}

const openConfigDialog = async () => {
  configLoading.value = true
  try {
    const data = await backupService.exportBackupConfig()
    Object.assign(configForm, data)
    configDialogVisible.value = true
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载备份配置失败'
    ElMessage.error(message)
  } finally {
    configLoading.value = false
  }
}

const saveConfig = async () => {
  configLoading.value = true
  try {
    await backupService.importBackupConfig({ ...configForm })
    ElMessage.success('备份配置已更新')
    configDialogVisible.value = false
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '保存备份配置失败'
    ElMessage.error(message)
  } finally {
    configLoading.value = false
  }
}

onMounted(() => {
  refreshAll()
})

onUnmounted(() => {
  stopTaskPolling()
})
</script>

<template>
  <div class="modern-backup-page" v-loading="pageLoading">
    <!-- Header Hero -->
    <header class="base-card welcome-hero">
      <div class="hero-content">
        <div class="hero-text">
          <span class="kicker-tag">Backup Maintenance</span>
          <h1 class="title">备份管理控制台</h1>
          <p class="description">管理系统的全量/增量备份策略，保障核心数据安全，实现快速灾备恢复。</p>
          <div class="hero-actions">
            <el-button color="#6366f1" type="primary" size="large" :loading="creating" @click="createBackup" round>
              <el-icon><Plus /></el-icon> 执行备份
            </el-button>
            <el-button size="large" :loading="cleanupLoading" @click="cleanupExpired" round plain>清理过期</el-button>
            <el-button size="large" :loading="configLoading" @click="openConfigDialog" round plain>策略配置</el-button>
          </div>
        </div>
        <div class="hero-stats">
          <UiStatCard label="运行就绪" :value="backupReadiness" />
          <UiStatCard label="总占用" :value="formatSize(statistics.totalBackupSize)" />
          <UiStatCard label="有效备份" :value="validCount" />
          <UiStatCard label="恢复能力" :value="recoveryStatus" />
        </div>
      </div>
    </header>

    <!-- 主布局 -->
    <div class="backup-layout">
      <!-- Sidebar 1: Creation -->
      <aside class="sidebar-left">
        <div class="base-card settings-card">
          <h2 class="section-title">创建任务</h2>
          <el-form label-position="top" class="modern-form">
            <el-form-item label="备份名称">
              <el-input v-model="createForm.backupName" placeholder="例如：backup-2026-03" />
            </el-form-item>
            <el-form-item label="备份类型">
              <el-segmented v-model="createForm.backupType" :options="[{label: '全量', value: 'FULL'}, {label: '增量', value: 'INCREMENTAL'}]" />
            </el-form-item>
            <el-form-item v-if="createForm.backupType === 'INCREMENTAL'" label="起始时间">
              <el-date-picker v-model="createForm.sinceTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="full-width" />
            </el-form-item>
            <el-form-item label="包含文件">
              <el-switch v-model="createForm.includeFiles" />
            </el-form-item>
          </el-form>
        </div>
      </aside>

      <!-- Center: Status & List -->
      <main class="main-content">
        <section class="base-card status-card">
          <div class="header-row">
            <h2 class="section-title">当前任务</h2>
            <el-button v-if="currentTask" size="small" @click="refreshTask" round>同步状态</el-button>
          </div>
          <div v-if="currentTask" class="status-grid">
            <div class="stat-mini"><span>ID</span><strong>{{ currentTask.taskId }}</strong></div>
            <div class="stat-mini"><span>状态</span><el-tag round size="small" :type="taskStatusTagType">{{ taskStatusText }}</el-tag></div>
            <div class="stat-mini"><span>类型</span><strong>{{ currentTask.backupType }}</strong></div>
            <div class="stat-mini"><span>开始</span><strong>{{ formatDateTime(currentTask.startTime) }}</strong></div>
          </div>
          <p v-if="currentTask && (currentTask.status === 'RUNNING' || currentTask.status === 'PENDING')" class="status-hint">
            备份中，请稍候，状态会自动刷新。
          </p>
          <el-empty v-else description="无活跃任务" :image-size="60" />
        </section>

        <section class="base-card asset-card">
          <h2 class="section-title">备份资产</h2>
          <el-table :data="backups" class="modern-table">
            <el-table-column label="备份名称" min-width="200">
              <template #default="{ row }">
                <div class="file-name">{{ row.backupName }}</div>
                <div class="file-path">{{ row.backupPath }}</div>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100">
              <template #default="{ row }">{{ formatSize(row.totalFileSize) }}</template>
            </el-table-column>
            <el-table-column label="类型/时间" min-width="180">
              <template #default="{ row }">
                <div class="text-sm">{{ row.backupType }}</div>
                <div class="text-xs text-muted">{{ formatDateTime(row.createTime) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="validateBackup(row)">校验</el-button>
                <el-button link type="warning" @click="restoreBackup(row)">恢复</el-button>
                <el-button link type="danger" @click="deleteBackup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </main>

      <!-- Sidebar 2: Policies -->
      <aside class="sidebar-right">
        <div class="base-card policy-card">
          <h2 class="section-title">策略详情</h2>
          <div class="policy-list">
            <div class="policy-item"><span>保留天数</span><strong>{{ configForm.retentionDays }} 天</strong></div>
            <div class="policy-item"><span>压缩等级</span><strong>{{ configForm.compressionLevel }}</strong></div>
            <div class="policy-item"><span>自动备份</span><el-tag :type="configForm.autoBackupEnabled ? 'success' : 'info'" round size="small">{{ configForm.autoBackupEnabled ? '开启' : '关闭' }}</el-tag></div>
          </div>
        </div>
      </aside>
    </div>

    <!-- Config Dialog -->
    <el-dialog v-model="configDialogVisible" title="⚙️ 备份策略配置" width="500px" class="modern-dialog">
      <el-form label-position="top">
        <el-form-item label="备份存放路径"><el-input v-model="configForm.backupBasePath" /></el-form-item>
        <el-form-item label="压缩等级 (0-9)"><el-input-number v-model="configForm.compressionLevel" :min="0" :max="9" /></el-form-item>
        <el-form-item label="保留天数"><el-input-number v-model="configForm.retentionDays" :min="1" /></el-form-item>
        <el-form-item label="自动备份"><el-switch v-model="configForm.autoBackupEnabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false" round>取消</el-button>
        <el-button type="primary" color="#6366f1" @click="saveConfig" round>保存策略</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.modern-backup-page {
  --primary-color: #6366f1;
  --bg-page: #f8fafc;
  background: var(--bg-page);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 100vh;
}

.base-card { background: #fff; border-radius: 20px; border: 1px solid #e2e8f0; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }

/* Layout */
.backup-layout { display: grid; grid-template-columns: 300px 1fr 280px; gap: 24px; align-items: start; }
.section-title { font-size: 16px; font-weight: 700; margin: 0 0 16px 0; }

/* Hero */
.welcome-hero { padding: 40px; }
.hero-content { display: flex; justify-content: space-between; align-items: center; gap: 40px; }
.hero-text { flex: 1; }
.kicker-tag { font-size: 12px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 800; margin: 12px 0; }
.description { color: #64748b; font-size: 14px; margin-bottom: 24px; }
.hero-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; min-width: 320px; }

/* Table & Status */
.status-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.stat-mini { background: #f8fafc; padding: 12px; border-radius: 8px; font-size: 13px; }
.stat-mini span { color: #64748b; margin-right: 8px; }
.status-hint { margin: 12px 2px 0; color: #a16207; font-size: 13px; }

.modern-table { --el-table-header-bg-color: #f8fafc; }
.file-name { font-weight: 600; }
.file-path { font-size: 12px; color: #94a3b8; }

.policy-list { display: flex; flex-direction: column; gap: 12px; }
.policy-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
.policy-item strong { font-weight: 600; }

@media (max-width: 1100px) {
  .backup-layout { grid-template-columns: 1fr; }
}
</style>