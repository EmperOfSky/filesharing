<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import fileCodeBoxService from '@/services/fileCodeBox'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  open_upload: true,
  upload_size: 0,
  upload_count: 0,
  upload_minute: 0,
  error_count: 0,
  error_minute: 0,
  max_save_seconds: 0,
  expire_styles: [] as string[],
  presign_expire_seconds: 0,
  download_token_ttl_seconds: 0
})

const expireStyleOptions =[
  { label: '按分钟', value: 'minute' },
  { label: '按小时', value: 'hour' },
  { label: '按天', value: 'day' },
  { label: '按次数', value: 'count' },
  { label: '永久', value: 'forever' }
]

const uploadSizeMb = computed(() => `${Math.max(form.upload_size / 1024 / 1024, 0).toFixed(0)} MB`)
const maxSaveDays = computed(() => `${Math.max(form.max_save_seconds / 86400, 0).toFixed(1)} 天`)
const presignMinutes = computed(() => `${Math.max(form.presign_expire_seconds / 60, 0).toFixed(0)} 分钟`)
const tokenMinutes = computed(() => `${Math.max(form.download_token_ttl_seconds / 60, 0).toFixed(0)} 分钟`)

const loadConfig = async () => {
  loading.value = true
  try {
    const data = await fileCodeBoxService.getAdminConfig()
    Object.assign(form, data)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载快传配置失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    const payload = {
      ...form,
      upload_size: Number(form.upload_size),
      upload_count: Number(form.upload_count),
      upload_minute: Number(form.upload_minute),
      error_count: Number(form.error_count),
      error_minute: Number(form.error_minute),
      max_save_seconds: Number(form.max_save_seconds),
      presign_expire_seconds: Number(form.presign_expire_seconds),
      download_token_ttl_seconds: Number(form.download_token_ttl_seconds)
    }
    const data = await fileCodeBoxService.updateAdminConfig(payload)
    Object.assign(form, data)
    ElMessage.success('快传配置已更新')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '保存快传配置失败'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="modern-settings-page" v-loading="loading">
    <!-- 顶部 Dashboard Header -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Transfer Policy</span>
          <h1 class="page-title">全局快传配置策略</h1>
          <p class="page-description">
            统一维护上传开关、体积限制、有效期策略、预签名时效与下载令牌寿命，确保快传入口具备清晰的治理边界与安全风控。
          </p>
          <div class="hero-badges">
            <span class="modern-badge" :class="form.open_upload ? 'is-success' : 'is-warning'">
              <span class="dot"></span>上传通道 {{ form.open_upload ? '已开启' : '已关闭' }}
            </span>
            <span class="modern-badge is-info">
              <el-icon><Timer /></el-icon>
              支持 {{ form.expire_styles.length }} 种过期策略
            </span>
          </div>
        </div>
        <div class="header-actions">
          <el-button size="large" @click="loadConfig" round plain>
            <el-icon class="el-icon--left"><Refresh /></el-icon>丢弃更改
          </el-button>
          <el-button type="primary" color="#6366f1" size="large" :loading="saving" @click="saveConfig" round>
            <el-icon class="el-icon--left"><Check /></el-icon>保存配置
          </el-button>
        </div>
      </div>

      <div class="header-stats-strip">
        <UiStatCard label="单次大小上限" :value="uploadSizeMb" hint="单个文件入口门槛" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="保存上限" :value="maxSaveDays" hint="最大占用存储时长" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="预签名有效期" :value="presignMinutes" hint="直传窗口暴露时间" />
        <el-divider direction="vertical" class="hidden-mobile" />
        <UiStatCard label="下载令牌寿命" :value="tokenMinutes" hint="下载地址有效寿命" />
      </div>
    </header>

    <!-- 策略总览网格 -->
    <section class="overview-grid">
      <UiStatCard label="频控阈值" :value="`${form.upload_count} 次 / ${form.upload_minute} 分钟`" hint="每个观察窗口内允许的上传次数" class="stat-card" />
      <UiStatCard label="异常风控阈值" :value="`${form.error_count} 次 / ${form.error_minute} 分钟`" hint="触发保护的错误次数与观察窗口" class="stat-card" />
      <UiStatCard label="当前策略状态" :value="form.open_upload ? '完全开放' : '收紧/维护'" hint="适合在活动期与风控期切换" class="stat-card" />
      <UiStatCard label="支持的过期策略" :value="form.expire_styles.join(' / ') || '--'" hint="前端选项将按该集合展示" class="stat-card" />
    </section>

    <!-- 核心表单区域：两栏布局 -->
    <div class="settings-layout">
      <!-- 左侧：主干配置 -->
      <section class="settings-main">
        <article class="settings-card base-card">
          <div class="card-header">
            <div class="header-icon"><el-icon><Switch /></el-icon></div>
            <div>
              <h2 class="card-title">入口与限流</h2>
              <p class="card-subtitle">先定义系统是否开放，再决定每次上传可以消耗多少系统资源。</p>
            </div>
          </div>

          <el-form class="modern-form" label-position="top">
            <div class="form-row flex-row">
              <div class="row-info">
                <label>开放快传上传</label>
                <span>关闭后，前端将不再接受任何新的文件上传请求。</span>
              </div>
              <div class="row-control">
                <el-switch v-model="form.open_upload" style="--el-switch-on-color: #10b981;" />
              </div>
            </div>

            <div class="form-row">
              <div class="row-info">
                <label>单文件大小上限 (Bytes)</label>
                <span>决定了单次上传最大能吞吐的文件体积，当前约为 <strong>{{ uploadSizeMb }}</strong>。</span>
              </div>
              <div class="row-control mt-3">
                <el-input-number v-model="form.upload_size" :min="0" :step="1048576" controls-position="right" class="full-width-input" />
              </div>
            </div>

            <div class="form-row split-col">
              <div class="sub-col">
                <div class="row-info"><label>上传次数限制</label></div>
                <el-input-number v-model="form.upload_count" :min="0" controls-position="right" class="full-width-input mt-2" />
              </div>
              <div class="sub-col">
                <div class="row-info"><label>限制统计窗口 (分钟)</label></div>
                <el-input-number v-model="form.upload_minute" :min="0" controls-position="right" class="full-width-input mt-2" />
              </div>
            </div>

            <div class="form-row last-row">
              <div class="row-info">
                <label>允许的过期策略选项</label>
                <span>前端创建分享时可供用户选择的有效期类型。</span>
              </div>
              <div class="row-control mt-3">
                <el-checkbox-group v-model="form.expire_styles" class="modern-checkbox-group">
                  <el-checkbox-button v-for="item in expireStyleOptions" :key="item.value" :label="item.value">
                    {{ item.label }}
                  </el-checkbox-button>
                </el-checkbox-group>
              </div>
            </div>
          </el-form>
        </article>
      </section>

      <!-- 右侧：时效与风控 -->
      <aside class="settings-sidebar">
        <!-- 存储时效 -->
        <article class="settings-card base-card">
          <div class="card-header compact">
            <div class="header-icon tone-blue"><el-icon><Clock /></el-icon></div>
            <div>
              <h2 class="card-title">生命周期与时效</h2>
              <p class="card-subtitle">控制存储驻留及安全凭证的暴露时间。</p>
            </div>
          </div>
          <el-form class="modern-form" label-position="top">
            <div class="form-row">
              <div class="row-info">
                <label>最长保存时间 (秒)</label>
                <span>硬性兜底机制，当前约为 <strong>{{ maxSaveDays }}</strong>。</span>
              </div>
              <el-input-number v-model="form.max_save_seconds" :min="0" :step="86400" controls-position="right" class="full-width-input mt-2" />
            </div>

            <div class="form-row">
              <div class="row-info">
                <label>预签名上传有效窗口 (秒)</label>
              </div>
              <el-input-number v-model="form.presign_expire_seconds" :min="0" :step="60" controls-position="right" class="full-width-input mt-2" />
            </div>

            <div class="form-row last-row">
              <div class="row-info">
                <label>下载令牌 Token 寿命 (秒)</label>
              </div>
              <el-input-number v-model="form.download_token_ttl_seconds" :min="0" :step="60" controls-position="right" class="full-width-input mt-2" />
            </div>
          </el-form>
        </article>

        <!-- 风险控制 -->
        <article class="settings-card base-card">
          <div class="card-header compact">
            <div class="header-icon tone-red"><el-icon><Warning /></el-icon></div>
            <div>
              <h2 class="card-title">熔断与风控</h2>
              <p class="card-subtitle">提前收口探测、撞库等恶意请求。</p>
            </div>
          </div>
          <el-form class="modern-form" label-position="top">
            <div class="form-row split-col last-row">
              <div class="sub-col">
                <div class="row-info"><label>容错次数阈值</label></div>
                <el-input-number v-model="form.error_count" :min="0" controls-position="right" class="full-width-input mt-2" />
              </div>
              <div class="sub-col">
                <div class="row-info"><label>判定窗口 (分钟)</label></div>
                <el-input-number v-model="form.error_minute" :min="0" controls-position="right" class="full-width-input mt-2" />
              </div>
            </div>
          </el-form>
          <div class="card-footer-tip">
            <el-icon><InfoFilled /></el-icon>
            体积 + 次数 + 错误窗口联动限制可避免仅依赖单一维度的限流失效。
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* 全局变量与底色设定 */
.modern-settings-page {
  --primary-color: #6366f1; /* 强调紫蓝色 */
  --primary-light: #e0e7ff;
  --success-color: #10b981;
  --warning-color: #f59e0b;
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

/* 顶部 Header */
.app-header {
  padding: 32px 32px 0 32px;
  overflow: hidden;
}
.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  gap: 40px;
}
.header-intro {
  flex: 1;
  max-width: 700px;
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
.page-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}
.page-description {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.6;
  margin: 0 0 20px 0;
}
.hero-badges {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.modern-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: #f1f5f9;
  color: var(--text-regular);
}
.modern-badge.is-success { background: #ecfdf5; color: #059669; }
.modern-badge.is-success .dot { background: #10b981; box-shadow: 0 0 0 2px #d1fae5; }
.modern-badge.is-warning { background: #fffbeb; color: #d97706; }
.modern-badge.is-warning .dot { background: #f59e0b; box-shadow: 0 0 0 2px #fef3c7; }
.modern-badge.is-info { background: #eff6ff; color: #2563eb; }
.dot { width: 8px; height: 8px; border-radius: 50%; }

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 状态条 */
.header-stats-strip {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr auto 1fr;
  align-items: center;
  border-top: 1px solid var(--border-color);
  padding: 20px 0;
  gap: 16px;
}
:deep(.header-stats-strip .ui-stat-card) { padding: 0; background: transparent; border: none; box-shadow: none; }

/* 策略总览 (Overview Grid) */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}
.stat-card {
  background: transparent;
  border: 1px dashed var(--border-color);
  box-shadow: none;
  border-radius: 16px;
  transition: all 0.3s ease;
}
.stat-card:hover { background: #fff; border-style: solid; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); transform: translateY(-2px); }

/* 主体设置排版 */
.settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 1fr;
  gap: 24px;
  align-items: start;
}
.settings-main, .settings-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 设置项卡片 */
.settings-card {
  overflow: hidden;
}
.card-header {
  padding: 24px 32px;
  border-bottom: 1px solid var(--border-color);
  background: #f8fafc;
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.card-header.compact { padding: 20px 24px; }
.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.header-icon.tone-blue { background: #e0f2fe; color: #0284c7; }
.header-icon.tone-red { background: #fee2e2; color: #dc2626; }
.card-title { font-size: 18px; font-weight: 700; color: var(--text-main); margin: 0 0 6px 0; }
.card-subtitle { font-size: 13px; color: var(--text-muted); margin: 0; line-height: 1.5; }

/* 深度定制表单行 (Vercel Style) */
.modern-form { padding: 12px 32px; }
.settings-sidebar .modern-form { padding: 12px 24px; }

.form-row {
  padding: 24px 0;
  border-bottom: 1px solid var(--border-color);
}
.form-row.last-row { border-bottom: none; }
.form-row.flex-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}
.form-row.split-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.row-info label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}
.row-info span {
  display: block;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}
.row-info strong { color: var(--text-main); font-weight: 600; }

.mt-2 { margin-top: 8px; }
.mt-3 { margin-top: 16px; }

/* 控件覆盖样式 */
.full-width-input { width: 100%; }
:deep(.full-width-input .el-input__wrapper) { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }

/* 复选框组优化为胶囊按钮 */
.modern-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
:deep(.modern-checkbox-group .el-checkbox-button__inner) {
  border: 1px solid var(--border-color) !important;
  border-radius: 8px !important;
  padding: 8px 16px;
  background: #f8fafc;
  color: var(--text-regular);
  box-shadow: none !important;
  transition: all 0.2s;
}
:deep(.modern-checkbox-group .el-checkbox-button.is-checked .el-checkbox-button__inner) {
  background: var(--primary-light);
  border-color: var(--primary-color) !important;
  color: var(--primary-color);
  font-weight: 600;
}

/* 底部提示区 */
.card-footer-tip {
  padding: 16px 24px;
  background: #fffbeb;
  border-top: 1px solid #fde68a;
  color: #b45309;
  font-size: 13px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.5;
}
.card-footer-tip .el-icon { font-size: 16px; margin-top: 2px; }

/* 响应式适配 */
@media (max-width: 1024px) {
  .settings-layout { grid-template-columns: 1fr; }
  .header-main { flex-direction: column; }
  .header-actions { width: 100%; justify-content: flex-start; }
}

@media (max-width: 768px) {
  .hidden-mobile { display: none; }
  .header-stats-strip { grid-template-columns: 1fr 1fr; gap: 24px 16px; }
  .form-row.flex-row { flex-direction: column; align-items: flex-start; }
  .form-row.split-col { grid-template-columns: 1fr; }
  .modern-form, .settings-sidebar .modern-form { padding: 12px 20px; }
  .card-header { padding: 20px; flex-direction: column; align-items: flex-start; }
}
</style>