<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import fileCodeBoxService, {
  type FileCodeBoxSelectResult,
  type FileCodeBoxShareResult
} from '@/services/fileCodeBox'

interface SendHistoryItem {
  id: string
  code: string
  name?: string
  kind: 'text' | 'file' | 'presign'
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

const SEND_HISTORY_KEY = 'fcb_send_history'
const PICKUP_HISTORY_KEY = 'fcb_pickup_history'

const activeTab = ref('text')

const sharing = ref(false)
const selecting = ref(false)
const presignUploading = ref(false)
const sendHistoryVisible = ref(false)
const pickupHistoryVisible = ref(false)

const textContent = ref('')
const textExpireStyle = ref('day')
const textExpireValue = ref(1)

const fileExpireStyle = ref('day')
const fileExpireValue = ref(1)
const fileToShare = ref<File | null>(null)

const pickupCode = ref('')
const selected = ref<FileCodeBoxSelectResult | null>(null)
const selectedIsDownload = ref(false)

const presignFile = ref<File | null>(null)
const presignExpireStyle = ref('day')
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

const expireStyles = ['day', 'hour', 'minute', 'count', 'forever']

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
  sendHistories.value = [item, ...sendHistories.value.filter((x) => x.code !== item.code)]
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
  pickupHistories.value = [item, ...pickupHistories.value.filter((x) => x.code !== item.code)]
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

const buildPickupUrl = (code: string) => {
  const basePath = `${window.location.origin}${route.path}`
  return `${basePath}?code=${encodeURIComponent(code)}`
}

const qrLink = computed(() => {
  if (!qrCodeTarget.value) return ''
  return buildPickupUrl(qrCodeTarget.value)
})

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
      width: 320
    })
  } catch {
    ElMessage.error('二维码生成失败')
  } finally {
    qrLoading.value = false
  }
}

const formatDateTime = (value: string) => {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

const onTextShare = async () => {
  if (!textContent.value.trim()) {
    ElMessage.warning('请输入文本内容')
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
    ElMessage.success(`文本分享成功，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '文本分享失败'
    ElMessage.error(message)
  } finally {
    sharing.value = false
  }
}

const onFilePicked = (evt: Event) => {
  const input = evt.target as HTMLInputElement
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
    ElMessage.success(`文件分享成功，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '文件分享失败'
    ElMessage.error(message)
  } finally {
    sharing.value = false
  }
}

const onSelectByCode = async () => {
  if (!pickupCode.value.trim()) {
    ElMessage.warning('请输入取件码')
    return
  }

  try {
    selecting.value = true
    const detail = await fileCodeBoxService.selectByCode(pickupCode.value.trim())
    selected.value = detail
    selectedIsDownload.value = typeof detail.text === 'string' && detail.text.startsWith('/api/public/share/download')
    pushPickupHistory(pickupCode.value.trim(), detail)
    ElMessage.success('取件成功')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '取件失败'
    ElMessage.error(message)
  } finally {
    selecting.value = false
  }
}

const downloadSelectedFile = () => {
  if (!selected.value || !selectedIsDownload.value) {
    return
  }
  const absoluteUrl = `${window.location.origin}${selected.value.text}`
  window.open(absoluteUrl, '_blank')
}

const onPresignFilePicked = (evt: Event) => {
  const input = evt.target as HTMLInputElement
  presignFile.value = input.files?.[0] || null
}

const uploadByDirectUrl = async (uploadUrl: string, file: File) => {
  const resp = await fetch(uploadUrl, {
    method: 'PUT',
    body: file
  })
  if (!resp.ok) {
    throw new Error(`直传失败: ${resp.status}`)
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

    ElMessage.success(`预签名上传完成，取件码：${lastShareResult.value.code}`)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '预签名上传失败'
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
  <div class="fcb-share-page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <div class="card-title">FileCodeBox 兼容分享</div>
          <el-space>
            <el-button size="small" @click="sendHistoryVisible = true">发件记录</el-button>
            <el-button size="small" @click="pickupHistoryVisible = true">取件记录</el-button>
          </el-space>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="文本分享" name="text">
          <el-form label-width="120px">
            <el-form-item label="文本内容">
              <el-input v-model="textContent" type="textarea" :rows="5" maxlength="220000" show-word-limit />
            </el-form-item>
            <el-form-item label="过期策略">
              <el-select v-model="textExpireStyle" style="width: 180px">
                <el-option v-for="s in expireStyles" :key="s" :label="s" :value="s" />
              </el-select>
              <el-input-number v-model="textExpireValue" :min="1" style="margin-left: 12px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sharing" @click="onTextShare">创建文本分享</el-button>
            </el-form-item>
            <el-form-item v-if="lastShareResult">
              <el-alert
                type="success"
                show-icon
                :closable="false"
                :title="`本次取件码：${lastShareResult.code}`"
              >
                <template #default>
                  <el-button link type="primary" @click="copyText(lastShareResult.code, '取件码已复制')">复制取件码</el-button>
                  <el-button link @click="copyPickupLink(lastShareResult.code)">复制链接</el-button>
                  <el-button link @click="openQrDialog(lastShareResult.code)">二维码</el-button>
                </template>
              </el-alert>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="文件分享" name="file">
          <el-form label-width="120px">
            <el-form-item label="选择文件">
              <input type="file" @change="onFilePicked" />
            </el-form-item>
            <el-form-item label="过期策略">
              <el-select v-model="fileExpireStyle" style="width: 180px">
                <el-option v-for="s in expireStyles" :key="s" :label="s" :value="s" />
              </el-select>
              <el-input-number v-model="fileExpireValue" :min="1" style="margin-left: 12px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sharing" @click="onFileShare">创建文件分享</el-button>
            </el-form-item>
            <el-form-item v-if="lastShareResult">
              <el-alert
                type="success"
                show-icon
                :closable="false"
                :title="`本次取件码：${lastShareResult.code}`"
              >
                <template #default>
                  <el-button link type="primary" @click="copyText(lastShareResult.code, '取件码已复制')">复制取件码</el-button>
                  <el-button link @click="copyPickupLink(lastShareResult.code)">复制链接</el-button>
                  <el-button link @click="openQrDialog(lastShareResult.code)">二维码</el-button>
                </template>
              </el-alert>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="取件码访问" name="select">
          <el-form inline>
            <el-form-item label="取件码">
              <el-input v-model="pickupCode" placeholder="请输入取件码" style="width: 220px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="selecting" @click="onSelectByCode">取件</el-button>
            </el-form-item>
          </el-form>

          <el-card v-if="selected" class="result-card" shadow="never">
            <p><strong>名称：</strong>{{ selected.name }}</p>
            <p><strong>大小：</strong>{{ selected.size }}</p>
            <template v-if="selectedIsDownload">
              <p><strong>下载链接：</strong>{{ selected.text }}</p>
              <el-button type="success" @click="downloadSelectedFile">下载文件</el-button>
            </template>
            <template v-else>
              <p><strong>文本内容：</strong></p>
              <el-input :model-value="selected.text" type="textarea" :rows="5" readonly />
            </template>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="预签名上传" name="presign">
          <el-form label-width="120px">
            <el-form-item label="选择文件">
              <input type="file" @change="onPresignFilePicked" />
            </el-form-item>
            <el-form-item label="过期策略">
              <el-select v-model="presignExpireStyle" style="width: 180px">
                <el-option v-for="s in expireStyles" :key="s" :label="s" :value="s" />
              </el-select>
              <el-input-number v-model="presignExpireValue" :min="1" style="margin-left: 12px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="presignUploading" @click="onPresignUpload">开始预签名上传</el-button>
            </el-form-item>
            <el-form-item v-if="presignMode">
              <el-tag type="info">当前模式：{{ presignMode }}</el-tag>
              <span class="upload-url" v-if="presignUploadUrl">{{ presignUploadUrl }}</span>
            </el-form-item>
            <el-form-item v-if="lastShareResult">
              <el-alert
                type="success"
                show-icon
                :closable="false"
                :title="`本次取件码：${lastShareResult.code}`"
              >
                <template #default>
                  <el-button link type="primary" @click="copyText(lastShareResult.code, '取件码已复制')">复制取件码</el-button>
                  <el-button link @click="copyPickupLink(lastShareResult.code)">复制链接</el-button>
                  <el-button link @click="openQrDialog(lastShareResult.code)">二维码</el-button>
                </template>
              </el-alert>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-drawer v-model="sendHistoryVisible" title="发件记录" direction="rtl" size="440px">
      <template #header>
        <div class="drawer-title">
          <span>发件记录</span>
          <el-button link type="danger" @click="clearSendHistory">清空</el-button>
        </div>
      </template>

      <el-empty v-if="sendHistories.length === 0" description="暂无发件记录" />
      <div v-else class="history-list">
        <el-card v-for="item in sendHistories" :key="item.id" shadow="never" class="history-item">
          <div class="history-top">
            <el-tag size="small">{{ item.kind }}</el-tag>
            <span class="history-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="history-line"><strong>取件码：</strong>{{ item.code }}</div>
          <div class="history-line"><strong>名称：</strong>{{ item.name || '-' }}</div>
          <el-space>
            <el-button link type="primary" @click="copyText(item.code, '取件码已复制')">复制取件码</el-button>
            <el-button link @click="copyPickupLink(item.code)">复制链接</el-button>
            <el-button link @click="openQrDialog(item.code)">二维码</el-button>
            <el-button link @click="fillPickupCode(item.code)">去取件页</el-button>
          </el-space>
        </el-card>
      </div>
    </el-drawer>

    <el-drawer v-model="pickupHistoryVisible" title="取件记录" direction="rtl" size="440px">
      <template #header>
        <div class="drawer-title">
          <span>取件记录</span>
          <el-button link type="danger" @click="clearPickupHistory">清空</el-button>
        </div>
      </template>

      <el-empty v-if="pickupHistories.length === 0" description="暂无取件记录" />
      <div v-else class="history-list">
        <el-card v-for="item in pickupHistories" :key="item.id" shadow="never" class="history-item">
          <div class="history-top">
            <el-tag size="small" :type="item.isFile ? 'success' : 'info'">{{ item.isFile ? '文件' : '文本' }}</el-tag>
            <span class="history-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="history-line"><strong>取件码：</strong>{{ item.code }}</div>
          <div class="history-line"><strong>名称：</strong>{{ item.name }}</div>
          <div class="history-line"><strong>大小：</strong>{{ item.size }}</div>
          <el-space>
            <el-button link type="primary" @click="copyText(item.code, '取件码已复制')">复制取件码</el-button>
            <el-button link @click="copyPickupLink(item.code)">复制链接</el-button>
            <el-button link @click="openQrDialog(item.code)">二维码</el-button>
            <el-button link @click="fillPickupCode(item.code)">再次取件</el-button>
          </el-space>
        </el-card>
      </div>
    </el-drawer>

    <el-dialog v-model="qrDialogVisible" title="扫码取件" width="420px" align-center>
      <div class="qr-panel" v-loading="qrLoading">
        <img v-if="qrImageData" :src="qrImageData" alt="取件二维码" class="qr-image" />
        <div class="qr-hint">使用移动端扫码，可直接进入取件页并自动填充取件码。</div>
        <el-input :model-value="qrLink" readonly />
        <div class="qr-actions">
          <el-button type="primary" @click="copyText(qrLink, '取件链接已复制')">复制取件链接</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.fcb-share-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-weight: 700;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-card {
  margin-top: 8px;
}

.upload-url {
  margin-left: 10px;
  color: #909399;
  word-break: break-all;
}

.drawer-title {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  border: 1px solid #ebeef5;
}

.history-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.history-time {
  color: #8a98a8;
  font-size: 12px;
}

.history-line {
  line-height: 1.9;
  color: #2c3e50;
}

.qr-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-height: 120px;
}

.qr-image {
  width: 240px;
  height: 240px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 8px;
  background: #fff;
}

.qr-hint {
  color: #6b7280;
  font-size: 13px;
  text-align: center;
}

.qr-actions {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>
