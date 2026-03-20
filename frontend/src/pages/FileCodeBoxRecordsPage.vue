<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import fileCodeBoxService, { type FileCodeBoxRecordItem } from '@/services/fileCodeBox'

const loading = ref(false)
const operatingId = ref<number | null>(null)
const batchOperating = ref(false)
const exporting = ref(false)
const records = ref<FileCodeBoxRecordItem[]>([])
const selectedRecordIds = ref<number[]>([])
const previewText = ref('')
const previewVisible = ref(false)

const filter = reactive({
  keyword: '',
  status: '',
  shareType: ''
})

const pager = reactive({
  page: 1,
  size: 20,
  total: 0
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '启用中', value: 'ACTIVE' },
  { label: '已禁用', value: 'DISABLED' },
  { label: '已过期', value: 'EXPIRED' }
]

const shareTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '文件', value: 'FILE' },
  { label: '文本', value: 'TEXT' }
]

const fetchRecords = async () => {
  try {
    loading.value = true
    const data = await fileCodeBoxService.getAdminRecords({
      page: Math.max(0, pager.page - 1),
      size: pager.size,
      keyword: filter.keyword.trim() || undefined,
      status: filter.status || undefined,
      share_type: filter.shareType || undefined
    })

    records.value = Array.isArray(data.content) ? data.content : []
    selectedRecordIds.value = selectedRecordIds.value.filter((id) => records.value.some((record) => record.id === id))
    pager.total = Number(data.totalElements || 0)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '加载记录失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  filter.keyword = ''
  filter.status = ''
  filter.shareType = ''
  pager.page = 1
  await fetchRecords()
}

const onSearch = async () => {
  pager.page = 1
  await fetchRecords()
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

const formatSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let value = bytes
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const statusTagType = (status: string) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'warning'
  return 'info'
}

const statusLabel = (status: string) => {
  if (status === 'ACTIVE') return '启用中'
  if (status === 'DISABLED') return '已禁用'
  return '已过期'
}

const copyCode = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('取件码已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const openTextPreview = (row: FileCodeBoxRecordItem) => {
  previewText.value = row.text_content || ''
  previewVisible.value = true
}

const canToggle = (row: FileCodeBoxRecordItem) => row.status !== 'EXPIRED'

const onSelectionChange = (rows: FileCodeBoxRecordItem[]) => {
  selectedRecordIds.value = rows.map((row) => row.id)
}

const selectedRows = computed(() => {
  if (selectedRecordIds.value.length === 0) return []
  const idSet = new Set(selectedRecordIds.value)
  return records.value.filter((row) => idSet.has(row.id))
})

const runBatchStatusUpdate = async (targetStatus: 'ACTIVE' | 'DISABLED') => {
  const rows = selectedRows.value.filter((row) => row.status !== 'EXPIRED' && row.status !== targetStatus)
  if (rows.length === 0) {
    ElMessage.warning('当前选择中没有可操作的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将 ${rows.length} 条记录设为${targetStatus === 'ACTIVE' ? '启用' : '禁用'}？`,
      '批量操作确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    batchOperating.value = true
    const results = await Promise.allSettled(
      rows.map((row) => fileCodeBoxService.updateAdminRecordStatus(row.id, targetStatus))
    )

    const successCount = results.filter((result) => result.status === 'fulfilled').length
    const failCount = results.length - successCount
    if (successCount > 0) {
      ElMessage.success(`批量操作完成，成功 ${successCount} 条${failCount > 0 ? `，失败 ${failCount} 条` : ''}`)
    } else {
      ElMessage.error('批量状态更新失败')
    }

    await fetchRecords()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '批量状态更新失败'
      ElMessage.error(message)
    }
  } finally {
    batchOperating.value = false
  }
}

const runBatchDelete = async () => {
  const rows = selectedRows.value
  if (rows.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认删除已选 ${rows.length} 条记录？删除后不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    batchOperating.value = true
    const results = await Promise.allSettled(rows.map((row) => fileCodeBoxService.deleteAdminRecord(row.id)))
    const successCount = results.filter((result) => result.status === 'fulfilled').length
    const failCount = results.length - successCount

    if (successCount > 0) {
      ElMessage.success(`批量删除完成，成功 ${successCount} 条${failCount > 0 ? `，失败 ${failCount} 条` : ''}`)
    } else {
      ElMessage.error('批量删除失败')
    }

    if (records.value.length === rows.length && pager.page > 1) {
      pager.page -= 1
    }
    await fetchRecords()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '批量删除失败'
      ElMessage.error(message)
    }
  } finally {
    batchOperating.value = false
  }
}

const csvEscape = (value: unknown) => {
  const text = value == null ? '' : String(value)
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const downloadCsvBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}

const exportCsv = async (mode: 'selected' | 'filtered') => {
  try {
    exporting.value = true

    if (mode === 'filtered') {
      const blob = await fileCodeBoxService.exportAdminRecordsCsv({
        keyword: filter.keyword.trim() || undefined,
        status: filter.status || undefined,
        share_type: filter.shareType || undefined
      })
      downloadCsvBlob(blob, `fcb_records_filtered_${new Date().toISOString().replace(/[:.]/g, '-')}.csv`)
      ElMessage.success('服务端导出成功')
      return
    }

    const rows = selectedRows.value
    if (rows.length === 0) {
      ElMessage.warning('当前没有选中记录可导出')
      return
    }

    const header = ['取件码', '名称', '类型', '状态', '大小(bytes)', '已用次数', '剩余次数', '过期时间', '创建时间']
    const lines = rows.map((row) => {
      const remain = row.is_count_limited ? row.remain_count ?? 0 : '不限'
      return [
        row.code,
        row.display_name || '',
        row.share_type,
        row.status,
        row.size_bytes || 0,
        row.used_count || 0,
        remain,
        row.expire_at || '永久有效',
        row.created_at || ''
      ]
        .map(csvEscape)
        .join(',')
    })

    const csv = ['\uFEFF' + header.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    downloadCsvBlob(blob, `fcb_records_selected_${new Date().toISOString().replace(/[:.]/g, '-')}.csv`)

    ElMessage.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '导出失败'
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}

const toggleStatus = async (row: FileCodeBoxRecordItem) => {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    operatingId.value = row.id
    await fileCodeBoxService.updateAdminRecordStatus(row.id, target)
    ElMessage.success(target === 'ACTIVE' ? '记录已启用' : '记录已禁用')
    await fetchRecords()
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    operatingId.value = null
  }
}

const removeRecord = async (row: FileCodeBoxRecordItem) => {
  try {
    await ElMessageBox.confirm(
      `确认删除记录 ${row.code}？删除后该取件码将不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    operatingId.value = row.id
    await fileCodeBoxService.deleteAdminRecord(row.id)
    ElMessage.success('记录已删除')

    if (records.value.length === 1 && pager.page > 1) {
      pager.page -= 1
    }
    await fetchRecords()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(message)
    }
  } finally {
    operatingId.value = null
  }
}

const remainCountLabel = computed(() => {
  return (row: FileCodeBoxRecordItem) => {
    if (!row.is_count_limited) return '不限'
    return row.remain_count ?? 0
  }
})

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="fcb-records-page">
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="filter.keyword"
            clearable
            placeholder="按取件码或文件名搜索"
            style="width: 240px"
            @keyup.enter="onSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filter.status" style="width: 150px">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="类型">
          <el-select v-model="filter.shareType" style="width: 150px">
            <el-option v-for="opt in shareTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :loading="loading" @click="fetchRecords">刷新</el-button>
        </el-form-item>

        <el-form-item>
          <el-button
            type="success"
            :disabled="selectedRecordIds.length === 0"
            :loading="batchOperating"
            @click="runBatchStatusUpdate('ACTIVE')"
          >
            批量启用
          </el-button>
          <el-button
            type="warning"
            :disabled="selectedRecordIds.length === 0"
            :loading="batchOperating"
            @click="runBatchStatusUpdate('DISABLED')"
          >
            批量禁用
          </el-button>
          <el-button
            type="danger"
            :disabled="selectedRecordIds.length === 0"
            :loading="batchOperating"
            @click="runBatchDelete"
          >
            批量删除
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button :loading="exporting" @click="exportCsv('filtered')">导出筛选结果</el-button>
          <el-button :loading="exporting" :disabled="selectedRecordIds.length === 0" @click="exportCsv('selected')">
            导出已选
          </el-button>
        </el-form-item>
      </el-form>

      <div class="selection-summary">已选 {{ selectedRecordIds.length }} 条记录</div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" stripe v-loading="loading" row-key="id" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" fixed="left" />

        <el-table-column label="取件码" width="140">
          <template #default="{ row }">
            <el-space>
              <span class="mono">{{ row.code }}</span>
              <el-button link type="primary" @click="copyCode(row.code)">复制</el-button>
            </el-space>
          </template>
        </el-table-column>

        <el-table-column label="名称 / 类型" min-width="220">
          <template #default="{ row }">
            <div class="name-col">
              <div class="name">{{ row.display_name || (row.share_type === 'TEXT' ? '文本分享' : '-') }}</div>
              <div class="meta">{{ row.share_type === 'TEXT' ? '文本' : '文件' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.size_bytes || 0) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="访问" width="130">
          <template #default="{ row }">
            {{ row.used_count || 0 }} / {{ remainCountLabel(row) }}
          </template>
        </el-table-column>

        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">
            {{ row.expire_at ? formatDateTime(row.expire_at) : '永久有效' }}
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.share_type === 'TEXT'"
              link
              type="primary"
              @click="openTextPreview(row)"
            >
              文本预览
            </el-button>
            <el-button
              link
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              :disabled="!canToggle(row)"
              :loading="operatingId === row.id"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              :loading="operatingId === row.id"
              @click="removeRecord(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && records.length === 0" description="暂无兼容记录" />

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pager.total"
          @size-change="fetchRecords"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" title="文本内容预览" width="720px">
      <el-input :model-value="previewText" type="textarea" :rows="14" readonly />
    </el-dialog>
  </div>
</template>

<style scoped>
.fcb-records-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card :deep(.el-card__body) {
  padding-bottom: 4px;
}

.name-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name {
  color: #1f2d3d;
  font-weight: 600;
}

.meta {
  color: #8a98a8;
  font-size: 12px;
}

.mono {
  font-family: Consolas, Menlo, Monaco, monospace;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.selection-summary {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 13px;
}
</style>
