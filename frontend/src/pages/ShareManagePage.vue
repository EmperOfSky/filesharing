<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import fileService from '@/services/fileService'

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
  expireTime?: string
  createdAt: string
  sharedContent?: {
    name?: string
  }
}

const loading = ref(false)
const operatingId = ref<number | null>(null)
const shares = ref<ShareRow[]>([])
const page = ref(0)
const size = ref(20)

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

const loadShares = async () => {
  try {
    loading.value = true
    const data = await fileService.getMyShares(page.value, size.value)
    shares.value = Array.isArray(data)
      ? data.map((item: ShareRow) => ({ ...item, shortLink: normalizeLink(item.shortLink) }))
      : []
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '加载分享列表失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const copyLink = async (row: ShareRow) => {
  if (!row.shortLink) {
    ElMessage.warning('短链为空，无法复制')
    return
  }
  try {
    await navigator.clipboard.writeText(row.shortLink)
    ElMessage.success('短链已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const toggleShareStatus = async (row: ShareRow) => {
  try {
    operatingId.value = row.id
    if (row.status === 'ACTIVE') {
      await fileService.disableShare(row.id)
      ElMessage.success('分享已禁用')
    } else {
      await fileService.enableShare(row.id)
      ElMessage.success('分享已启用')
    }
    await loadShares()
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    operatingId.value = null
  }
}

const removeShare = async (row: ShareRow) => {
  try {
    await ElMessageBox.confirm('确认删除该分享记录？删除后短链将立即失效。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    operatingId.value = row.id
    await fileService.deleteShare(row.id)
    ElMessage.success('分享已删除')
    await loadShares()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(message)
    }
  } finally {
    operatingId.value = null
  }
}

const formatDateTime = (v?: string) => {
  if (!v) return '-'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return d.toLocaleString()
}

onMounted(() => {
  loadShares()
})
</script>

<template>
  <div class="share-manage-page">
    <div class="header">
      <h2>分享管理</h2>
      <el-button :loading="loading" @click="loadShares">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="shares" v-loading="loading" stripe>
        <el-table-column label="标题 / 内容" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <div class="title">{{ row.title || row.sharedContent?.name || '-' }}</div>
              <div class="sub">{{ row.sharedContent?.name || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="短链" min-width="280">
          <template #default="{ row }">
            <el-link :href="row.shortLink" target="_blank" type="primary">{{ row.shortLink }}</el-link>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ACTIVE'" type="success">启用中</el-tag>
            <el-tag v-else-if="row.status === 'DISABLED'" type="warning">已禁用</el-tag>
            <el-tag v-else type="info">已过期</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="访问次数" width="130">
          <template #default="{ row }">
            <span>{{ row.currentAccessCount || 0 }} / {{ row.maxAccessCount > 0 ? row.maxAccessCount : '不限' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expireTime ? formatDateTime(row.expireTime) : '永久有效' }}
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyLink(row)">复制</el-button>
            <el-button
              link
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              :disabled="row.status === 'EXPIRED'"
              :loading="operatingId === row.id"
              @click="toggleShareStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" :loading="operatingId === row.id" @click="removeShare(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && shares.length === 0" description="暂无分享记录" />
    </el-card>
  </div>
</template>

<style scoped>
.share-manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title {
  font-weight: 600;
  color: #2c3e50;
}

.sub {
  color: #8a98a8;
  font-size: 12px;
}
</style>
