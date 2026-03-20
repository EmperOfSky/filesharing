<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import fileCodeBoxService, { type FileCodeBoxRecordItem } from '@/services/fileCodeBox'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  status: '',
  share_type: ''
})

const records = ref<FileCodeBoxRecordItem[]>([])
const previewVisible = ref(false)
const previewRecord = ref<FileCodeBoxRecordItem | null>(null)

const activeCount = computed(() => records.value.filter((item) => item.status === 'ACTIVE').length)
const expiredCount = computed(() => records.value.filter((item) => item.is_expired || item.status === 'EXPIRED').length)
const disabledCount = computed(() => records.value.filter((item) => item.status === 'DISABLED').length)
const totalVisits = computed(() => records.value.reduce((sum, item) => sum + Number(item.used_count || 0), 0))
const attentionRecords = computed(() =>
  records.value.filter((item) => item.status !== 'ACTIVE' || Number(item.used_count || 0) > 0).slice(0, 5)
)

const loadRecords = async () => {
  loading.value = true
  try {
    const result = await fileCodeBoxService.getAdminRecords({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      share_type: filters.share_type || undefined
    })
    records.value = result.content ||[]
    pagination.total = Number(result.totalElements || 0)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载快传记录失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.share_type = ''
  pagination.page = 0
  loadRecords()
}

const onPageChange = (page: number) => {
  pagination.page = page - 1
  loadRecords()
}

const onSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 0
  loadRecords()
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units =['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const statusTagType = (status: string) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'danger'
  return 'warning'
}

const typeLabel = (type: string) => (type === 'FILE' ? '文件' : '文本')

const openPreview = (record: FileCodeBoxRecordItem) => {
  previewRecord.value = record
  previewVisible.value = true
}

const toggleStatus = async (record: FileCodeBoxRecordItem) => {
  const targetStatus = record.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  try {
    await fileCodeBoxService.updateAdminRecordStatus(record.id, targetStatus)
    ElMessage.success(targetStatus === 'ACTIVE' ? '记录已重新启用' : '记录已停用')
    await loadRecords()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '更新记录状态失败'
    ElMessage.error(message)
  }
}

const removeRecord = async (record: FileCodeBoxRecordItem) => {
  try {
    await ElMessageBox.confirm(`确认删除取件码 ${record.code} 吗？该操作不可撤销。`, '删除记录', {
      type: 'warning'
    })
    await fileCodeBoxService.deleteAdminRecord(record.id)
    ElMessage.success('记录已删除')
    await loadRecords()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '删除记录失败'
    ElMessage.error(message)
  }
}

const exportCsv = async () => {
  exporting.value = true
  try {
    const blob = await fileCodeBoxService.exportAdminRecordsCsv({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      share_type: filters.share_type || undefined
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `quick-transfer-records-${Date.now()}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('CSV 导出成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '导出快传记录失败'
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadRecords()
})
</script>

<template>
  <div class="modern-audit-page">
    <!-- 顶部控制台 (Header) -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Transfer Audit</span>
          <h1 class="page-title">快传中转记录</h1>
          <p class="page-description">在此查阅和审计所有生成的快传取件码，管理文件及文本的分发状态。</p>
        </div>
        <div class="header-actions">
          <el-button size="large" @click="loadRecords" round plain :disabled="loading">
            <el-icon class="el-icon--left"><Refresh /></el-icon>刷新列表
          </el-button>
          <el-button type="primary" color="#6366f1" size="large" :loading="exporting" @click="exportCsv" round>
            <el-icon class="el-icon--left"><Download /></el-icon>导出 CSV
          </el-button>
        </div>
      </div>
      
      <div class="header-stats-strip">
        <UiStatCard label="活跃记录" :value="activeCount" hint="当前可用状态" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="已过期" :value="expiredCount" hint="已超出系统时效" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="已停用" :value="disabledCount" hint="管理员手动封禁" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="总计访问" :value="totalVisits" hint="历史提取次数汇总" />
      </div>
    </header>

    <!-- 工具栏 (Filter Toolbar) -->
    <section class="filter-toolbar base-card">
      <div class="filter-left">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索取件码、名称或来源 IP..."
          clearable
          class="modern-input search-input"
          @keyup.enter="loadRecords"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        
        <el-select v-model="filters.status" clearable placeholder="状态筛选" class="modern-select">
          <el-option label="🟢 活跃中" value="ACTIVE" />
          <el-option label="🟠 已过期" value="EXPIRED" />
          <el-option label="🔴 已停用" value="DISABLED" />
        </el-select>
        
        <el-select v-model="filters.share_type" clearable placeholder="类型筛选" class="modern-select">
          <el-option label="📁 文件分享" value="FILE" />
          <el-option label="📝 文本分享" value="TEXT" />
        </el-select>
      </div>
      
      <div class="filter-right">
        <el-button type="primary" color="#6366f1" @click="pagination.page = 0; loadRecords()" round>
          立即查询
        </el-button>
        <el-button @click="resetFilters" round>重置</el-button>
      </div>
    </section>

    <!-- 主体双栏布局 -->
    <div class="audit-layout">
      <!-- 左侧：数据表格 -->
      <main class="table-panel base-card">
        <div class="panel-header">
          <h2 class="panel-title">记录总览</h2>
          <span class="panel-meta">共找到 {{ pagination.total }} 条相关记录</span>
        </div>

        <div class="table-container">
          <el-table :data="records" v-loading="loading" class="modern-table" style="width: 100%">
            <template #empty>
              <div class="empty-state">
                <el-empty description="未找到匹配的快传记录" />
              </div>
            </template>

            <el-table-column prop="code" label="取件码" width="140">
              <template #default="{ row }">
                <span class="mono-badge">{{ row.code }}</span>
              </template>
            </el-table-column>

            <el-table-column label="分享内容" min-width="260">
              <template #default="{ row }">
                <div class="content-cell">
                  <div class="content-icon" :class="row.share_type === 'TEXT' ? 'is-text' : 'is-file'">
                    <el-icon><component :is="row.share_type === 'TEXT' ? 'Document' : 'FolderOpened'" /></el-icon>
                  </div>
                  <div class="content-info">
                    <span class="content-name">{{ row.display_name || '未命名内容' }}</span>
                    <span class="content-meta">{{ typeLabel(row.share_type) }} · {{ formatSize(row.size_bytes) }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <div class="status-indicator" :class="`is-${row.status.toLowerCase()}`">
                  <span class="dot"></span>
                  {{ row.status === 'ACTIVE' ? '活跃' : row.status === 'EXPIRED' ? '过期' : '已停用' }}
                </div>
              </template>
            </el-table-column>

            <el-table-column label="访问" width="100">
              <template #default="{ row }">
                <span class="text-strong">{{ row.used_count || 0 }}</span> 次
              </template>
            </el-table-column>

            <el-table-column label="剩余配额" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.is_count_limited ? 'info' : 'success'" effect="plain">
                  {{ row.is_count_limited ? `${row.remain_count ?? 0} 次` : '无限制' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">
                <span class="text-muted">{{ formatDateTime(row.created_at) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="160" fixed="right" align="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-tooltip content="查看详情" placement="top">
                    <el-button circle size="small" type="primary" plain @click="openPreview(row)">
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="row.status === 'DISABLED' ? '解除封禁' : '封禁记录'" placement="top">
                    <el-button circle size="small" :type="row.status === 'DISABLED' ? 'success' : 'warning'" plain @click="toggleStatus(row)">
                      <el-icon><component :is="row.status === 'DISABLED' ? 'Check' : 'Lock'" /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="彻底删除" placement="top">
                    <el-button circle size="small" type="danger" plain @click="removeRecord(row)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :current-page="pagination.page + 1"
            :page-sizes="[10, 20, 50]"
            :page-size="pagination.size"
            :total="pagination.total"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </main>

      <!-- 右侧：关注列表 -->
      <aside class="sidebar-panel">
        <section class="widget-card base-card">
          <div class="widget-header">
            <h2 class="widget-title">异常/热点关注</h2>
            <el-tooltip content="展示高频访问或状态异常的记录" placement="top">
              <el-icon class="widget-icon text-muted"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>

          <div v-if="attentionRecords.length" class="attention-list custom-scrollbar">
            <article v-for="row in attentionRecords" :key="row.id" class="attention-card" @click="openPreview(row)">
              <div class="card-top">
                <span class="mono-badge mini">{{ row.code }}</span>
                <span class="status-indicator mini" :class="`is-${row.status.toLowerCase()}`">
                  <span class="dot"></span>
                </span>
              </div>
              <div class="card-mid">
                <h4 class="item-name">{{ row.display_name || '未命名内容' }}</h4>
                <div class="item-stats">已访问 {{ row.used_count || 0 }} 次</div>
              </div>
              <div class="card-bot">
                <el-button link type="primary" size="small" @click.stop="openPreview(row)">查看详情</el-button>
                <el-button link :type="row.status === 'DISABLED' ? 'success' : 'warning'" size="small" @click.stop="toggleStatus(row)">
                  {{ row.status === 'DISABLED' ? '恢复' : '停用' }}
                </el-button>
              </div>
            </article>
          </div>

          <div v-else class="empty-widget">
            <el-empty description="当前一切平稳" :image-size="80" />
          </div>
        </section>
      </aside>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="previewVisible" title="🧾 快传记录详情" width="680px" class="modern-dialog" destroy-on-close>
      <div v-if="previewRecord" class="preview-layout">
        
        <!-- 核心属性网格 -->
        <div class="properties-grid">
          <div class="prop-item">
            <span class="prop-label">取件码</span>
            <span class="prop-value"><span class="mono-badge">{{ previewRecord.code }}</span></span>
          </div>
          <div class="prop-item">
            <span class="prop-label">内容类型</span>
            <span class="prop-value">{{ typeLabel(previewRecord.share_type) }}</span>
          </div>
          <div class="prop-item col-span-2">
            <span class="prop-label">显示名称</span>
            <span class="prop-value text-strong">{{ previewRecord.display_name || '未命名内容' }}</span>
          </div>
          <div class="prop-item">
            <span class="prop-label">内容体积</span>
            <span class="prop-value">{{ formatSize(previewRecord.size_bytes) }}</span>
          </div>
          <div class="prop-item">
            <span class="prop-label">来源 IP</span>
            <span class="prop-value mono-text">{{ previewRecord.created_ip || '未知' }}</span>
          </div>
        </div>

        <!-- 文本内容预览 -->
        <div v-if="previewRecord?.share_type === 'TEXT'" class="extra-panel">
          <div class="panel-subtitle"><el-icon><Document /></el-icon> 文本内容</div>
          <el-input 
            :model-value="previewRecord?.text_content || ''" 
            type="textarea" 
            :autosize="{ minRows: 6, maxRows: 15 }" 
            readonly 
            class="code-textarea"
          />
        </div>

        <!-- 文件存储详情 -->
        <div v-else-if="previewRecord" class="extra-panel">
          <div class="panel-subtitle"><el-icon><DataLine /></el-icon> 底层存储信息</div>
          <div class="properties-grid dense">
            <div class="prop-item col-span-2">
              <span class="prop-label">存储路径 (Storage Path)</span>
              <span class="prop-value mono-text text-wrap">{{ previewRecord.storage_path || '--' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">MIME 类型</span>
              <span class="prop-value">{{ previewRecord.content_type || '--' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">节点 (Config ID)</span>
              <span class="prop-value">{{ previewRecord.cloud_config_id || '--' }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 全局变量与底色设定 */
.modern-audit-page {
  --primary-color: #6366f1; /* 强调紫蓝色 */
  --primary-light: #e0e7ff;
  --text-main: #0f172a;
  --text-regular: #334155;
  --text-muted: #64748b;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --border-color: #e2e8f0;

  background-color: var(--bg-page);
  min-height: calc(100vh - 60px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 基础卡片样式 */
.base-card {
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--border-color);
}

/* 定制滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }

/* 顶部 Header */
.app-header { padding: 32px 32px 0 32px; overflow: hidden; }
.header-main { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; gap: 40px; }
.header-intro { flex: 1; max-width: 600px; }
.kicker-tag { display: inline-block; font-size: 13px; font-weight: 600; color: var(--primary-color); background: var(--primary-light); padding: 4px 12px; border-radius: 20px; margin-bottom: 12px; }
.page-title { font-size: 26px; font-weight: 800; color: var(--text-main); margin: 0 0 12px 0; letter-spacing: -0.5px; }
.page-description { font-size: 14px; color: var(--text-regular); line-height: 1.6; margin: 0; }
.header-actions { display: flex; gap: 12px; flex-shrink: 0; }

.header-stats-strip { display: grid; grid-template-columns: 1fr auto 1fr auto 1fr auto 1fr; align-items: center; border-top: 1px solid var(--border-color); padding: 20px 0; gap: 16px; }
:deep(.header-stats-strip .ui-stat-card) { padding: 0; background: transparent; border: none; box-shadow: none; }

/* 悬浮工具栏 (Toolbar) */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.filter-left { display: flex; gap: 12px; flex-wrap: wrap; flex: 1; }
.filter-right { display: flex; gap: 12px; }

.search-input { width: 280px; }
.modern-select { width: 150px; }
:deep(.modern-input .el-input__wrapper),
:deep(.modern-select .el-input__wrapper) {
  border-radius: 20px;
  background: #f8fafc;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.3s;
}
:deep(.modern-input .el-input__wrapper.is-focus),
:deep(.modern-select .el-input__wrapper.is-focus) { background: #fff; border-color: var(--primary-color); box-shadow: 0 0 0 3px var(--primary-light) !important; }

/* 布局网格 */
.audit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

/* 主干表格区 */
.table-panel { display: flex; flex-direction: column; overflow: hidden; }
.panel-header { padding: 20px 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.panel-title { font-size: 16px; font-weight: 700; margin: 0; color: var(--text-main); }
.panel-meta { font-size: 13px; color: var(--text-muted); }

/* 深度定制的 Element Plus 表格 */
.modern-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: #f8fafc;
}
:deep(.el-table th.el-table__cell) { font-weight: 600; color: var(--text-muted); font-size: 13px; padding: 14px 0; border-bottom: 1px solid var(--border-color); }
:deep(.el-table td.el-table__cell) { padding: 16px 0; border-bottom: 1px solid #f1f5f9; }
:deep(.el-table__row:last-child td.el-table__cell) { border-bottom: none; }
.empty-state { padding: 60px 0; }

/* 列表特定单元格样式 */
.mono-badge {
  font-family: 'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, monospace;
  background: #f1f5f9;
  color: #475569;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 13px;
  letter-spacing: 0.5px;
}
.mono-badge.mini { font-size: 12px; padding: 2px 6px; }
.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px;}
.text-strong { font-weight: 600; color: var(--text-main); }
.text-muted { color: var(--text-muted); font-size: 13px; }

.content-cell { display: flex; align-items: center; gap: 12px; }
.content-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0;}
.content-icon.is-file { background: #e0f2fe; color: #0284c7; }
.content-icon.is-text { background: #ffedd5; color: #ea580c; }
.content-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.content-name { font-size: 14px; font-weight: 600; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.content-meta { font-size: 12px; color: var(--text-muted); }

/* 状态指示器 (呼吸点) */
.status-indicator { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; }
.status-indicator .dot { width: 8px; height: 8px; border-radius: 50%; }
.status-indicator.mini .dot { width: 6px; height: 6px; }
.status-indicator.is-active { color: #10b981; }
.status-indicator.is-active .dot { background: #10b981; box-shadow: 0 0 0 2px #d1fae5; }
.status-indicator.is-expired { color: #f59e0b; }
.status-indicator.is-expired .dot { background: #f59e0b; box-shadow: 0 0 0 2px #fef3c7; }
.status-indicator.is-disabled { color: #ef4444; }
.status-indicator.is-disabled .dot { background: #ef4444; box-shadow: 0 0 0 2px #fee2e2; }

.row-actions { opacity: 0; transition: opacity 0.3s; display: flex; gap: 8px; justify-content: flex-end;}
:deep(.el-table__row:hover) .row-actions { opacity: 1; }

.pagination-wrapper { padding: 16px 24px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; }

/* 右侧侧边栏 Widgets */
.sidebar-panel { display: flex; flex-direction: column; gap: 24px; }
.widget-card { padding: 20px; display: flex; flex-direction: column; max-height: 800px;}
.widget-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.widget-title { font-size: 16px; font-weight: 700; margin: 0; color: var(--text-main); }
.empty-widget { padding: 40px 0; }

/* 关注卡片流 (Activity Feed) */
.attention-list { display: flex; flex-direction: column; gap: 12px; overflow-y: auto; padding-right: 4px; }
.attention-card {
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.attention-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px -4px rgba(0,0,0,0.05); border-color: #cbd5e1; background: #fff;}
.card-top { display: flex; justify-content: space-between; align-items: center; }
.card-mid { display: flex; flex-direction: column; gap: 4px; }
.item-name { font-size: 14px; font-weight: 600; color: var(--text-main); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-stats { font-size: 12px; color: var(--text-muted); }
.card-bot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; border-top: 1px dashed #e2e8f0; padding-top: 10px;}

/* 弹窗与属性面板 (Property Grid) */
:deep(.modern-dialog) { border-radius: 20px; overflow: hidden; }
:deep(.modern-dialog .el-dialog__header) { background: #f8fafc; padding: 20px 24px; border-bottom: 1px solid var(--border-color); margin: 0;}
:deep(.modern-dialog .el-dialog__title) { font-weight: 700; font-size: 18px; }
:deep(.modern-dialog .el-dialog__body) { padding: 24px; }

.preview-layout { display: flex; flex-direction: column; gap: 24px; }
.properties-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: var(--border-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}
.properties-grid.dense { grid-template-columns: 1fr; }
.prop-item { background: #fff; padding: 14px 16px; display: flex; flex-direction: column; gap: 4px; }
.prop-item.col-span-2 { grid-column: span 2; }
.prop-label { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.prop-value { font-size: 14px; color: var(--text-main); }
.text-wrap { word-break: break-all; white-space: normal; line-height: 1.5; }

.extra-panel { display: flex; flex-direction: column; gap: 12px; }
.panel-subtitle { font-size: 15px; font-weight: 600; color: var(--text-main); display: flex; align-items: center; gap: 6px; }
.code-textarea :deep(.el-textarea__inner) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  box-shadow: none !important;
  color: #334155;
}
.code-textarea :deep(.el-textarea__inner:focus) { background: #fff; border-color: var(--primary-color); }

/* 响应式适配 */
@media (max-width: 1120px) {
  .audit-layout { grid-template-columns: 1fr; }
  .header-main { flex-direction: column; }
  .header-actions { width: 100%; justify-content: flex-start; }
}

@media (max-width: 768px) {
  .hidden-mobile { display: none; }
  .header-stats-strip { grid-template-columns: 1fr 1fr; gap: 24px 16px; }
  .filter-toolbar { flex-direction: column; align-items: stretch; }
  .filter-left { flex-direction: column; }
  .search-input, .modern-select { width: 100%; }
  .properties-grid { grid-template-columns: 1fr; }
  .prop-item.col-span-2 { grid-column: span 1; }
}
</style>