<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, CopyDocument, Position, Lock, Unlock, Delete, DataAnalysis } from '@element-plus/icons-vue'
import fileService from '@/services/fileService'
import UiStatCard from '@/components/ui/UiStatCard.vue'

interface ShareRow {
  id: number
  title?: string
  description?: string
  shareKey: string
  shareType: 'FILE' | 'FOLDER'
  shortLink: string
  status: 'ACTIVE' | 'EXPIRED' | 'DISABLED'
  requiresPassword: boolean
  allowDownload: boolean
  maxAccessCount: number
  currentAccessCount: number
  pvCount?: number
  uvCount?: number
  lastVisitorIp?: string
  lastVisitorAddress?: string
  lastAccessAt?: string
  expireTime?: string
  createdAt: string
  sharedContent?: {
    name?: string
  }
}

interface MonitoringDimensionItem {
  name: string
  count: number
}

interface MonitoringDailyItem {
  date: string
  count: number
}

interface MonitoringVisitItem {
  ip?: string
  address?: string
  referer?: string
  browser?: string
  os?: string
  device?: string
  userAgent?: string
  accessedAt?: string
}

interface ShareMonitoringDetails {
  pv: number
  uv: number
  uip: number
  accessCount: number
  lastAccessAt?: string
  lastVisitorIp?: string
  lastVisitorAddress?: string
  dailyTrend: MonitoringDailyItem[]
  browserStats: MonitoringDimensionItem[]
  osStats: MonitoringDimensionItem[]
  refererStats: MonitoringDimensionItem[]
  recentVisits: MonitoringVisitItem[]
}

const router = useRouter()

const loading = ref(false)
const operatingId = ref<number | null>(null)
const shares = ref<ShareRow[]>([])
const monitoringDialogVisible = ref(false)
const monitoringLoading = ref(false)
const monitoringTarget = ref<ShareRow | null>(null)
const monitoringDetails = ref<ShareMonitoringDetails | null>(null)

const stats = computed(() => ({
  total: shares.value.length,
  active: shares.value.filter((item) => item.status === 'ACTIVE').length,
  disabled: shares.value.filter((item) => item.status === 'DISABLED').length,
  expired: shares.value.filter((item) => item.status === 'EXPIRED').length,
  visits: shares.value.reduce((sum, item) => sum + Number((item.pvCount ?? item.currentAccessCount) || 0), 0)
}))

const normalizeLink = (link: string) => {
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

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const shareTitle = (row: ShareRow) => row.title || row.sharedContent?.name || '未命名分享'
const shareSubtitle = (row: ShareRow) => row.sharedContent?.name || row.description || row.shareKey
const displayText = (value?: string) => (value && value.trim() ? value : '--')

const normalizeMonitoringDetails = (raw: any): ShareMonitoringDetails => ({
  pv: Number(raw?.pv || 0),
  uv: Number(raw?.uv || 0),
  uip: Number(raw?.uip || 0),
  accessCount: Number(raw?.accessCount || 0),
  lastAccessAt: raw?.lastAccessAt,
  lastVisitorIp: raw?.lastVisitorIp,
  lastVisitorAddress: raw?.lastVisitorAddress,
  dailyTrend: Array.isArray(raw?.dailyTrend) ? raw.dailyTrend : [],
  browserStats: Array.isArray(raw?.browserStats) ? raw.browserStats : [],
  osStats: Array.isArray(raw?.osStats) ? raw.osStats : [],
  refererStats: Array.isArray(raw?.refererStats) ? raw.refererStats : [],
  recentVisits: Array.isArray(raw?.recentVisits) ? raw.recentVisits : []
})

const loadShares = async () => {
  loading.value = true
  try {
    const data = await fileService.getMyShares(0, 100)
    shares.value = Array.isArray(data)
      ? data.map((item: ShareRow) => ({ ...item, shortLink: normalizeLink(item.shortLink) }))
      : []
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载分享列表失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const copyLink = async (row: ShareRow) => {
  if (!row.shortLink) {
    ElMessage.warning('当前分享没有可复制的链接')
    return
  }
  try {
    await navigator.clipboard.writeText(row.shortLink)
    ElMessage.success('分享链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const openLink = (row: ShareRow) => {
  if (!row.shortLink) return
  window.open(row.shortLink, '_blank')
}

const loadMonitoring = async (shareId: number) => {
  monitoringLoading.value = true
  try {
    const data = await fileService.getShareMonitoring(shareId, 30)
    monitoringDetails.value = normalizeMonitoringDetails(data)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载监控详情失败'
    ElMessage.error(message)
  } finally {
    monitoringLoading.value = false
  }
}

const openMonitoring = async (row: ShareRow) => {
  monitoringTarget.value = row
  monitoringDialogVisible.value = true
  await loadMonitoring(row.id)
}

const refreshMonitoring = async () => {
  if (!monitoringTarget.value) return
  await loadMonitoring(monitoringTarget.value.id)
}

const toggleShareStatus = async (row: ShareRow) => {
  operatingId.value = row.id
  try {
    if (row.status === 'ACTIVE') {
      await fileService.disableShare(row.id)
      ElMessage.success('分享已停用')
    } else {
      await fileService.enableShare(row.id)
      ElMessage.success('分享已启用')
    }
    await loadShares()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    operatingId.value = null
  }
}

const removeShare = async (row: ShareRow) => {
  try {
    await ElMessageBox.confirm('确认删除这条分享记录吗？删除后短链会立即失效。', '删除分享', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    operatingId.value = row.id
    await fileService.deleteShare(row.id)
    ElMessage.success('分享已删除')
    await loadShares()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      const message = error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(message)
    }
  } finally {
    operatingId.value = null
  }
}

onMounted(() => {
  loadShares()
})
</script>

<template>
  <div class="modern-workspace modern-share-manage">
    <!-- 顶部卡片：介绍与核心数据 -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Share Control</span>
          <h1 class="title">分享管理</h1>
          <p class="subtitle">在这里集中管理您的所有分享链接。掌控访问权限、过期时间并监控被访问情况。</p>
        </div>
        <div class="header-actions">
          <el-button color="#6366f1" type="primary" size="large" :loading="loading" @click="loadShares" round>
            <el-icon class="el-icon--left"><Refresh /></el-icon>刷新数据
          </el-button>
          <el-button size="large" plain @click="router.push('/dashboard/files')" round>返回文件库</el-button>
        </div>
      </div>

      <div class="header-stats-strip">
        <UiStatCard label="历史分享总计" :value="stats.total" />
        <el-divider direction="vertical" />
        <UiStatCard label="活跃有效" :value="stats.active" />
        <el-divider direction="vertical" />
        <UiStatCard label="累计被访问数" :value="stats.visits" />
        <el-divider direction="vertical" />
        <UiStatCard label="已失效/过期" :value="stats.expired" />
      </div>
    </header>

    <!-- 工作区主体：表格 -->
    <main class="workspace-main base-card">
      <div class="table-toolbar">
        <el-breadcrumb separator="/" class="modern-breadcrumb">
          <el-breadcrumb-item>
            <span class="breadcrumb-active">全部列表</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="table-container">
        <el-table :data="shares" v-loading="loading" class="modern-table" style="width: 100%" stripe>
          <template #empty>
            <div class="empty-state">
              <el-empty description="当前还没有创建过任何分享" :image-size="100">
                <el-button type="primary" color="#6366f1" plain @click="router.push('/dashboard/files')" round>去文件管理创建</el-button>
              </el-empty>
            </div>
          </template>

          <el-table-column label="资源名称" min-width="280">
            <template #default="{ row }">
              <div class="file-item">
                <div class="file-icon" :class="{ 'is-folder': row.shareType === 'FOLDER' }">
                  <el-icon><component :is="row.shareType === 'FOLDER' ? 'Folder' : 'Document'" /></el-icon>
                </div>
                <div class="file-info">
                  <span class="file-name">{{ shareTitle(row) }}</span>
                  <span class="file-meta">{{ shareSubtitle(row) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="分享链接" min-width="220">
            <template #default="{ row }">
              <el-link :href="row.shortLink" target="_blank" type="primary" class="mono-link" :underline="false">
                {{ row.shortLink }}
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="当前状态" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'ACTIVE'" type="success" effect="light" round disable-transitions>启用中</el-tag>
              <el-tag v-else-if="row.status === 'DISABLED'" type="warning" effect="light" round disable-transitions>已停用</el-tag>
              <el-tag v-else type="info" effect="plain" round disable-transitions>已过期</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="访问热度" width="120">
            <template #default="{ row }">
              <span class="text-secondary">{{ row.currentAccessCount || 0 }} <span class="dim">/ {{ row.maxAccessCount > 0 ? row.maxAccessCount : '不限' }}</span></span>
            </template>
          </el-table-column>

          <el-table-column label="有效期至" min-width="170">
            <template #default="{ row }">
              <span class="text-secondary">{{ row.expireTime ? formatDateTime(row.expireTime) : '永久有效' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="300" fixed="right" align="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-tooltip content="复制链接" placement="top">
                  <el-button circle size="small" type="primary" plain @click="copyLink(row)">
                    <el-icon><CopyDocument /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="打开" placement="top">
                  <el-button circle size="small" type="primary" plain @click="openLink(row)">
                    <el-icon><Position /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="监控详情" placement="top">
                  <el-button circle size="small" type="primary" plain @click="openMonitoring(row)">
                    <el-icon><DataAnalysis /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip :content="row.status === 'ACTIVE' ? '停用链接' : '重新启用'" placement="top">
                  <el-button
                    circle
                    size="small"
                    :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                    plain
                    :disabled="row.status === 'EXPIRED'"
                    :loading="operatingId === row.id"
                    @click="toggleShareStatus(row)"
                  >
                    <el-icon><component :is="row.status === 'ACTIVE' ? 'Lock' : 'Unlock'" /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="永久删除" placement="top">
                  <el-button circle size="small" type="danger" plain :loading="operatingId === row.id" @click="removeShare(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>

    <el-dialog
      v-model="monitoringDialogVisible"
      width="920px"
      class="modern-dialog monitoring-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="monitoring-header">
          <div>
            <h3>链接监控详情</h3>
            <p>{{ monitoringTarget?.shareKey || '未选择分享' }}</p>
          </div>
          <el-button type="primary" plain round size="small" @click="refreshMonitoring" :loading="monitoringLoading">
            <el-icon class="el-icon--left"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <div v-loading="monitoringLoading" class="monitoring-body">
        <template v-if="monitoringDetails">
          <div class="monitoring-stats-grid">
            <UiStatCard label="累计 PV" :value="monitoringDetails.pv" />
            <UiStatCard label="独立访客 UV" :value="monitoringDetails.uv" />
            <UiStatCard label="独立 IP" :value="monitoringDetails.uip" />
            <UiStatCard label="访问授权次数" :value="monitoringDetails.accessCount" />
          </div>

          <div class="monitoring-meta">
            <span>最近访问时间：{{ formatDateTime(monitoringDetails.lastAccessAt) }}</span>
            <span>最近访客 IP：{{ displayText(monitoringDetails.lastVisitorIp) }}</span>
            <span>最近访客地址：{{ displayText(monitoringDetails.lastVisitorAddress) }}</span>
          </div>

          <div class="trend-row">
            <div class="trend-item" v-for="item in monitoringDetails.dailyTrend" :key="item.date">
              <span>{{ item.date }}</span>
              <strong>{{ item.count }}</strong>
            </div>
          </div>

          <div class="dimension-grid">
            <section class="dimension-card">
              <h4>浏览器分布</h4>
              <div class="dimension-item" v-for="item in monitoringDetails.browserStats" :key="`browser-${item.name}`">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <el-empty v-if="!monitoringDetails.browserStats.length" description="暂无数据" :image-size="60" />
            </section>

            <section class="dimension-card">
              <h4>系统分布</h4>
              <div class="dimension-item" v-for="item in monitoringDetails.osStats" :key="`os-${item.name}`">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <el-empty v-if="!monitoringDetails.osStats.length" description="暂无数据" :image-size="60" />
            </section>

            <section class="dimension-card">
              <h4>来源分布</h4>
              <div class="dimension-item" v-for="item in monitoringDetails.refererStats" :key="`referer-${item.name}`">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <el-empty v-if="!monitoringDetails.refererStats.length" description="暂无数据" :image-size="60" />
            </section>
          </div>

          <div class="recent-table-wrap">
            <h4>最近访问记录</h4>
            <el-table :data="monitoringDetails.recentVisits" size="small" stripe max-height="300">
              <el-table-column label="时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.accessedAt) }}</template>
              </el-table-column>
              <el-table-column label="IP" prop="ip" min-width="120" />
              <el-table-column label="地址" min-width="160">
                <template #default="{ row }">{{ displayText(row.address) }}</template>
              </el-table-column>
              <el-table-column label="浏览器" prop="browser" width="100" />
              <el-table-column label="系统" prop="os" width="100" />
              <el-table-column label="来源" min-width="180">
                <template #default="{ row }">{{ displayText(row.referer) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.modern-share-manage {
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

.base-card { background: var(--bg-card); border-radius: 16px; border: 1px solid #e2e8f0; }

/* Header Area */
.app-header { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: start; margin-bottom: 24px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 800; color: #0f172a; margin: 12px 0; }
.subtitle { color: #64748b; font-size: 14px; margin: 0; }
.header-actions { display: flex; gap: 12px; }

.header-stats-strip { 
  display: flex; 
  align-items: center; 
  gap: 32px; 
  border-top: 1px solid #e2e8f0; 
  padding-top: 24px; 
  overflow-x: auto; 
}

/* Main Workspace */
.workspace-main {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.table-toolbar {
  padding: 0 24px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.breadcrumb-active {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.table-container { padding: 0 24px; }
.empty-state { padding: 60px 0; }

/* Table Item Styles */
.file-item { display: flex; align-items: center; gap: 14px; }
.file-icon {
  width: 42px; height: 42px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 12px;
  font-size: 20px;
  background: #e0f2fe; color: #0284c7;
}
.file-icon.is-folder { background: #fef3c7; color: #d97706; }

.file-info { display: flex; flex-direction: column; }
.file-name { color: #0f172a; font-weight: 600; font-size: 14px; margin-bottom: 2px; }
.file-meta { color: #94a3b8; font-size: 12px; }

.mono-link { font-family: monospace; font-size: 13px; }
.text-secondary { color: #475569; font-weight: 500; }
.dim { color: #cbd5e1; font-weight: 400; }

.action-buttons { display: flex; gap: 8px; justify-content: flex-end; }

.monitoring-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.monitoring-header h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.monitoring-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.monitoring-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 220px;
}

.monitoring-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.monitoring-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  font-size: 13px;
  color: #475569;
}

.trend-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.trend-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.trend-item span {
  font-size: 12px;
  color: #64748b;
}

.trend-item strong {
  font-size: 16px;
  color: #0f172a;
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.dimension-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.dimension-card h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #334155;
}

.dimension-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #e2e8f0;
  color: #475569;
  font-size: 13px;
}

.dimension-item:last-child {
  border-bottom: none;
}

.recent-table-wrap h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #334155;
}

@media (max-width: 960px) {
  .header-main { flex-direction: column; gap: 20px; }
  .header-actions { width: 100%; }
  .header-actions .el-button { flex: 1; }

  .monitoring-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .monitoring-meta {
    grid-template-columns: 1fr;
  }

  .trend-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dimension-grid {
    grid-template-columns: 1fr;
  }
}
</style>
