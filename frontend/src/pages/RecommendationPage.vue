<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick,
  Delete,
  Refresh,
  View,
  Check,
  Trophy,
  Document,
  CollectionTag,
  Clock,
  Connection
} from '@element-plus/icons-vue'
import recommendationService from '@/services/recommendation'
import type { SmartRecommendation } from '@/types'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const loading = ref(false)
const generating = ref(false)
const cleaning = ref(false)
const analyticsLoading = ref(false)

const list = ref<SmartRecommendation[]>([])
const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

const analytics = reactive({
  totalRecommendations: 0,
  viewedRecommendations: 0,
  adoptedRecommendations: 0,
  viewRate: 0,
  adoptionRate: 0
})

const viewedCount = computed(() => list.value.filter((item) => item.isViewed).length)
const adoptedCount = computed(() => list.value.filter((item) => item.isAdopted).length)
const pendingCount = computed(() => list.value.filter((item) => !item.isViewed && !item.isAdopted).length)
const currentTypeMix = computed(() => {
  const map = new Map<string, number>()
  for (const item of list.value) {
    const key = recommendationTypeLabel(item.recommendationType)
    map.set(key, (map.get(key) || 0) + 1)
  }
  return Array.from(map.entries()).slice(0, 4)
})

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatScore = (value?: number) => `${Math.round((value || 0) * 100)} 分`

const recommendationTypeLabel = (value?: string) => {
  const map: Record<string, string> = {
    FILE: '文件',
    FOLDER: '文件夹',
    TAG: '标签',
    COLLABORATION: '协作',
    SEARCH_RESULT: '搜索结果'
  }
  return map[value || ''] || value || '--'
}

const sourceTypeLabel = (value?: string) => {
  const map: Record<string, string> = {
    AI_MODEL: '模型推断',
    USER_BEHAVIOR: '行为分析',
    COLLABORATION_PATTERN: '协作模式',
    CONTENT_SIMILARITY: '内容相似'
  }
  return map[value || ''] || value || '--'
}

const loadAnalytics = async () => {
  analyticsLoading.value = true
  try {
    const data = await recommendationService.getAnalytics()
    Object.assign(analytics, data)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载推荐分析失败'
    ElMessage.error(message)
  } finally {
    analyticsLoading.value = false
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const result = await recommendationService.getRecommendations(pagination.page, pagination.size)
    list.value = result.content || []
    pagination.total = Number(result.totalElements || 0)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载推荐列表失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadList(), loadAnalytics()])
}

const generateRecommendations = async () => {
  generating.value = true
  try {
    await recommendationService.generateRecommendations()
    ElMessage.success('已生成新的推荐内容')
    await refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '生成推荐失败'
    ElMessage.error(message)
  } finally {
    generating.value = false
  }
}

const cleanupRecommendations = async () => {
  cleaning.value = true
  try {
    const result = await recommendationService.cleanupExpiredRecommendations()
    ElMessage.success(result?.message || '已清理过期推荐')
    await refreshAll()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '清理过期推荐失败'
    ElMessage.error(message)
  } finally {
    cleaning.value = false
  }
}

const markViewed = async (item: SmartRecommendation) => {
  try {
    await recommendationService.markAsViewed(item.id)
    item.isViewed = true
    ElMessage.success('已标记为已查看')
    await loadAnalytics()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '标记查看失败'
    ElMessage.error(message)
  }
}

const markAdopted = async (item: SmartRecommendation) => {
  try {
    await recommendationService.markAsAdopted(item.id)
    item.isAdopted = true
    ElMessage.success('已标记为已采纳')
    await loadAnalytics()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '标记采纳失败'
    ElMessage.error(message)
  }
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
  <div class="modern-recommend-page">
    <!-- 顶部控制台 -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Intelligence</span>
          <h1 class="title">推荐中心</h1>
          <p class="subtitle">基于用户行为、内容关联与协作模式的实时智能推荐引擎。</p>
        </div>
        <div class="header-actions">
          <el-button color="#6366f1" type="primary" size="large" :loading="generating" @click="generateRecommendations" round>
            <el-icon class="el-icon--left"><MagicStick /></el-icon>生成推荐
          </el-button>
          <el-button size="large" :loading="cleaning" @click="cleanupRecommendations" round>
            <el-icon class="el-icon--left"><Delete /></el-icon>清理过期
          </el-button>
          <el-button size="large" @click="refreshAll" round>
            <el-icon class="el-icon--left"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </div>
      
      <div class="header-stats-strip" v-loading="analyticsLoading">
        <UiStatCard label="总推荐项" :value="analytics.totalRecommendations" />
        <el-divider direction="vertical" />
        <UiStatCard label="查看转化率" :value="`${analytics.viewRate || 0}%`" />
        <el-divider direction="vertical" />
        <UiStatCard label="采纳转化率" :value="`${analytics.adoptionRate || 0}%`" />
      </div>
    </header>

    <!-- 工作区 -->
    <div class="workspace-layout">
      <!-- 左栏：列表 -->
      <main class="list-column">
        <div class="summary-bar">
          <div class="summary-pill"><span>待处理</span><strong>{{ pendingCount }}</strong></div>
          <div class="summary-pill"><span>已查看</span><strong>{{ viewedCount }}</strong></div>
          <div class="summary-pill"><span>已采纳</span><strong>{{ adoptedCount }}</strong></div>
        </div>

        <div class="recommendation-list" v-loading="loading">
          <article v-for="item in list" :key="item.id" class="modern-card">
            <div class="card-top">
              <el-tag effect="light" type="primary" round>{{ recommendationTypeLabel(item.recommendationType) }}</el-tag>
              <div class="score-pill"><el-icon><Trophy /></el-icon> {{ formatScore(item.relevanceScore) }}</div>
            </div>

            <div class="card-body">
              <h3 class="item-reason">{{ item.reason }}</h3>
              <div class="meta-strip">
                <span><el-icon><Connection /></el-icon>{{ sourceTypeLabel(item.sourceType) }}</span>
                <span><el-icon><Clock /></el-icon>{{ formatDateTime(item.expireAt) }} 到期</span>
              </div>
            </div>

            <div class="card-footer">
              <div class="tags-group">
                <el-tag size="small" :type="item.isViewed ? 'success' : 'info'" round>{{ item.isViewed ? '已查看' : '未查看' }}</el-tag>
                <el-tag size="small" :type="item.isAdopted ? 'warning' : 'info'" round>{{ item.isAdopted ? '已采纳' : '未采纳' }}</el-tag>
              </div>
              <div class="action-group">
                <el-button size="small" :disabled="item.isViewed" @click="markViewed(item)" round>查看</el-button>
                <el-button type="primary" color="#6366f1" size="small" :disabled="item.isAdopted" @click="markAdopted(item)" round>采纳</el-button>
              </div>
            </div>
          </article>
        </div>

        <div class="pagination-wrapper">
          <el-pagination background layout="total, prev, pager, next" :current-page="pagination.page + 1" :page-size="pagination.size" :total="pagination.total" @current-change="onPageChange" />
        </div>
      </main>

      <!-- 右栏：分析 -->
      <aside class="side-column">
        <div class="base-card widget">
          <h2 class="widget-title">推荐类型分布</h2>
          <div v-if="currentTypeMix.length" class="type-mix">
            <div v-for="[label, count] in currentTypeMix" :key="label" class="mix-item">
              <span>{{ label }}</span>
              <strong>{{ count }} 项</strong>
            </div>
          </div>
          <el-empty v-else description="暂无分布数据" :image-size="60" />
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.modern-recommend-page {
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

.app-header { padding: 32px; }
.header-main { display: flex; justify-content: space-between; align-items: start; margin-bottom: 24px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; }
.title { font-size: 28px; font-weight: 800; color: #0f172a; margin: 12px 0; }
.subtitle { color: #64748b; font-size: 14px; }
.header-stats-strip { display: flex; gap: 32px; border-top: 1px solid #e2e8f0; padding-top: 24px; }

.workspace-layout { display: grid; grid-template-columns: 1fr 320px; gap: 24px; align-items: start; }

.summary-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.summary-pill { background: #fff; padding: 12px 20px; border-radius: 12px; border: 1px solid #e2e8f0; display: flex; gap: 8px; align-items: center; }
.summary-pill strong { color: var(--primary-color); font-size: 18px; }

.modern-card {
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  margin-bottom: 16px;
  transition: transform 0.2s;
}
.modern-card:hover { transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); }

.card-top { display: flex; justify-content: space-between; align-items: center; }
.score-pill { font-weight: 700; color: #b45309; background: #fffbeb; padding: 4px 12px; border-radius: 20px; font-size: 13px; }
.item-reason { font-size: 16px; color: #1e293b; margin: 16px 0; font-weight: 600; }
.meta-strip { display: flex; gap: 16px; font-size: 12px; color: #64748b; margin-bottom: 20px; }
.card-footer { display: flex; justify-content: space-between; align-items: center; }

.widget { padding: 24px; }
.widget-title { font-size: 16px; font-weight: 700; margin: 0 0 16px 0; }
.mix-item { display: flex; justify-content: space-between; padding: 10px 0; }
</style>