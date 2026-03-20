<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import collaborationService from '@/services/collaboration'
import type { CollaborationProject } from '@/types/collaboration'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const statsLoading = ref(false)
const dialogVisible = ref(false)

const list = ref<CollaborationProject[]>([])
const searchKeyword = ref('')

const pagination = reactive({
  page: 0,
  size: 9,
  total: 0
})

const userStats = reactive({
  totalProjects: 0,
  totalDocuments: 0,
  totalComments: 0,
  projectsOwned: 0,
  projectsParticipated: 0
})

const realtimeStats = reactive({
  activeSessions: 0,
  totalCollaborators: 0,
  documentsBeingEdited: 0,
  averageCollaboratorsPerDocument: 0
})

const createForm = reactive({
  projectName: '',
  description: '',
  tags: ''
})

const activeProjects = computed(() => list.value.filter((item) => (item.status || 'ACTIVE') !== 'ARCHIVED').length)
const ownedRate = computed(() => {
  if (!userStats.totalProjects) return 0
  return Math.round((userStats.projectsOwned / userStats.totalProjects) * 100)
})
const hotProjects = computed(() =>
  [...list.value]
    .sort((a, b) => Number(b.memberCount || 0) - Number(a.memberCount || 0))
    .slice(0, 4)
)

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const openProject = (projectId: number) => {
  router.push(`/dashboard/collaboration/${projectId}`)
}

const loadProjects = async () => {
  loading.value = true
  try {
    const result = searchKeyword.value.trim()
      ? await collaborationService.searchProjects(searchKeyword.value.trim(), pagination.page, pagination.size)
      : await collaborationService.getMyProjects(pagination.page, pagination.size)
    list.value = result.content ||[]
    pagination.total = Number(result.totalElements || 0)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载协作项目失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const [mine, realtime] = await Promise.all([
      collaborationService.getMyStatistics(),
      collaborationService.getRealtimeStatistics()
    ])
    Object.assign(userStats, mine)
    Object.assign(realtimeStats, realtime)
  } catch {
    // 指标失败不阻断页面
  } finally {
    statsLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadProjects(), loadStats()])
}

const createProject = async () => {
  if (!createForm.projectName.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }

  creating.value = true
  try {
    const result = await collaborationService.createProject({
      projectName: createForm.projectName.trim(),
      description: createForm.description.trim() || undefined,
      tags: createForm.tags.trim() || undefined
    })
    ElMessage.success('协作项目已创建')
    dialogVisible.value = false
    createForm.projectName = ''
    createForm.description = ''
    createForm.tags = ''
    await refreshAll()
    if (result.id) openProject(result.id)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '创建协作项目失败'
    ElMessage.error(message)
  } finally {
    creating.value = false
  }
}

const deleteProject = async (project: CollaborationProject) => {
  try {
    await ElMessageBox.confirm(`确认删除项目 ${project.projectName} 吗？`, '删除项目', { type: 'warning' })
    await collaborationService.deleteProject(project.id)
    ElMessage.success('项目已删除')
    await refreshAll()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const message = error?.response?.data?.message || error?.message || '删除项目失败'
    ElMessage.error(message)
  }
}

const onPageChange = (page: number) => {
  pagination.page = page - 1
  loadProjects()
}

const onSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 0
  loadProjects()
}

const resetSearch = () => {
  searchKeyword.value = ''
  pagination.page = 0
  loadProjects()
}

onMounted(() => {
  refreshAll()
})
</script>

<template>
  <div class="modern-collab-workspace">
    <!-- 顶部 Dashboard Hero -->
    <header class="base-card welcome-hero" v-loading="statsLoading">
      <div class="hero-content">
        <div class="hero-text">
          <span class="kicker-tag">Collaboration Hub</span>
          <h1 class="greeting">团队协作空间</h1>
          <p class="subtitle">实时编辑文档、管理多人协作项目，构建属于团队的知识库。</p>
          <div class="hero-actions">
            <el-button color="#6366f1" type="primary" size="large" @click="dialogVisible = true" round>
              <el-icon class="el-icon--left"><Plus /></el-icon>新建项目
            </el-button>
            <el-button size="large" @click="refreshAll" round plain>
              <el-icon class="el-icon--left"><Refresh /></el-icon>刷新数据
            </el-button>
          </div>
        </div>

        <div class="hero-stats">
          <UiStatCard label="项目总量" :value="userStats.totalProjects" />
          <UiStatCard label="文档总量" :value="userStats.totalDocuments" />
          <UiStatCard label="在线会话" :value="realtimeStats.activeSessions" />
          <UiStatCard label="在线协作者" :value="realtimeStats.totalCollaborators" />
        </div>
      </div>
    </header>

    <!-- 主体双栏布局 -->
    <div class="workspace-layout">
      <!-- 左侧：项目主视图 -->
      <main class="main-content">
        <!-- 控制栏与次级统计 -->
        <div class="main-toolbar">
          <div class="search-wrap">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索项目名称、描述或标签..."
              clearable
              class="modern-search"
              @keyup.enter="pagination.page = 0; loadProjects()"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" color="#6366f1" plain @click="pagination.page = 0; loadProjects()" round>
              搜索
            </el-button>
            <el-button @click="resetSearch" round>重置</el-button>
          </div>

          <!-- 次级统计指标 -->
          <div class="sub-stats-bar">
            <div class="stat-pill"><span class="label">当前页</span><span class="val">{{ list.length }}</span></div>
            <div class="divider"></div>
            <div class="stat-pill"><span class="label">活跃项目</span><span class="val">{{ activeProjects }}</span></div>
            <div class="divider"></div>
            <div class="stat-pill"><span class="label">我创建的</span><span class="val">{{ userStats.projectsOwned }}</span></div>
            <div class="divider"></div>
            <div class="stat-pill"><span class="label">拥有占比</span><span class="val">{{ ownedRate }}%</span></div>
          </div>
        </div>

        <!-- 项目卡片网格 -->
        <section v-if="list.length" class="project-grid" v-loading="loading">
          <article v-for="project in list" :key="project.id" class="modern-project-card">
            <div class="card-header">
              <div class="status-badge" :class="project.status === 'ARCHIVED' ? 'is-archived' : 'is-active'">
                <span class="dot"></span>
                {{ project.status || 'ACTIVE' }}
              </div>
              <div class="member-badge">
                <el-icon><User /></el-icon> {{ project.memberCount || 0 }}
              </div>
            </div>

            <div class="card-body">
              <h3 class="project-title" @click="openProject(project.id)">{{ project.projectName }}</h3>
              <p class="project-desc">{{ project.description || '暂无描述信息' }}</p>
              <div class="tags-wrap" v-if="project.tags">
                <span class="tag-pill">{{ project.tags }}</span>
              </div>
            </div>

            <div class="card-meta">
              <div class="meta-item">
                <div class="meta-val">{{ project.documentCount || 0 }}</div>
                <div class="meta-label">包含文档</div>
              </div>
              <div class="meta-item">
                <div class="meta-val">{{ project.memberCount || 0 }}</div>
                <div class="meta-label">协作成员</div>
              </div>
            </div>

            <div class="card-footer">
              <span class="update-time">更新于 {{ formatDateTime(project.updatedAt || project.createdAt) }}</span>
              <div class="action-buttons">
                <el-tooltip content="删除项目" placement="top">
                  <el-button circle size="small" type="danger" plain @click="deleteProject(project)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button type="primary" color="#6366f1" size="small" round @click="openProject(project.id)">
                  进入工作区
                </el-button>
              </div>
            </div>
          </article>
        </section>

        <!-- 空状态 -->
        <section v-else class="empty-state-wrap base-card">
          <el-empty description="没有找到匹配的协作项目" image-size="160">
            <el-button type="primary" color="#6366f1" round @click="dialogVisible = true">立即创建</el-button>
          </el-empty>
        </section>

        <!-- 分页 -->
        <div class="pagination-wrap" v-if="pagination.total > 0">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :current-page="pagination.page + 1"
            :page-sizes="[9, 18, 36]"
            :page-size="pagination.size"
            :total="pagination.total"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </main>

      <!-- 右侧：协作脉冲 & 热门项目 -->
      <aside class="sidebar">
        <!-- Widget: 协作脉冲 -->
        <section class="widget-card base-card">
          <div class="widget-header">
            <h2 class="widget-title">协作脉冲</h2>
            <el-icon class="widget-icon"><DataLine /></el-icon>
          </div>
          <div class="pulse-grid">
            <div class="pulse-item">
              <div class="val">{{ realtimeStats.documentsBeingEdited }}</div>
              <div class="label">实时编辑文档</div>
            </div>
            <div class="pulse-item">
              <div class="val">{{ realtimeStats.averageCollaboratorsPerDocument || 0 }}</div>
              <div class="label">平均协作者/篇</div>
            </div>
            <div class="pulse-item">
              <div class="val">{{ userStats.totalComments }}</div>
              <div class="label">累计互动评论</div>
            </div>
            <div class="pulse-item">
              <div class="val">{{ userStats.projectsParticipated }}</div>
              <div class="label">参与协作项目</div>
            </div>
          </div>
        </section>

        <!-- Widget: 热门项目 -->
        <section class="widget-card base-card">
          <div class="widget-header">
            <h2 class="widget-title">热门活跃项目</h2>
            <el-icon class="widget-icon hot-icon"><Discount /></el-icon>
          </div>
          
          <div v-if="hotProjects.length" class="hot-list">
            <div v-for="(project, index) in hotProjects" :key="project.id" class="hot-item" @click="openProject(project.id)">
              <div class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
              <div class="hot-info">
                <h4 class="hot-name">{{ project.projectName }}</h4>
                <div class="hot-meta">{{ project.memberCount || 0 }} 成员 · {{ project.documentCount || 0 }} 文档</div>
              </div>
              <el-icon class="hot-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          
          <div v-else class="hot-empty">
            <span class="text-muted">暂无热门数据</span>
          </div>
        </section>
      </aside>
    </div>

    <!-- 新建项目弹窗 -->
    <el-dialog v-model="dialogVisible" title="✨ 创建新的协作项目" width="560px" class="modern-dialog" destroy-on-close>
      <div class="dialog-subtitle">设立一个共享空间，与您的团队成员共同编写和管理文档。</div>
      <el-form label-position="top" class="modern-form">
        <el-form-item label="项目名称">
          <el-input v-model="createForm.projectName" placeholder="例如：Q4 产品增长计划" size="large" />
        </el-form-item>
        <el-form-item label="项目说明 (可选)">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="简要描述该项目的目标和主要产出内容..." resize="none" />
        </el-form-item>
        <el-form-item label="分类标签 (可选)">
          <el-input v-model="createForm.tags" placeholder="例如：产品, 规划, 周报" size="large">
            <template #prefix><el-icon><CollectionTag /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" round>取消</el-button>
          <el-button type="primary" color="#6366f1" :loading="creating" @click="createProject" round>
            确认创建
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 全局变量 */
.modern-collab-workspace {
  --primary-color: #6366f1; /* 靛蓝色主题 */
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
  border-radius: 20px;
  box-shadow: 0 4px 24px -4px rgba(0, 0, 0, 0.03), 0 2px 8px -2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

/* Hero Section */
.welcome-hero {
  padding: 40px;
}
.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}
.hero-text {
  flex: 1;
  max-width: 500px;
}
.kicker-tag {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 12px;
}
.greeting {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}
.subtitle {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.6;
  margin: 0 0 24px 0;
}
.hero-actions {
  display: flex;
  gap: 12px;
}
.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-width: 320px;
}

/* 主体布局 */
.workspace-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}
.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 工具栏与次级统计 */
.main-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  background: var(--bg-card);
  padding: 16px 24px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
}
.search-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 300px;
}
.modern-search {
  max-width: 320px;
}
:deep(.modern-search .el-input__wrapper) {
  border-radius: 20px;
  background: #f8fafc;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.3s;
}
:deep(.modern-search .el-input__wrapper.is-focus) {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light) !important;
}

.sub-stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-left: 16px;
  border-left: 1px solid var(--border-color);
}
.stat-pill {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.stat-pill .label { font-size: 13px; color: var(--text-muted); }
.stat-pill .val { font-size: 16px; font-weight: 700; color: var(--text-main); }
.divider { width: 4px; height: 4px; border-radius: 50%; background: #cbd5e1; }

/* 项目卡片网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.modern-project-card {
  background: var(--bg-card);
  border-radius: 20px;
  border: 1px solid var(--border-color);
  padding: 24px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.modern-project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

/* 卡片头部：状态与人数 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}
.status-badge.is-active { background: #ecfdf5; color: #10b981; }
.status-badge.is-active .dot { background: #10b981; }
.status-badge.is-archived { background: #f1f5f9; color: #64748b; }
.status-badge.is-archived .dot { background: #64748b; }
.dot { width: 6px; height: 6px; border-radius: 50%; }

.member-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
  background: #f8fafc;
  padding: 4px 8px;
  border-radius: 8px;
}

/* 卡片主体：标题与描述 */
.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.project-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
  cursor: pointer;
  transition: color 0.2s;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.project-title:hover {
  color: var(--primary-color);
}
.project-desc {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}
.tags-wrap {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag-pill {
  font-size: 12px;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 2px 8px;
  border-radius: 6px;
}

/* 卡片统计（文档与成员） */
.card-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 20px 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 12px;
}
.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.meta-val {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}
.meta-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 卡片底部：时间与操作 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
.update-time {
  font-size: 12px;
  color: #94a3b8;
}
.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 空状态与分页 */
.empty-state-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

/* 侧边栏 Widgets */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.widget-card {
  padding: 24px;
}
.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.widget-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: var(--text-main);
}
.widget-icon {
  font-size: 18px;
  color: var(--text-muted);
}
.hot-icon { color: #f59e0b; }

/* 脉冲数据 Grid */
.pulse-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.pulse-item {
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pulse-item .val {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary-color);
}
.pulse-item .label {
  font-size: 12px;
  color: var(--text-muted);
}

/* 热门项目列表 */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.hot-item:hover {
  background: #f8fafc;
}
.hot-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  background: #e2e8f0;
  border-radius: 6px;
}
.hot-rank.rank-1 { background: #fee2e2; color: #ef4444; }
.hot-rank.rank-2 { background: #ffedd5; color: #f97316; }
.hot-rank.rank-3 { background: #fef3c7; color: #f59e0b; }

.hot-info {
  flex: 1;
  min-width: 0;
}
.hot-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hot-meta {
  font-size: 12px;
  color: var(--text-muted);
}
.hot-arrow {
  color: #cbd5e1;
  transition: transform 0.2s;
}
.hot-item:hover .hot-arrow {
  transform: translateX(2px);
  color: var(--primary-color);
}
.hot-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
}

/* 弹窗及表单优化 */
:deep(.modern-dialog) {
  border-radius: 20px;
  overflow: hidden;
}
:deep(.modern-dialog .el-dialog__header) {
  background: #f8fafc;
  margin-right: 0;
  padding: 24px;
  border-bottom: 1px solid var(--border-color);
}
:deep(.modern-dialog .el-dialog__title) {
  font-weight: 700;
  font-size: 18px;
}
:deep(.modern-dialog .el-dialog__body) {
  padding: 24px;
}
.dialog-subtitle {
  margin-bottom: 24px;
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* 响应式适配 */
@media (max-width: 1120px) {
  .workspace-layout {
    grid-template-columns: 1fr;
  }
  .sub-stats-bar {
    border-left: none;
    padding-left: 0;
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-stats {
    width: 100%;
    grid-template-columns: 1fr 1fr;
  }
  .main-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-wrap {
    flex-wrap: wrap;
  }
  .pulse-grid {
    grid-template-columns: 1fr;
  }
}
</style>