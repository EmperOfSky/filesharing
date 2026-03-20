<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Unlock, Download, Key, Link } from '@element-plus/icons-vue'
import fileService from '@/services/fileService'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const route = useRoute()
const shareKey = String(route.params.shareKey || '')

const loading = ref(false)
const accessing = ref(false)
const downloading = ref(false)
const shareInfo = ref<any | null>(null)
const accessToken = ref('')
const password = ref('')

const shareStatus = computed(() => shareInfo.value?.status || '--')
const canDownload = computed(() => Boolean(accessToken.value && shareInfo.value?.allowDownload && shareInfo.value?.shareType === 'FILE'))

const loadShareInfo = async () => {
  if (!shareKey) {
    ElMessage.error('分享链接无效')
    return
  }

  loading.value = true
  try {
    shareInfo.value = await fileService.getSharedFile(shareKey)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '获取分享信息失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const authorizeShare = async () => {
  accessing.value = true
  try {
    const data = await fileService.accessSharedFile(shareKey, password.value)
    accessToken.value = data.accessToken
    shareInfo.value = data.share
    ElMessage.success('授权成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '授权失败'
    ElMessage.error(message)
  } finally {
    accessing.value = false
  }
}

const downloadFile = async () => {
  if (!accessToken.value) {
    ElMessage.warning('请先完成访问授权')
    return
  }

  downloading.value = true
  try {
    const blob = await fileService.downloadSharedFile(shareKey, accessToken.value)
    const url = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = shareInfo.value?.sharedContent?.name || `shared-${shareKey}`
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '下载失败'
    ElMessage.error(message)
  } finally {
    downloading.value = false
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '永久有效'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

onMounted(() => {
  loadShareInfo()
})
</script>

<template>
  <div class="modern-share-page" v-loading="loading">
    <div class="page-wrapper">
      
      <!-- 顶部 Hero -->
      <header class="app-header base-card">
        <div class="header-main">
          <div class="header-intro">
            <span class="kicker-tag">Shared Content</span>
            <h1 class="title">{{ shareInfo?.title || '外链访问' }}</h1>
            <p class="subtitle">通过云端高速中转，获取您需要的内容。</p>
          </div>
          
          <div class="status-strip">
            <div class="stat-item">
              <span class="label">有效期</span>
              <span class="val">{{ formatDateTime(shareInfo?.expireTime) }}</span>
            </div>
            <div class="stat-item">
              <span class="label">权限</span>
              <span class="val">{{ shareInfo?.allowDownload ? '允许下载' : '禁止下载' }}</span>
            </div>
            <div class="stat-item">
              <span class="label">状态</span>
              <el-tag :type="shareStatus === 'ACTIVE' ? 'success' : 'info'" round size="small">{{ shareStatus }}</el-tag>
            </div>
          </div>
        </div>
      </header>

      <!-- 双栏布局 -->
      <div v-if="shareInfo" class="workspace-layout">
        <!-- 左栏：文件详情 -->
        <main class="content-column base-card">
          <div class="panel-header">
            <h2 class="widget-title"><el-icon><Link /></el-icon> 文件详情</h2>
          </div>
          <div class="info-grid">
            <div class="info-card">
              <span class="label">文件名称</span>
              <strong class="value">{{ shareInfo.sharedContent?.name || '--' }}</strong>
            </div>
            <div class="info-card">
              <span class="label">内容类型</span>
              <strong class="value">{{ shareInfo.shareType || '--' }}</strong>
            </div>
            <div class="info-card">
              <span class="label">分享人</span>
              <strong class="value">{{ shareInfo.sharerName || '--' }}</strong>
            </div>
            <div class="info-card">
              <span class="label">标识符 (Share Key)</span>
              <strong class="value mono">{{ shareKey }}</strong>
            </div>
          </div>
        </main>

        <!-- 右栏：授权控制台 -->
        <aside class="side-column">
          <!-- Auth Section -->
          <div class="base-card widget" :class="{ 'is-authorized': accessToken }">
            <h2 class="widget-title">访问授权</h2>
            <p class="widget-subtitle">{{ accessToken ? '验证通过，可以进行下载' : '需提取码解密内容' }}</p>
            
            <div class="auth-control">
              <el-input
                v-if="!accessToken"
                v-model="password"
                size="large"
                type="password"
                show-password
                placeholder="请输入提取密码..."
                @keyup.enter="authorizeShare"
                class="modern-input"
              />
              <el-button 
                class="action-btn" 
                :type="accessToken ? 'success' : 'primary'" 
                :color="!accessToken ? '#6366f1' : undefined" 
                size="large" 
                :loading="accessing" 
                @click="authorizeShare" 
                round
              >
                <el-icon class="el-icon--left">
                  <component :is="accessToken ? Unlock : Key" />
                </el-icon>
                {{ accessToken ? '已授权' : '确认提取' }}
              </el-button>
            </div>
          </div>

          <!-- Download Section -->
          <div class="base-card widget" :class="{ 'ready': canDownload }">
            <h2 class="widget-title">文件提取</h2>
            <el-button 
              class="action-btn" 
              size="large" 
              :type="canDownload ? 'primary' : 'info'" 
              :disabled="!canDownload" 
              :loading="downloading" 
              @click="downloadFile" 
              round
            >
              <el-icon class="el-icon--left"><Download /></el-icon> 
              {{ canDownload ? '立即下载' : '等待授权' }}
            </el-button>
          </div>
        </aside>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="base-card empty-state">
        <el-empty description="该分享链接不存在或已失效" :image-size="120" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.modern-share-page {
  --primary-color: #6366f1;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --text-main: #0f172a;
  --text-muted: #64748b;
  --border-color: #e2e8f0;
  
  background-color: var(--bg-page);
  min-height: 100vh;
  padding: 40px 24px;
  display: flex;
  justify-content: center;
}

.page-wrapper { width: 100%; max-width: 1000px; display: flex; flex-direction: column; gap: 24px; }
.base-card { background: var(--bg-card); border-radius: 20px; border: 1px solid var(--border-color); padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.03); }

/* Header */
.header-main { margin-bottom: 24px; }
.kicker-tag { font-size: 13px; font-weight: 600; color: var(--primary-color); background: #e0e7ff; padding: 4px 12px; border-radius: 20px; margin-bottom: 12px; display: inline-block;}
.title { font-size: 32px; font-weight: 800; color: var(--text-main); margin: 0; }
.subtitle { color: var(--text-muted); margin: 8px 0 0 0; }
.status-strip { display: flex; gap: 24px; margin-top: 24px; padding: 16px; background: #f8fafc; border-radius: 12px; }
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-item .label { font-size: 12px; color: var(--text-muted); }
.stat-item .val { font-size: 14px; font-weight: 600; color: var(--text-main); }

/* Layout */
.workspace-layout { display: grid; grid-template-columns: 1fr 340px; gap: 24px; }

/* Info Grid */
.panel-header { margin-bottom: 20px; }
.widget-title { font-size: 18px; font-weight: 700; display: flex; align-items: center; gap: 8px; margin: 0; }
.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.info-card { background: #f8fafc; padding: 16px; border-radius: 12px; display: flex; flex-direction: column; gap: 4px; }
.info-card .label { font-size: 12px; color: var(--text-muted); }
.info-card .value { font-size: 15px; color: var(--text-main); font-weight: 600; }
.mono { font-family: monospace; color: var(--primary-color); }

/* Sidebar Widgets */
.widget { padding: 24px; transition: all 0.3s; }
.widget.is-authorized { border-color: #86efac; background: #f0fdf4; }
.widget.ready { border-color: #86efac; }
.widget-subtitle { font-size: 13px; color: var(--text-muted); margin: 4px 0 20px 0; }

.auth-control { display: flex; flex-direction: column; gap: 12px; }
.modern-input :deep(.el-input__wrapper) { border-radius: 10px; }
.action-btn { width: 100%; font-weight: 600; }

@media (max-width: 850px) {
  .workspace-layout { grid-template-columns: 1fr; }
}
</style>