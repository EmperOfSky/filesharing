<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  BellFilled,
  CircleCheckFilled,
  Refresh,
  Timer
} from '@element-plus/icons-vue'
import UiStatCard from '@/components/ui/UiStatCard.vue'
import monitoringService, {
  type HealthCheckResult,
  type MetricHistory,
  type MonitoringAlert,
  type PerformanceReport,
  type SystemMetrics,
  type MonitoringStatistics
} from '@/services/monitoring'

const loading = ref(false)
const closingAlertId = ref<string | null>(null)
const metrics = ref<SystemMetrics | null>(null)
const health = ref<HealthCheckResult | null>(null)
const report = ref<PerformanceReport | null>(null)
const alerts = ref<MonitoringAlert[]>([])
const statistics = ref<MonitoringStatistics | null>(null)
const metricHistory = ref<MetricHistory | null>(null)
const selectedMetric = ref('heap_memory_usage')

const metricOptions = [
  { label: '堆内存使用率', value: 'heap_memory_usage' },
  { label: '线程数', value: 'thread_count' }
]

const router = useRouter()

const resolveHealthStatusCode = (status: unknown) => {
  if (typeof status === 'string') {
    return status.toUpperCase()
  }
  if (status && typeof status === 'object') {
    const shapedStatus = status as { status?: unknown; code?: unknown }
    if (typeof shapedStatus.status === 'string') {
      return shapedStatus.status.toUpperCase()
    }
    if (typeof shapedStatus.code === 'string') {
      return shapedStatus.code.toUpperCase()
    }
  }
  return ''
}

const isHealthy = computed(() => resolveHealthStatusCode(health.value?.status) === 'UP')

const formattedLoad = computed(() => {
  const value = metrics.value?.systemLoadAverage
  if (value === undefined || value === null || value < 0) return '--'
  return value.toFixed(2)
})

const heapUsagePercent = computed(() => {
  const value = metrics.value?.heapMemoryUsage
  if (value === undefined || value === null || value < 0) return 0
  return Math.round(value * 100)
})

const threadCount = computed(() => metrics.value?.threadCount ?? 0)

const alertCount = computed(() => alerts.value.length)

const componentEntries = computed(() => Object.entries(health.value?.components || {}))

const historyPoints = computed(() => metricHistory.value?.dataPoints || [])

const historyMax = computed(() => {
  const values = historyPoints.value.map((point) => point.value)
  return values.length ? Math.max(...values, 1) : 1
})

const healthStatusLabel = (status: unknown) => {
  const normalized = resolveHealthStatusCode(status)
  if (!normalized) return '未知'
  if (normalized === 'UP') return '正常'
  if (normalized === 'DOWN') return '故障'
  if (normalized === 'OUT_OF_SERVICE') return '不可用'
  return normalized
}

const healthStatusType = (status: unknown) => {
  const normalized = resolveHealthStatusCode(status)
  if (normalized === 'UP') return 'success'
  if (normalized === 'DOWN') return 'danger'
  if (normalized === 'OUT_OF_SERVICE') return 'warning'
  return 'info'
}

const alertLevelType = (level?: string) => {
  const normalized = (level || '').toUpperCase()
  if (normalized === 'CRITICAL') return 'danger'
  if (normalized === 'WARNING') return 'warning'
  if (normalized === 'INFO') return 'info'
  return 'primary'
}

const formatBytes = (value?: number) => {
  if (!value || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null || Number.isNaN(value)) return '--'
  return `${value.toFixed(1)}%`
}

const formatDuration = (ms?: number) => {
  if (!ms && ms !== 0) return '--'
  const totalSeconds = Math.floor(ms / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)

  if (days > 0) return `${days} 天 ${hours} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟`
  return `${minutes} 分钟`
}

const formatDateTime = (value?: string | number) => {
  if (!value) return '--'
  const date = typeof value === 'number' ? new Date(value) : new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const metricTitle = (name: string) => {
  const mapping: Record<string, string> = {
    heap_memory_usage: '堆内存使用率',
    thread_count: '线程数'
  }
  return mapping[name] || name
}

const summarizeDetails = (details?: Record<string, unknown>) => {
  if (!details || Object.keys(details).length === 0) return '--'
  return Object.entries(details)
    .slice(0, 2)
    .map(([key, value]) => `${key}: ${String(value)}`)
    .join(' · ')
}

const loadMetricHistory = async () => {
  try {
    metricHistory.value = await monitoringService.getMetricHistory(selectedMetric.value, 24)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载趋势数据失败'
    ElMessage.error(message)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [systemMetrics, healthCheck, performanceReport, alertResult, stats] = await Promise.all([
      monitoringService.getSystemMetrics(),
      monitoringService.getHealthCheck(),
      monitoringService.getPerformanceReport(7),
      monitoringService.getAlerts({ status: 'OPEN', limit: 20 }),
      monitoringService.getStatistics()
    ])

    metrics.value = systemMetrics
    health.value = healthCheck
    report.value = performanceReport
    alerts.value = alertResult.alerts || []
    statistics.value = stats

    await loadMetricHistory()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载系统负载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const onMetricChange = async () => {
  await loadMetricHistory()
}

const closeAlert = async (alert: MonitoringAlert) => {
  try {
    await ElMessageBox.confirm(`确认关闭告警「${alert.title}」吗？`, '关闭告警', {
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
      type: 'warning'
    })

    closingAlertId.value = alert.id
    await monitoringService.closeAlert(alert.id)
    ElMessage.success('告警已关闭')
    await loadData()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '关闭告警失败'
    ElMessage.error(message)
  } finally {
    closingAlertId.value = null
  }
}

const refreshPage = async () => {
  await loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="system-load-page" v-loading="loading">
    <header class="hero-card">
      <div class="hero-copy">
        <span class="kicker">Admin Console</span>
        <h1>系统负载监控</h1>
        <p>此页面仅对管理员开放，用于查看 JVM 资源、系统负载、健康状态和告警情况。</p>
        <div class="hero-actions">
          <el-button type="primary" color="#0ea5e9" size="large" @click="refreshPage" round>
            <el-icon class="el-icon--left"><Refresh /></el-icon>刷新数据
          </el-button>
          <el-button size="large" round plain @click="router.push('/dashboard')">返回工作台</el-button>
        </div>
      </div>

      <div class="hero-status">
        <div class="status-chip" :class="isHealthy ? 'is-healthy' : 'is-warning'">
          <el-icon><CircleCheckFilled /></el-icon>
          <span>{{ isHealthy ? '系统运行正常' : '系统存在异常' }}</span>
        </div>
        <div class="status-chip subtle">
          <el-icon><Timer /></el-icon>
          <span>采样时间 {{ formatDateTime(metrics?.timestamp) }}</span>
        </div>
        <div class="status-chip subtle">
          <el-icon><BellFilled /></el-icon>
          <span>开放告警 {{ alertCount }} 条</span>
        </div>
      </div>
    </header>

    <section class="stat-grid">
      <UiStatCard label="系统负载平均值" :value="formattedLoad" hint="OperatingSystemMXBean 返回的系统负载" />
      <UiStatCard label="堆内存占用" :value="formatPercent(heapUsagePercent)" hint="当前 JVM 堆空间使用率" />
      <UiStatCard label="活动线程数" :value="threadCount" hint="当前 JVM 活跃线程数量" />
      <UiStatCard label="当前告警" :value="alertCount" hint="系统监控模块中的开放告警" />
    </section>

    <section class="content-grid">
      <article class="panel-card panel-history">
        <div class="panel-head">
          <div>
            <span class="panel-tag">趋势采样</span>
            <h2>近期指标变化</h2>
          </div>
          <div class="panel-actions">
            <el-select v-model="selectedMetric" size="small" @change="onMetricChange" style="width: 180px">
              <el-option v-for="item in metricOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>

        <div class="history-summary">
          <div class="summary-item">
            <span class="label">指标名称</span>
            <strong>{{ metricTitle(metricHistory?.metricName || selectedMetric) }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">采样点数</span>
            <strong>{{ metricHistory?.count || 0 }}</strong>
          </div>
        </div>

        <div class="history-chart" v-if="historyPoints.length">
          <div
            v-for="point in historyPoints"
            :key="`${point.timestamp}-${point.value}`"
            class="history-bar-wrap"
          >
            <div class="history-bar" :style="{ height: `${Math.max((point.value / historyMax) * 100, 8)}%` }"></div>
            <span class="history-value">{{ point.value.toFixed(2) }}</span>
            <span class="history-time">{{ formatDateTime(point.timestamp) }}</span>
          </div>
        </div>

        <el-empty v-else description="暂无趋势采样数据" />
      </article>

      <article class="panel-card panel-health">
        <div class="panel-head">
          <div>
            <span class="panel-tag">健康检查</span>
            <h2>组件状态概览</h2>
          </div>
          <el-tag :type="isHealthy ? 'success' : 'warning'" effect="light" round>
            {{ healthStatusLabel(health?.status) }}
          </el-tag>
        </div>

        <div class="health-list">
          <div v-for="[name, component] in componentEntries" :key="name" class="health-item">
            <div class="health-main">
              <div>
                <strong>{{ name }}</strong>
                <p>{{ summarizeDetails(component.details) }}</p>
              </div>
              <el-tag :type="healthStatusType(component.status)" effect="light" round>
                {{ healthStatusLabel(component.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="meta-grid">
          <div class="meta-chip">
            <span class="meta-label">总运行时间</span>
            <strong>{{ formatDuration(metrics?.uptime) }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">峰值线程数</span>
            <strong>{{ metrics?.peakThreadCount ?? '--' }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">守护线程数</span>
            <strong>{{ metrics?.daemonThreadCount ?? '--' }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">非堆内存</span>
            <strong>{{ formatBytes(metrics?.nonHeapMemoryUsed) }}</strong>
          </div>
        </div>
      </article>
    </section>

    <section class="report-grid">
      <article class="panel-card report-card">
        <div class="panel-head compact">
          <div>
            <span class="panel-tag">性能报告</span>
            <h2>7 天统计摘要</h2>
          </div>
          <el-tag type="info" effect="light" round>Generated {{ formatDateTime(report?.generatedAt) }}</el-tag>
        </div>

        <div class="mini-stats">
          <div class="mini-stat">
            <span>CPU 平均值</span>
            <strong>{{ formatPercent((report?.cpuUsageStats?.average || 0) * 100) }}</strong>
          </div>
          <div class="mini-stat">
            <span>内存平均值</span>
            <strong>{{ formatPercent((report?.memoryUsageStats?.average || 0) * 100) }}</strong>
          </div>
          <div class="mini-stat">
            <span>磁盘 I/O 平均值</span>
            <strong>{{ report?.diskIoStats?.average?.toFixed(2) ?? '0.00' }}</strong>
          </div>
          <div class="mini-stat">
            <span>网络流量平均值</span>
            <strong>{{ report?.networkTrafficStats?.average?.toFixed(2) ?? '0.00' }}</strong>
          </div>
        </div>

        <div class="trend-list" v-if="report?.trends?.length">
          <div v-for="trend in report.trends" :key="trend.metricName" class="trend-item">
            <div>
              <strong>{{ metricTitle(trend.metricName) }}</strong>
              <p>趋势方向：{{ trend.trendDirection }}</p>
            </div>
            <el-tag :type="trend.severity === 'HIGH' ? 'danger' : 'success'" effect="light" round>
              {{ trend.severity }}
            </el-tag>
          </div>
        </div>
      </article>

      <article class="panel-card stats-card">
        <div class="panel-head compact">
          <div>
            <span class="panel-tag">监控配置</span>
            <h2>系统监控概况</h2>
          </div>
        </div>

        <div class="meta-grid single-column">
          <div class="meta-chip">
            <span class="meta-label">开放监控指标</span>
            <strong>{{ statistics?.activeMetrics || '--' }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">监控组件</span>
            <strong>{{ statistics?.monitoredComponents || '--' }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">告警总数</span>
            <strong>{{ statistics?.alertCount ?? 0 }}</strong>
          </div>
          <div class="meta-chip">
            <span class="meta-label">最后检查时间</span>
            <strong>{{ formatDateTime(statistics?.lastCheckTime) }}</strong>
          </div>
        </div>
      </article>
    </section>

    <section class="panel-card alert-card">
      <div class="panel-head">
        <div>
          <span class="panel-tag">告警中心</span>
          <h2>开放告警列表</h2>
        </div>
        <el-tag type="warning" effect="light" round>{{ alertCount }} 条</el-tag>
      </div>

      <el-table :data="alerts" class="alert-table" v-loading="loading" empty-text="暂无开放告警" style="width: 100%">
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <span class="dim-text">{{ formatDateTime(row.timestamp) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="110">
          <template #default="{ row }">
            <el-tag :type="alertLevelType(row.level)" effect="light" round>{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="消息" min-width="320">
          <template #default="{ row }">
            <span class="dim-text">{{ row.message }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'danger' : 'success'" effect="light" round>
              {{ row.status === 'OPEN' ? '开放' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'OPEN'"
              size="small"
              type="primary"
              plain
              :loading="closingAlertId === row.id"
              @click="closeAlert(row)"
            >
              关闭
            </el-button>
            <span v-else class="dim-text">--</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.system-load-page {
  --primary: #0ea5e9;
  --primary-soft: #e0f2fe;
  --accent: #7c3aed;
  --bg: #f8fafc;
  --card: rgba(255, 255, 255, 0.92);
  --border: rgba(148, 163, 184, 0.22);
  --text: #0f172a;
  --muted: #64748b;

  min-height: calc(100vh - 60px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(124, 58, 237, 0.12), transparent 26%),
    var(--bg);
}

.hero-card,
.panel-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 24px;
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.hero-card {
  padding: 32px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
}

.hero-copy {
  max-width: 620px;
}

.kicker,
.panel-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1,
.panel-head h2 {
  margin: 12px 0 10px;
  color: var(--text);
}

.hero-copy h1 {
  font-size: 34px;
  line-height: 1.1;
}

.hero-copy p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
  flex-wrap: wrap;
}

.hero-status {
  display: grid;
  gap: 12px;
  min-width: 280px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.22);
  color: var(--text);
  font-weight: 600;
}

.status-chip.is-healthy {
  background: rgba(16, 185, 129, 0.08);
  color: #065f46;
}

.status-chip.is-warning {
  background: rgba(245, 158, 11, 0.08);
  color: #92400e;
}

.status-chip.subtle {
  color: var(--muted);
  font-weight: 500;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.content-grid,
.report-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 20px;
}

.panel-card {
  padding: 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head.compact {
  margin-bottom: 16px;
}

.panel-head h2 {
  font-size: 22px;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--muted);
}

.history-summary,
.meta-grid,
.mini-stats {
  display: grid;
  gap: 12px;
}

.history-summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18px;
}

.summary-item,
.meta-chip,
.mini-stat {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 250, 252, 0.92);
  padding: 14px 16px;
}

.summary-item .label,
.meta-label,
.mini-stat span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.summary-item strong,
.meta-chip strong,
.mini-stat strong {
  color: var(--text);
  font-size: 18px;
}

.history-chart {
  height: 260px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(36px, 1fr));
  gap: 10px;
  align-items: end;
  padding: 8px 0 0;
}

.history-bar-wrap {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.history-bar {
  width: 100%;
  min-height: 10px;
  border-radius: 12px 12px 6px 6px;
  background: linear-gradient(180deg, #38bdf8 0%, #0ea5e9 100%);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.25);
  transition: transform 0.2s ease;
}

.history-bar-wrap:hover .history-bar {
  transform: translateY(-2px);
}

.history-value,
.history-time {
  font-size: 11px;
  color: var(--muted);
  text-align: center;
  word-break: break-word;
}

.health-list,
.trend-list {
  display: grid;
  gap: 12px;
}

.health-item,
.trend-item {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 250, 252, 0.92);
  padding: 14px 16px;
}

.health-main,
.trend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.health-main p,
.trend-item p,
.dim-text {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.meta-grid {
  margin-top: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.single-column {
  grid-template-columns: 1fr;
}

.mini-stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 16px;
}

.alert-card {
  overflow: hidden;
}

:deep(.alert-table) {
  --el-table-border-color: rgba(148, 163, 184, 0.18);
  --el-table-header-bg-color: #f8fafc;
}

@media (max-width: 1200px) {
  .stat-grid,
  .report-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .meta-grid,
  .history-summary,
  .mini-stats {
    grid-template-columns: 1fr;
  }
}
</style>