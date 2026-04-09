<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { UploadFilled, Document, Clock, Connection, Check, CopyDocument, TopRight, Bottom } from '@element-plus/icons-vue'
import fileCodeBoxService, {
  type FileCodeBoxSelectResult,
  type FileCodeBoxShareResult
} from '@/services/fileCodeBox'

interface SendHistoryItem {
  id: string
  code: string
  name?: string
  kind: 'text' | 'file' | 'chunk' | 'presign'
  createdAt: string
}

interface PickupHistoryItem {
  id: string
  code: string
  name: string
  size: number
  createdAt: string
  isFile: boolean
}

const route = useRoute()
const router = useRouter()

const SEND_HISTORY_KEY = 'fcb_send_history'
const PICKUP_HISTORY_KEY = 'fcb_pickup_history'
const PICKUP_CODE_LENGTH = 8

const activeTab = ref<'text' | 'file' | 'chunk' | 'presign' | 'select'>('text')

const sharing = ref(false)
const selecting = ref(false)
const chunkUploading = ref(false)
const presignUploading = ref(false)
const sendHistoryVisible = ref(false)
const pickupHistoryVisible = ref(false)

const textContent = ref('')
const textExpireStyle = ref('hour')
const textExpireValue = ref(1)

const fileExpireStyle = ref('hour')
const fileExpireValue = ref(1)
const fileToShare = ref<File | null>(null)

const chunkFile = ref<File | null>(null)
const chunkExpireStyle = ref('hour')
const chunkExpireValue = ref(1)
const chunkProgress = ref(0)
const chunkUploadId = ref('')

const pickupCode = ref('')
const selected = ref<FileCodeBoxSelectResult | null>(null)
const selectedIsDownload = ref(false)

const presignFile = ref<File | null>(null)
const presignExpireStyle = ref('hour')
const presignExpireValue = ref(1)
const presignMode = ref('')
const presignUploadUrl = ref('')

const lastShareResult = ref<FileCodeBoxShareResult | null>(null)
const sendHistories = ref<SendHistoryItem[]>([])
const pickupHistories = ref<PickupHistoryItem[]>([])
const qrDialogVisible = ref(false)
const qrImageData = ref('')
const qrLoading = ref(false)
const qrCodeTarget = ref('')

const expireStyles = ['hour']
const showPresignUpload = false

const safeJsonParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const loadHistories = () => {
  sendHistories.value = safeJsonParse<SendHistoryItem[]>(localStorage.getItem(SEND_HISTORY_KEY), [])
  pickupHistories.value = safeJsonParse<PickupHistoryItem[]>(localStorage.getItem(PICKUP_HISTORY_KEY), [])
}

const saveSendHistories = () => {
  localStorage.setItem(SEND_HISTORY_KEY, JSON.stringify(sendHistories.value.slice(0, 30)))
}

const savePickupHistories = () => {
  localStorage.setItem(PICKUP_HISTORY_KEY, JSON.stringify(pickupHistories.value.slice(0, 30)))
}

const pushSendHistory = (result: FileCodeBoxShareResult, kind: SendHistoryItem['kind']) => {
  const item: SendHistoryItem = {
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    code: result.code,
    name: result.name,
    kind,
    createdAt: new Date().toISOString()
  }
  sendHistories.value = [item, ...sendHistories.value.filter((entry) => entry.code !== item.code)]
  saveSendHistories()
}

const pushPickupHistory = (code: string, detail: FileCodeBoxSelectResult) => {
  const item: PickupHistoryItem = {
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    code,
    name: detail.name,
    size: detail.size,
    createdAt: new Date().toISOString(),
    isFile: typeof detail.text === 'string' && detail.text.startsWith('/api/public/share/download')
  }
  pickupHistories.value = [item, ...pickupHistories.value.filter((entry) => entry.code !== item.code)]
  savePickupHistories()
}

const clearSendHistory = () => {
  sendHistories.value = []
  saveSendHistories()
}

const clearPickupHistory = () => {
  pickupHistories.value = []
  savePickupHistories()
}

const fillPickupCode = (code: string) => {
  pickupCode.value = code
  activeTab.value = 'select'
}

const goPickupSpace = (code: string) => {
  router.push({
    path: '/dashboard/pickup-space',
    query: { code }
  })
}

const buildPickupUrl = (code: string) => {
  const basePath = `${window.location.origin}/dashboard/pickup-space`
  return `${basePath}?code=${encodeURIComponent(code)}`
}

const qrLink = computed(() => {
  if (!qrCodeTarget.value) return ''
  return buildPickupUrl(qrCodeTarget.value)
})

const sendCount = computed(() => sendHistories.value.length)
const pickupCount = computed(() => pickupHistories.value.length)
const modeLabel = computed(() => {
  const labels: Record<typeof activeTab.value, string> = {
    text: '文本快传',
    file: '文件快传',
    chunk: '分片上传',
    presign: '直传上传',
    select: '取件验证'
  }
  return labels[activeTab.value]
})
const selectedTextPreview = computed(() => {
  if (!selected.value || selectedIsDownload.value) return ''
  const text = selected.value.text || ''
  return text.length > 1000 ? `${text.slice(0, 1000)}...` : text
})

const sendPreviewList = computed(() => sendHistories.value.slice(0, 4))
const pickupPreviewList = computed(() => pickupHistories.value.slice(0, 4))

const formatSize = (size: number) => {
  if (size <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let value = size
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const formatDateTime = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const copyText = async (text: string, successText: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successText)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const copyPickupLink = async (code: string) => {
  await copyText(buildPickupUrl(code), '取件链接已复制')
}

const openQrDialog = async (code: string) => {
  try {
    qrCodeTarget.value = code
    qrDialogVisible.value = true
    qrLoading.value = true
    qrImageData.value = await QRCode.toDataURL(buildPickupUrl(code), {
      margin: 2,
      width: 320,
      color: {
        dark: '#334155',
        light: '#ffffff'
      }
    })
  } catch {
    ElMessage.error('二维码生成失败')
  } finally {
    qrLoading.value = false
  }
}

const onTextShare = async () => {
  if (!textContent.value.trim()) {
    ElMessage.warning('请输入要发送的文本')
    return
  }

  try {
    sharing.value = true
    lastShareResult.value = await fileCodeBoxService.shareText(
      textContent.value,
      textExpireValue.value,
      textExpireStyle.value
    )
    pushSendHistory(lastShareResult.value, 'text')
    ElMessage.success(`快传已创建，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '文本快传失败'
    ElMessage.error(message)
  } finally {
    sharing.value = false
  }
}

const onFilePicked = (event: Event) => {
  const input = event.target as HTMLInputElement
  fileToShare.value = input.files?.[0] || null
}

const onFileShare = async () => {
  if (!fileToShare.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    sharing.value = true
    lastShareResult.value = await fileCodeBoxService.shareFile(
      fileToShare.value,
      fileExpireValue.value,
      fileExpireStyle.value
    )
    pushSendHistory(lastShareResult.value, 'file')
    ElMessage.success(`快传已创建，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '文件快传失败'
    ElMessage.error(message)
  } finally {
    sharing.value = false
  }
}

const onChunkFilePicked = (event: Event) => {
  const input = event.target as HTMLInputElement
  chunkFile.value = input.files?.[0] || null
}

const onChunkShare = async () => {
  if (!chunkFile.value) {
    ElMessage.warning('请先选择要分片上传的文件')
    return
  }

  const chunkSize = 5 * 1024 * 1024
  try {
    chunkUploading.value = true
    chunkProgress.value = 0

    const initResult = await fileCodeBoxService.initChunkUpload(
      chunkFile.value.name,
      chunkFile.value.size,
      chunkSize,
      chunkExpireValue.value,
      chunkExpireStyle.value
    )

    chunkUploadId.value = initResult.upload_id
    const uploadedSet = new Set<number>(initResult.uploaded_chunks || [])
    const totalChunks = initResult.total_chunks
    const effectiveChunkSize = initResult.chunk_size || chunkSize

    for (let index = 0; index < totalChunks; index += 1) {
      if (!uploadedSet.has(index)) {
        const start = index * effectiveChunkSize
        const end = Math.min(chunkFile.value.size, start + effectiveChunkSize)
        const part = chunkFile.value.slice(start, end)
        await fileCodeBoxService.uploadChunk(initResult.upload_id, index, part)
        uploadedSet.add(index)
      }

      chunkProgress.value = Math.round((uploadedSet.size / totalChunks) * 100)
    }

    const status = await fileCodeBoxService.getChunkUploadStatus(initResult.upload_id)
    chunkProgress.value = Math.round(status.progress)

    lastShareResult.value = await fileCodeBoxService.completeChunkUpload(
      initResult.upload_id,
      chunkExpireValue.value,
      chunkExpireStyle.value
    )

    chunkProgress.value = 100
    pushSendHistory(lastShareResult.value, 'chunk')
    ElMessage.success(`分片上传完成，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '分片上传失败'
    ElMessage.error(message)
  } finally {
    chunkUploading.value = false
  }
}

const onSelectByCode = async () => {
  const code = pickupCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入取件码')
    return
  }
  if (code.length !== PICKUP_CODE_LENGTH) {
    ElMessage.warning(`取件码必须为 ${PICKUP_CODE_LENGTH} 位`)
    return
  }
  if (!/^\d{8}$/.test(code)) {
    ElMessage.warning('取件码仅支持8位数字')
    return
  }

  try {
    selecting.value = true
    const detail = await fileCodeBoxService.selectByCode(code)
    selected.value = detail
    selectedIsDownload.value = typeof detail.text === 'string' && detail.text.startsWith('/api/public/share/download')
    pushPickupHistory(code, detail)
    ElMessage.success('取件成功')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '取件失败'
    ElMessage.error(message)
  } finally {
    selecting.value = false
  }
}

const downloadSelectedFile = () => {
  if (!selected.value || !selectedIsDownload.value) return
  window.open(`${window.location.origin}${selected.value.text}`, '_blank')
}

const onPresignFilePicked = (event: Event) => {
  const input = event.target as HTMLInputElement
  presignFile.value = input.files?.[0] || null
}

const uploadByDirectUrl = async (uploadUrl: string, file: File) => {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    body: file
  })
  if (!response.ok) {
    throw new Error(`直传失败：${response.status}`)
  }
}

const onPresignUpload = async () => {
  if (!presignFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    presignUploading.value = true
    const initResult = await fileCodeBoxService.initPresignUpload(
      presignFile.value.name,
      presignFile.value.size,
      presignExpireValue.value,
      presignExpireStyle.value
    )

    presignMode.value = initResult.mode
    presignUploadUrl.value = initResult.upload_url

    if (initResult.mode === 'proxy') {
      lastShareResult.value = await fileCodeBoxService.uploadPresignProxy(initResult.upload_id, presignFile.value)
    } else {
      await uploadByDirectUrl(initResult.upload_url, presignFile.value)
      lastShareResult.value = await fileCodeBoxService.confirmPresignUpload(
        initResult.upload_id,
        presignExpireValue.value,
        presignExpireStyle.value
      )
    }

    pushSendHistory(lastShareResult.value, 'presign')
    ElMessage.success(`直传已完成，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '直传上传失败'
    ElMessage.error(message)
  } finally {
    presignUploading.value = false
  }
}

onMounted(() => {
  loadHistories()
  const codeFromQuery = route.query.code
  if (typeof codeFromQuery === 'string' && codeFromQuery.trim()) {
    fillPickupCode(codeFromQuery.trim())
  }
})
</script>

<template>
  <div class="page-container">
    <!-- Header Hero Section -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="title">快传工作台</h1>
        <p class="subtitle">Quick Transfer — 高效流转您的文件与文本信息</p>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-label">当前模式</span>
          <span class="stat-value primary">{{ modeLabel }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">发件记录</span>
          <span class="stat-value">{{ sendCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">取件记录</span>
          <span class="stat-value">{{ pickupCount }}</span>
        </div>
      </div>
    </header>

    <!-- Main Layout Grid -->
    <div class="main-layout">
      <!-- Left Column: Actions & Forms -->
      <div class="action-column">
        <el-card class="box-card form-card" shadow="hover">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane label="文本快传" name="text"></el-tab-pane>
            <el-tab-pane label="文件快传" name="file"></el-tab-pane>
            <el-tab-pane label="分片上传" name="chunk"></el-tab-pane>
            <el-tab-pane v-if="showPresignUpload" label="直传上传" name="presign"></el-tab-pane>
            <el-tab-pane label="取件验证" name="select"></el-tab-pane>
          </el-tabs>

          <div class="tab-content">
            <!-- Text Share -->
            <transition name="fade-slide" mode="out-in">
              <div v-if="activeTab === 'text'" class="form-panel">
                <el-input
                  v-model="textContent"
                  type="textarea"
                  :rows="10"
                  maxlength="220000"
                  show-word-limit
                  placeholder="在此输入或粘贴您要发送的文本内容"
                  class="custom-textarea"
                />
                <div class="settings-row">
                  <div class="setting-item">
                    <span class="setting-label"><el-icon><Clock /></el-icon> 过期策略</span>
                    <el-select v-model="textExpireStyle" class="full-width">
                      <el-option v-for="item in expireStyles" :key="item" :label="item" :value="item" />
                    </el-select>
                  </div>
                  <div class="setting-item">
                    <span class="setting-label">有效数值</span>
                    <el-input-number v-model="textExpireValue" :min="1" :max="1" class="full-width" />
                  </div>
                </div>
                <el-button type="primary" size="large" class="submit-btn" :loading="sharing" @click="onTextShare">
                  <el-icon class="el-icon--left"><Connection /></el-icon> 生成文本取件码
                </el-button>
              </div>

              <!-- File Share -->
              <div v-else-if="activeTab === 'file'" class="form-panel">
                <label class="modern-dropzone" :class="{ 'has-file': fileToShare }">
                  <input type="file" @change="onFilePicked" hidden />
                  <el-icon class="dropzone-icon"><UploadFilled /></el-icon>
                  <strong class="dropzone-title">{{ fileToShare ? fileToShare.name : '点击或拖拽文件到这里' }}</strong>
                  <span class="dropzone-desc">{{ fileToShare ? formatSize(fileToShare.size) : '普通文件走标准快传链路' }}</span>
                </label>
                <div class="settings-row">
                  <div class="setting-item">
                    <span class="setting-label"><el-icon><Clock /></el-icon> 过期策略</span>
                    <el-select v-model="fileExpireStyle" class="full-width">
                      <el-option v-for="item in expireStyles" :key="item" :label="item" :value="item" />
                    </el-select>
                  </div>
                  <div class="setting-item">
                    <span class="setting-label">有效数值</span>
                    <el-input-number v-model="fileExpireValue" :min="1" :max="1" class="full-width" />
                  </div>
                </div>
                <el-button type="primary" size="large" class="submit-btn" :loading="sharing" @click="onFileShare">
                  <el-icon class="el-icon--left"><Document /></el-icon> 创建文件快传
                </el-button>
              </div>

              <!-- Chunk Share -->
              <div v-else-if="activeTab === 'chunk'" class="form-panel">
                <label class="modern-dropzone" :class="{ 'has-file': chunkFile }">
                  <input type="file" @change="onChunkFilePicked" hidden />
                  <el-icon class="dropzone-icon"><UploadFilled /></el-icon>
                  <strong class="dropzone-title">{{ chunkFile ? chunkFile.name : '选择要分片上传的大文件' }}</strong>
                  <span class="dropzone-desc">{{ chunkFile ? formatSize(chunkFile.size) : '默认 5MB 分片，支持断点续传' }}</span>
                </label>
                <div class="settings-row">
                  <div class="setting-item">
                    <span class="setting-label"><el-icon><Clock /></el-icon> 过期策略</span>
                    <el-select v-model="chunkExpireStyle" class="full-width">
                      <el-option v-for="item in expireStyles" :key="item" :label="item" :value="item" />
                    </el-select>
                  </div>
                  <div class="setting-item">
                    <span class="setting-label">有效数值</span>
                    <el-input-number v-model="chunkExpireValue" :min="1" :max="1" class="full-width" />
                  </div>
                </div>
                <el-button type="primary" size="large" class="submit-btn" :loading="chunkUploading" @click="onChunkShare">
                  开始分片上传
                </el-button>
                <div v-if="chunkUploading || chunkProgress > 0" class="status-alert">
                  <strong>上传进度：{{ chunkProgress }}%</strong>
                  <p v-if="chunkUploadId">会话ID：{{ chunkUploadId }}</p>
                  <el-progress :percentage="chunkProgress" :stroke-width="12" status="success" />
                </div>
              </div>

              <!-- Presign Share -->
              <div v-else-if="showPresignUpload && activeTab === 'presign'" class="form-panel">
                <label class="modern-dropzone" :class="{ 'has-file': presignFile }">
                  <input type="file" @change="onPresignFilePicked" hidden />
                  <el-icon class="dropzone-icon"><UploadFilled /></el-icon>
                  <strong class="dropzone-title">{{ presignFile ? presignFile.name : '选择要直传的大文件' }}</strong>
                  <span class="dropzone-desc">{{ presignFile ? formatSize(presignFile.size) : '适合对象存储直传和大文件场景' }}</span>
                </label>
                <div class="settings-row">
                  <div class="setting-item">
                    <span class="setting-label"><el-icon><Clock /></el-icon> 过期策略</span>
                    <el-select v-model="presignExpireStyle" class="full-width">
                      <el-option v-for="item in expireStyles" :key="item" :label="item" :value="item" />
                    </el-select>
                  </div>
                  <div class="setting-item">
                    <span class="setting-label">有效数值</span>
                    <el-input-number v-model="presignExpireValue" :min="1" :max="1" class="full-width" />
                  </div>
                </div>
                <el-button type="primary" size="large" class="submit-btn" :loading="presignUploading" @click="onPresignUpload">
                  开始直传文件
                </el-button>
                <div v-if="presignMode" class="status-alert">
                  <strong>上传模式：{{ presignMode }}</strong>
                  <p>{{ presignUploadUrl || '当前模式无需展示上传地址' }}</p>
                </div>
              </div>

              <!-- Pickup Share -->
              <div v-else class="form-panel">
                <div class="pickup-input-wrapper">
                  <el-input
                    v-model="pickupCode"
                    placeholder="请输入 8 位数字取件码"
                    size="large"
                    maxlength="8"
                    clearable
                    @keyup.enter="onSelectByCode"
                    class="pickup-input"
                  >
                    <template #append>
                      <el-button type="primary" :loading="selecting" @click="onSelectByCode">
                        立即验证
                      </el-button>
                    </template>
                  </el-input>
                </div>

                <div v-if="pickupPreviewList.length" class="quick-pickup-chips">
                  <span class="chips-label">最近取件记录：</span>
                  <el-tag
                    v-for="item in pickupPreviewList"
                    :key="item.id"
                    class="pickup-chip"
                    type="info"
                    effect="plain"
                    round
                    @click="fillPickupCode(item.code)"
                  >
                    {{ item.code }} <span class="chip-name">{{ item.name }}</span>
                  </el-tag>
                </div>
              </div>
            </transition>
          </div>
        </el-card>
      </div>

      <!-- Right Column: Results & History -->
      <div class="result-column">
        <!-- Action Dashboard -->
        <el-card class="box-card result-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>{{ activeTab === 'select' ? '取件结果' : '分发结果' }}</span>
              <el-button v-if="activeTab !== 'select' && lastShareResult" link type="primary" @click="goPickupSpace(lastShareResult.code)">
                打开空间 <el-icon class="el-icon--right"><TopRight /></el-icon>
              </el-button>
              <el-button v-if="activeTab === 'select' && selected" link type="primary" @click="goPickupSpace(pickupCode)">
                打开空间 <el-icon class="el-icon--right"><TopRight /></el-icon>
              </el-button>
            </div>
          </template>

          <transition name="fade-slide" mode="out-in">
            <!-- Share Mode Result -->
            <div v-if="activeTab !== 'select' && lastShareResult" class="share-result">
              <div class="success-icon"><el-icon><Check /></el-icon></div>
              <h3 class="result-code">{{ lastShareResult.code }}</h3>
              <p class="result-name">{{ lastShareResult.name || '未设置显示名称' }}</p>
              
              <div class="action-buttons-group">
                <el-button type="primary" round @click="copyText(lastShareResult.code, '取件码已复制')">
                  复制取件码
                </el-button>
                <el-button round @click="copyPickupLink(lastShareResult.code)">复制链接</el-button>
                <el-button round @click="openQrDialog(lastShareResult.code)">二维码</el-button>
              </div>
            </div>

            <!-- Pickup Mode Result -->
            <div v-else-if="activeTab === 'select' && selected" class="pickup-result">
              <div class="pickup-info-grid">
                <div class="info-block">
                  <span class="info-label">内容名称</span>
                  <span class="info-value text-ellipsis" :title="selected.name">{{ selected.name || '--' }}</span>
                </div>
                <div class="info-block">
                  <span class="info-label">文件大小</span>
                  <span class="info-value">{{ formatSize(selected.size) }}</span>
                </div>
                <div class="info-block">
                  <span class="info-label">类型</span>
                  <el-tag size="small" :type="selectedIsDownload ? 'success' : 'warning'">
                    {{ selectedIsDownload ? '文件' : '文本' }}
                  </el-tag>
                </div>
              </div>

              <div class="pickup-action-area">
                <template v-if="selectedIsDownload">
                  <el-button type="primary" class="full-width" size="large" @click="downloadSelectedFile">
                    <el-icon class="el-icon--left"><Bottom /></el-icon> 立即下载文件
                  </el-button>
                </template>
                <template v-else>
                  <el-input :model-value="selectedTextPreview" type="textarea" :rows="8" readonly class="readonly-textarea"/>
                  <el-button plain class="full-width mt-10" @click="copyText(selected.text || '', '内容已复制到剪贴板')">
                    <el-icon class="el-icon--left"><CopyDocument /></el-icon> 复制由于内容
                  </el-button>
                </template>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty :description="activeTab === 'select' ? '请输入取件码验证后查看内容' : '提交文件或文本后，这里将生成取件码'" :image-size="80" />
            </div>
          </transition>
        </el-card>

        <!-- Quick Histories -->
        <el-card class="box-card history-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>活动记录</span>
            </div>
          </template>
          
          <div class="history-actions">
            <el-button plain size="small" @click="sendHistoryVisible = true">查看所有发件</el-button>
            <el-button plain size="small" @click="pickupHistoryVisible = true">查看所有取件</el-button>
          </div>
          
          <div v-if="sendPreviewList.length || pickupPreviewList.length" class="mini-history-list">
             <div class="list-title" v-if="sendPreviewList.length">最近发件</div>
             <div 
               v-for="item in sendPreviewList.slice(0, 2)" :key="'s-'+item.id" 
               class="mini-history-item"
               @click="goPickupSpace(item.code)"
             >
                <div class="item-main">
                  <span class="item-code">{{ item.code }}</span>
                  <span class="item-name">{{ item.name || '未命名' }}</span>
                </div>
                <el-tag size="small" type="info">{{ item.kind }}</el-tag>
             </div>
             
             <div class="list-title mt-10" v-if="pickupPreviewList.length">最近取件</div>
             <div 
               v-for="item in pickupPreviewList.slice(0, 2)" :key="'p-'+item.id" 
               class="mini-history-item"
               @click="fillPickupCode(item.code)"
             >
                <div class="item-main">
                  <span class="item-code">{{ item.code }}</span>
                  <span class="item-name">{{ item.name }}</span>
                </div>
                <el-tag size="small" :type="item.isFile ? 'success' : 'warning'">{{ item.isFile ? '文件' : '文本' }}</el-tag>
             </div>
          </div>
          <el-empty v-else description="暂无活动记录" :image-size="50" />
        </el-card>
      </div>
    </div>

    <!-- Modals & Drawers -->
    <el-drawer v-model="sendHistoryVisible" title="发件历史" direction="rtl" size="460px" class="custom-drawer">
      <template #header>
        <div class="drawer-header-content">
          <span class="draw-title">全部发件记录</span>
          <el-button type="danger" link @click="clearSendHistory">清空记录</el-button>
        </div>
      </template>

      <el-empty v-if="sendHistories.length === 0" description="暂无发件历史" />
      <div v-else class="drawer-list">
        <div v-for="item in sendHistories" :key="item.id" class="drawer-item">
          <div class="drawer-item-header">
            <span class="drawer-code">{{ item.code }}</span>
            <span class="drawer-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="drawer-item-body">{{ item.name || '未命名内容' }}</div>
          <div class="drawer-item-actions">
            <el-button link type="primary" size="small" @click="copyText(item.code, '取件码已复制')">复制取件码</el-button>
            <el-button link size="small" @click="copyPickupLink(item.code)">复制链接</el-button>
            <el-button link size="small" @click="openQrDialog(item.code)">二维码</el-button>
            <el-button link size="small" @click="goPickupSpace(item.code)">打开</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="pickupHistoryVisible" title="取件历史" direction="rtl" size="460px" class="custom-drawer">
      <template #header>
        <div class="drawer-header-content">
          <span class="draw-title">全部取件记录</span>
          <el-button type="danger" link @click="clearPickupHistory">清空记录</el-button>
        </div>
      </template>

      <el-empty v-if="pickupHistories.length === 0" description="暂无取件历史" />
      <div v-else class="drawer-list">
        <div v-for="item in pickupHistories" :key="item.id" class="drawer-item">
          <div class="drawer-item-header">
            <span class="drawer-code">{{ item.code }}</span>
            <span class="drawer-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="drawer-item-body">{{ item.name }}</div>
          <div class="drawer-item-actions">
            <el-button link type="primary" size="small" @click="fillPickupCode(item.code)">填入验证</el-button>
            <el-button link size="small" @click="copyText(item.code, '取件码已复制')">复制取件码</el-button>
            <el-button link size="small" @click="goPickupSpace(item.code)">直接打开</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="qrDialogVisible" title="扫码取件" width="380px" align-center custom-class="qr-dialog">
      <div class="qr-container" v-loading="qrLoading">
        <div class="qr-wrapper">
          <img v-if="qrImageData" :src="qrImageData" alt="取件二维码" />
        </div>
        <p class="qr-tip">使用手机相机或其他扫码工具扫描提取</p>
        <el-input :model-value="qrLink" readonly class="qr-input">
          <template #append>
            <el-button type="primary" @click="copyText(qrLink, '取件链接已复制')">复制链接</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
/* Base Layout & Variables */
.page-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 100vh;
  background-color: var(--el-bg-color-page, #f8fafc);
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

/* Header Section */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e2e8f0);
}

.header-content .title {
  margin: 0 0 8px 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--el-text-color-primary, #0f172a);
  letter-spacing: -0.5px;
}

.header-content .subtitle {
  margin: 0;
  font-size: 15px;
  color: var(--el-text-color-secondary, #64748b);
}

.header-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary, #64748b);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary, #0f172a);
}
.stat-value.primary {
  color: var(--el-color-primary, #409eff);
}

/* Main Grid Layout */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  align-items: start;
}

.box-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.04) !important;
  background: #ffffff;
}

/* Form Styles */
.form-card {
  min-height: 550px;
}
.custom-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
}
.form-panel {
  padding: 16px 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fadeIn 0.3s ease-in-out;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 16px;
  font-size: 15px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}
.custom-textarea :deep(.el-textarea__inner:focus) {
  background-color: #ffffff;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* Dropzone Styles */
.modern-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 240px;
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
  background-color: #f8fafc;
  cursor: pointer;
  transition: all 0.3s ease;
}
.modern-dropzone:hover, .modern-dropzone.has-file {
  border-color: var(--el-color-primary);
  background-color: #f0f9ff;
}
.dropzone-icon {
  font-size: 48px;
  color: #94a3b8;
  margin-bottom: 16px;
  transition: color 0.3s ease;
}
.modern-dropzone:hover .dropzone-icon, .modern-dropzone.has-file .dropzone-icon {
  color: var(--el-color-primary);
}
.dropzone-title {
  font-size: 16px;
  color: #334155;
  margin-bottom: 8px;
}
.dropzone-desc {
  font-size: 13px;
  color: #64748b;
}

/* Settings Row */
.settings-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding-top: 8px;
}
.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.setting-label {
  font-size: 14px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 4px;
}
.full-width {
  width: 100%;
}

.submit-btn {
  height: 52px;
  font-size: 16px;
  border-radius: 12px;
  margin-top: 16px;
}

/* Pickup Specific */
.pickup-input :deep(.el-input__wrapper) {
  padding-left: 16px;
  height: 56px;
  font-size: 16px;
  border-radius: 12px 0 0 12px;
}
.pickup-input :deep(.el-input-group__append) {
  border-radius: 0 12px 12px 0;
  background-color: var(--el-color-primary);
  color: white;
  border: none;
  padding: 0 24px;
  font-weight: 600;
  cursor: pointer;
}
.pickup-input :deep(.el-input-group__append:hover) {
  opacity: 0.9;
}
.quick-pickup-chips {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.chips-label {
  font-size: 13px;
  color: #64748b;
}
.pickup-chip {
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background-color: #f1f5f9;
}
.pickup-chip:hover {
  background-color: #e2e8f0;
  transform: translateY(-1px);
}
.chip-name {
  color: #94a3b8;
  margin-left: 4px;
}

/* Result Column */
.result-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #0f172a;
}
.empty-state {
  padding: 40px 0;
}

/* Share Results */
.share-result {
  text-align: center;
  padding: 24px 0;
}
.success-icon {
  width: 56px;
  height: 56px;
  background-color: #f0fdf4;
  color: #22c55e;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin: 0 auto 24px;
}
.result-code {
  font-size: 48px;
  font-weight: 800;
  color: var(--el-color-primary);
  letter-spacing: 2px;
  margin: 0 0 12px 0;
  line-height: 1;
}
.result-name {
  color: #64748b;
  margin: 0 0 32px 0;
}
.action-buttons-group {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* Pickup Results */
.pickup-result {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.pickup-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  background-color: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.info-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-label {
  font-size: 12px;
  color: #64748b;
}
.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}
.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mt-10 {
  margin-top: 10px;
}

/* Histories Sidebar */
.history-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.mini-history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.list-title {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
}
.mini-history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f8fafc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.mini-history-item:hover {
  background-color: #ffffff;
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.item-code {
  font-weight: 600;
  color: #0f172a;
  display: block;
}
.item-name {
  font-size: 12px;
  color: #64748b;
}

/* Drawers */
.drawer-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.draw-title {
  font-size: 18px;
  font-weight: 600;
}
.drawer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 8px;
}
.drawer-item {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}
.drawer-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.drawer-code {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.drawer-time {
  font-size: 12px;
  color: #94a3b8;
}
.drawer-item-body {
  font-size: 14px;
  color: #475569;
  margin-bottom: 12px;
}
.drawer-item-actions {
  display: flex;
  gap: 8px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 12px;
}

/* QR Code Dialog */
.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}
.qr-wrapper {
  background: white;
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 16px;
}
.qr-wrapper img {
  width: 220px;
  height: 220px;
  display: block;
}
.qr-tip {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 24px;
}
.qr-input {
  width: 100%;
}

/* Animations */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Responsive adjustments */
@media (max-width: 1024px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  .header-stats {
    display: none;
  }
}
@media (max-width: 768px) {
  .page-container {
    padding: 16px;
  }
  .settings-row {
    grid-template-columns: 1fr;
  }
}
</style>