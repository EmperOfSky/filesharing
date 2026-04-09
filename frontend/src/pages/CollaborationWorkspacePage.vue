<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Delete, Connection } from '@element-plus/icons-vue'
import * as Y from 'yjs'
import { useAuthStore } from '@/stores/auth'
import collaborationService from '@/services/collaboration'
import type { CollaborationProject, CollaborativeDocument, ProjectMember } from '@/types/collaboration'

interface SyncMessage {
  type: 'sync-request' | 'sync-state'
  docId: number
  data?: string
  ts?: number
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed<number | null>(() => {
  const raw = route.params.projectId
  const first = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(first)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})

const pageLoading = ref(false)
const project = ref<CollaborationProject | null>(null)
const documents = ref<CollaborativeDocument[]>([])
const selectedDocumentId = ref<number | null>(null)
const selectedDocument = ref<CollaborativeDocument | null>(null)
const members = ref<ProjectMember[]>([])

const editorTitle = ref('')
const editorContent = ref('')

const wsConnected = ref(false)
const wsConnecting = ref(false)
const saving = ref(false)
const creating = ref(false)
const lastSavedAt = ref<string>('')
const invitingMember = ref(false)
const loadingMembers = ref(false)
const removingMemberId = ref<number | null>(null)

const showCreateDialog = ref(false)
const newDocTitle = ref('')
const newDocType = ref<'TEXT' | 'MARKDOWN' | 'WIKI'>('TEXT')
const newMemberEmail = ref('')
const newMemberRole = ref<'ADMIN' | 'MEMBER' | 'EDITOR' | 'VIEWER'>('MEMBER')

let ydoc: Y.Doc | null = null
let ytext: Y.Text | null = null
let yTextObserver: ((event: Y.YTextEvent) => void) | null = null
let yUpdateHandler: ((update: Uint8Array, origin: unknown) => void) | null = null
let hasSeededInitialContent = false
let waitingInitialSyncState = false

let ws: WebSocket | null = null
let reconnectTimer: number | null = null
let reconnectAttempts = 0
const maxReconnectAttempts = 5

let autoSaveTimer: number | null = null

const roleLabelMap: Record<string, string> = {
  OWNER: '所有者',
  ADMIN: '管理员',
  MEMBER: '成员',
  EDITOR: '编辑',
  VIEWER: '只读'
}

const realtimeStatus = computed(() => {
  if (wsConnected.value) return '已连接'
  if (wsConnecting.value) return '连接中'
  return '未连接'
})

const canManageMembers = computed(() => {
  if (project.value?.currentUserIsOwner) return true
  return members.value.some((member) => member.canManage)
})

const clearAutoSaveTimer = () => {
  if (autoSaveTimer != null) {
    window.clearTimeout(autoSaveTimer)
    autoSaveTimer = null
  }
}

const scheduleAutoSave = () => {
  clearAutoSaveTimer()
  autoSaveTimer = window.setTimeout(() => {
    void saveDocument(false)
  }, 1200)
}

watch(editorTitle, () => {
  if (selectedDocumentId.value) {
    scheduleAutoSave()
  }
})

const normalizeWsBaseUrl = () => {
  const envWsBase = (import.meta as any).env?.VITE_WS_BASE_URL as string | undefined
  if (envWsBase && envWsBase.trim()) {
    return envWsBase.replace(/\/$/, '')
  }

  const envApiBase = (import.meta as any).env?.VITE_API_BASE_URL as string | undefined
  if (envApiBase && /^https?:\/\//i.test(envApiBase)) {
    try {
      const apiUrl = new URL(envApiBase)
      const wsProtocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${wsProtocol}//${apiUrl.host}`
    } catch {
      // ignore invalid api base url and fallback
    }
  }

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const hostname = window.location.hostname
  const currentPort = window.location.port

  const isLocalHost = hostname === 'localhost' || hostname === '127.0.0.1'
  // 仅在本地开发时把前端端口回退到后端 8080，生产域名保持当前站点端口。
  const targetPort = (isLocalHost && (!currentPort || currentPort === '3000' || currentPort === '5173' || currentPort === '4173'))
    ? '8080'
    : currentPort

  return `${protocol}://${hostname}${targetPort ? `:${targetPort}` : ''}`
}

const uint8ArrayToBase64 = (bytes: Uint8Array) => {
  let binary = ''
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(i, i + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return btoa(binary)
}

const base64ToUint8Array = (base64: string) => {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

const applyLocalTextChange = (next: string) => {
  if (!ytext) return

  const current = ytext.toString()
  if (current === next) return

  let start = 0
  while (start < current.length && start < next.length && current[start] === next[start]) {
    start += 1
  }

  let currentEnd = current.length - 1
  let nextEnd = next.length - 1
  while (currentEnd >= start && nextEnd >= start && current[currentEnd] === next[nextEnd]) {
    currentEnd -= 1
    nextEnd -= 1
  }

  const deleteLen = Math.max(0, currentEnd - start + 1)
  const insertText = next.slice(start, nextEnd + 1)

  if (deleteLen > 0) {
    ytext.delete(start, deleteLen)
  }
  if (insertText.length > 0) {
    ytext.insert(start, insertText)
  }
}

const handleEditorInput = (value: string) => {
  // 先回写到受控输入，避免协作连接异常时文本框无法输入。
  editorContent.value = value
  applyLocalTextChange(value)
  scheduleAutoSave()
}

const cleanupYDoc = () => {
  if (ytext && yTextObserver) {
    ytext.unobserve(yTextObserver)
  }
  if (ydoc && yUpdateHandler) {
    ydoc.off('update', yUpdateHandler)
  }
  yTextObserver = null
  yUpdateHandler = null
  ytext = null
  if (ydoc) {
    ydoc.destroy()
  }
  ydoc = null
}

const initYDoc = (initialContent: string) => {
  cleanupYDoc()
  hasSeededInitialContent = false
  waitingInitialSyncState = false

  ydoc = new Y.Doc()
  ytext = ydoc.getText('content')
  if (initialContent) {
    ytext.insert(0, initialContent)
    hasSeededInitialContent = true
  }

  editorContent.value = ytext.toString()

  yTextObserver = () => {
    if (!ytext) return
    const latest = ytext.toString()
    if (editorContent.value !== latest) {
      editorContent.value = latest
    }
    scheduleAutoSave()
  }
  ytext.observe(yTextObserver)

  yUpdateHandler = (update, origin) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    if (origin === 'remote') return
    waitingInitialSyncState = false
    const payload = Uint8Array.from(update)
    ws.send(payload)
  }
  ydoc.on('update', yUpdateHandler)
}

const closeWs = () => {
  if (reconnectTimer != null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  if (ws) {
    ws.onopen = null
    ws.onmessage = null
    ws.onerror = null
    ws.onclose = null
    if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
      ws.close()
    }
    ws = null
  }

  wsConnected.value = false
  wsConnecting.value = false
}

const sendSyncRequest = () => {
  if (!ws || ws.readyState !== WebSocket.OPEN || !selectedDocumentId.value) return
  const payload: SyncMessage = {
    type: 'sync-request',
    docId: selectedDocumentId.value,
    ts: Date.now()
  }
  ws.send(JSON.stringify(payload))
}

const sendSyncState = () => {
  if (!ws || ws.readyState !== WebSocket.OPEN || !selectedDocumentId.value || !ydoc) return
  const update = Y.encodeStateAsUpdate(ydoc)
  const payload: SyncMessage = {
    type: 'sync-state',
    docId: selectedDocumentId.value,
    data: uint8ArrayToBase64(update),
    ts: Date.now()
  }
  ws.send(JSON.stringify(payload))
}

const applyRemoteUpdate = (update: Uint8Array) => {
  if (!ydoc) return
  Y.applyUpdate(ydoc, update, 'remote')
}

const handleWsTextMessage = (raw: string) => {
  let message: SyncMessage
  try {
    message = JSON.parse(raw) as SyncMessage
  } catch {
    return
  }

  if (!selectedDocumentId.value || message.docId !== selectedDocumentId.value) {
    return
  }

  if (message.type === 'sync-request') {
    sendSyncState()
    return
  }

  if (message.type === 'sync-state' && message.data) {
    try {
      if (waitingInitialSyncState && hasSeededInitialContent && ydoc && ytext) {
        ydoc.transact(() => {
          const len = ytext!.length
          if (len > 0) {
            ytext!.delete(0, len)
          }
        }, 'remote')
      }
      applyRemoteUpdate(base64ToUint8Array(message.data))
      waitingInitialSyncState = false
      hasSeededInitialContent = false
    } catch {
      // ignore invalid update
    }
  }
}

const handleWsBinaryMessage = async (data: Blob | ArrayBuffer) => {
  try {
    if (data instanceof Blob) {
      const buffer = await data.arrayBuffer()
      applyRemoteUpdate(new Uint8Array(buffer))
      return
    }
    applyRemoteUpdate(new Uint8Array(data))
  } catch {
    // ignore invalid binary payload
  }
}

const connectWs = (documentId: number) => {
  if (!authStore.token) {
    ElMessage.error('请先登录后再进行协作')
    return
  }

  closeWs()
  wsConnecting.value = true

  const wsUrl = `${normalizeWsBaseUrl()}/collab/${documentId}?token=${encodeURIComponent(authStore.token)}`
  ws = new WebSocket(wsUrl)
  ws.binaryType = 'arraybuffer'

  ws.onopen = () => {
    wsConnected.value = true
    wsConnecting.value = false
    reconnectAttempts = 0
    waitingInitialSyncState = hasSeededInitialContent
    sendSyncRequest()
  }

  ws.onmessage = (event) => {
    if (typeof event.data === 'string') {
      handleWsTextMessage(event.data)
      return
    }
    void handleWsBinaryMessage(event.data as Blob | ArrayBuffer)
  }

  ws.onerror = () => {
    wsConnected.value = false
    wsConnecting.value = false
  }

  ws.onclose = () => {
    wsConnected.value = false
    wsConnecting.value = false

    if (!selectedDocumentId.value || selectedDocumentId.value !== documentId) {
      return
    }

    if (reconnectAttempts >= maxReconnectAttempts) {
      return
    }

    reconnectAttempts += 1
    const delay = Math.min(4000, reconnectAttempts * 800)
    reconnectTimer = window.setTimeout(() => {
      connectWs(documentId)
    }, delay)
  }
}

const persistEditorToServer = async (showToast: boolean) => {
  if (!selectedDocumentId.value || !selectedDocument.value) return

  const safeTitle = editorTitle.value.trim() || selectedDocument.value.title || '未命名文档'
  const safeContent = editorContent.value || ''

  const updated = await collaborationService.updateDocument(selectedDocumentId.value, {
    title: safeTitle,
    content: safeContent
  })

  selectedDocument.value = updated
  const index = documents.value.findIndex((doc) => doc.id === updated.id)
  if (index >= 0) {
    documents.value[index] = updated
  }

  lastSavedAt.value = new Date().toLocaleTimeString()
  if (showToast) {
    ElMessage.success('文档已保存')
  }
}

const saveDocument = async (showToast = true) => {
  if (!selectedDocumentId.value) {
    ElMessage.warning('请先选择文档')
    return
  }

  saving.value = true
  try {
    await persistEditorToServer(showToast)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '保存失败'
    if (showToast) {
      ElMessage.error(message)
    }
  } finally {
    saving.value = false
  }
}

const teardownRealtime = async () => {
  clearAutoSaveTimer()
  closeWs()
  cleanupYDoc()
}

const resolveMemberName = (member: ProjectMember) => {
  return member.user?.nickname || member.user?.username || member.user?.email || `成员${member.id}`
}

const resolveMemberInitial = (member: ProjectMember) => {
  const name = resolveMemberName(member)
  return name.trim().charAt(0).toUpperCase()
}

const resolveRoleLabel = (role?: string) => {
  if (!role) return '成员'
  return roleLabelMap[role.toUpperCase()] || role
}

const selectDocument = async (documentId: number) => {
  await teardownRealtime()

  const document = await collaborationService.getDocumentById(documentId)
  selectedDocument.value = document
  selectedDocumentId.value = document.id

  editorTitle.value = document.title || ''
  initYDoc(document.content || '')

  connectWs(document.id)
}

const loadProject = async () => {
  if (!projectId.value) return
  project.value = await collaborationService.getProjectById(projectId.value)
}

const loadMembers = async () => {
  if (!projectId.value) return
  loadingMembers.value = true
  try {
    const page = await collaborationService.getProjectMembers(projectId.value, 0, 200)
    members.value = page.content || []
  } finally {
    loadingMembers.value = false
  }
}

const loadDocuments = async () => {
  if (!projectId.value) return

  const page = await collaborationService.getProjectDocuments(projectId.value, 0, 200)
  documents.value = page.content || []

  if (documents.value.length === 0) {
    selectedDocumentId.value = null
    selectedDocument.value = null
    editorTitle.value = ''
    editorContent.value = ''
    return
  }

  const currentId = selectedDocumentId.value
  const matched = currentId ? documents.value.find((doc) => doc.id === currentId) : null
  const target = matched || documents.value[0]
  await selectDocument(target.id)
}

const createDocument = async () => {
  if (!projectId.value) return
  if (!newDocTitle.value.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }

  creating.value = true
  try {
    const created = await collaborationService.createDocument({
      projectId: projectId.value,
      title: newDocTitle.value.trim(),
      documentType: newDocType.value
    })

    showCreateDialog.value = false
    newDocTitle.value = ''
    newDocType.value = 'TEXT'

    await loadDocuments()
    await selectDocument(created.id)
    ElMessage.success('文档创建成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '创建文档失败'
    ElMessage.error(message)
  } finally {
    creating.value = false
  }
}

const inviteMember = async () => {
  if (!projectId.value) return
  const email = newMemberEmail.value.trim()
  if (!email) {
    ElMessage.warning('请输入成员邮箱')
    return
  }

  invitingMember.value = true
  try {
    await collaborationService.addProjectMember(projectId.value, {
      email,
      role: newMemberRole.value
    })
    newMemberEmail.value = ''
    newMemberRole.value = 'MEMBER'
    await Promise.all([loadProject(), loadMembers()])
    ElMessage.success('已添加协作人')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '添加协作人失败'
    ElMessage.error(message)
  } finally {
    invitingMember.value = false
  }
}

const removeMember = async (member: ProjectMember) => {
  if (!projectId.value) return
  if (!member.id) return

  try {
    await ElMessageBox.confirm(`确认移除 ${resolveMemberName(member)} 吗？`, '移除协作人', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })

    removingMemberId.value = member.id
    await collaborationService.removeProjectMember(projectId.value, member.id)
    await Promise.all([loadProject(), loadMembers()])
    ElMessage.success('协作人已移除')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      const message = error?.response?.data?.message || error?.message || '移除协作人失败'
      ElMessage.error(message)
    }
  } finally {
    removingMemberId.value = null
  }
}

const removeDocument = async () => {
  if (!selectedDocumentId.value) return

  try {
    await ElMessageBox.confirm('确认删除当前文档？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await collaborationService.deleteDocument(selectedDocumentId.value)
    await loadDocuments()
    ElMessage.success('文档已删除')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      const message = error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(message)
    }
  }
}

const loadAll = async () => {
  if (!projectId.value) {
    ElMessage.error('无效项目ID')
    router.push('/dashboard/collaboration')
    return
  }

  pageLoading.value = true
  try {
    await Promise.all([loadProject(), loadMembers()])
    await loadDocuments()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载协作工作区失败'
    ElMessage.error(message)
  } finally {
    pageLoading.value = false
  }
}

watch(
  () => route.params.projectId,
  async () => {
    await teardownRealtime()
    await loadAll()
  }
)

onMounted(async () => {
  await loadAll()
})

onBeforeUnmount(async () => {
  await teardownRealtime()
})
</script>

<template>
  <div class="collab-page" v-loading="pageLoading">
    <header class="top-bar">
      <div class="headline-wrap">
        <h1 class="title">{{ project?.projectName || '协作文档' }}</h1>
        <div class="meta-strip">
          <span class="meta-chip">文档 {{ documents.length }}</span>
          <span class="meta-chip">协作人 {{ members.length }}</span>
        </div>
      </div>
      <div class="actions">
        <el-tag :type="wsConnected ? 'success' : 'warning'" effect="light" round>
          <el-icon class="el-icon--left"><Connection /></el-icon>{{ realtimeStatus }}
        </el-tag>
        <el-button round @click="loadAll">
          <el-icon class="el-icon--left"><Refresh /></el-icon>刷新
        </el-button>
        <el-button type="primary" round @click="showCreateDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>新建文档
        </el-button>
      </div>
    </header>

    <div class="layout">
      <aside class="sidebar">
        <section class="side-card">
          <div class="panel-title-row">
            <div class="panel-title">文档列表</div>
            <span class="panel-count">{{ documents.length }}</span>
          </div>
          <div class="doc-list">
            <button
              v-for="doc in documents"
              :key="doc.id"
              class="doc-item"
              :class="{ active: doc.id === selectedDocumentId }"
              @click="selectDocument(doc.id)"
            >
              <div class="doc-name">{{ doc.title }}</div>
              <div class="doc-meta">{{ doc.documentType || 'TEXT' }}</div>
            </button>
            <el-empty v-if="documents.length === 0" description="暂无文档" :image-size="56" />
          </div>
        </section>

        <section class="side-card members-card" v-loading="loadingMembers">
          <div class="panel-title-row">
            <div class="panel-title">协作人</div>
            <span class="panel-count">{{ members.length }}</span>
          </div>

          <div class="invite-row" v-if="canManageMembers">
            <el-input v-model="newMemberEmail" placeholder="输入邮箱邀请协作人" />
            <el-select v-model="newMemberRole" style="width: 124px">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="成员" value="MEMBER" />
              <el-option label="编辑" value="EDITOR" />
              <el-option label="只读" value="VIEWER" />
            </el-select>
            <el-button type="primary" :loading="invitingMember" @click="inviteMember">邀请</el-button>
          </div>

          <div class="member-list">
            <div class="member-item" v-for="member in members" :key="member.id">
              <div class="avatar-dot">{{ resolveMemberInitial(member) }}</div>
              <div class="member-text">
                <div class="member-name">{{ resolveMemberName(member) }}</div>
                <div class="member-role">{{ resolveRoleLabel(member.role) }}</div>
              </div>
              <el-button
                v-if="canManageMembers && (member.role || '').toUpperCase() !== 'OWNER'"
                link
                type="danger"
                :loading="removingMemberId === member.id"
                @click="removeMember(member)"
              >
                移除
              </el-button>
            </div>
            <el-empty v-if="members.length === 0" description="暂无协作人" :image-size="56" />
          </div>
        </section>
      </aside>

      <main class="editor-panel">
        <div v-if="selectedDocumentId" class="editor-wrap">
          <div class="editor-toolbar">
            <el-input
              v-model="editorTitle"
              placeholder="文档标题"
              class="title-input"
            />
            <div class="toolbar-actions">
              <span class="save-tip">{{ lastSavedAt ? `最近保存：${lastSavedAt}` : '尚未保存' }}</span>
              <el-button type="danger" plain round @click="removeDocument">
                <el-icon class="el-icon--left"><Delete /></el-icon>删除
              </el-button>
              <el-button type="primary" :loading="saving" round @click="saveDocument(true)">
                保存
              </el-button>
            </div>
          </div>

          <el-input
            :model-value="editorContent"
            type="textarea"
            :rows="24"
            resize="none"
            placeholder="开始协作编辑..."
            class="editor-textarea"
            @update:model-value="handleEditorInput"
          />
        </div>

        <div v-else class="empty">
          <el-empty description="请先新建或选择文档" />
        </div>
      </main>
    </div>

    <el-dialog v-model="showCreateDialog" title="新建文档" width="460px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="newDocTitle" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="newDocType" style="width: 100%">
            <el-option label="文本" value="TEXT" />
            <el-option label="Markdown" value="MARKDOWN" />
            <el-option label="Wiki" value="WIKI" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createDocument">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.collab-page {
  min-height: calc(100vh - 64px);
  background:
    radial-gradient(circle at 0% 0%, rgba(15, 118, 110, 0.18), transparent 34%),
    radial-gradient(circle at 100% 100%, rgba(234, 88, 12, 0.16), transparent 30%),
    linear-gradient(180deg, #eef2ff 0%, #f8fafc 42%, #f1f5f9 100%);
  padding: 20px;
  font-family: 'Source Han Sans CN', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  padding: 18px 20px;
}

.headline-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.subtitle {
  margin: 0;
  color: #475569;
  font-size: 13px;
}

.meta-strip {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-chip {
  font-size: 12px;
  border-radius: 999px;
  padding: 2px 10px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.13);
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: calc(100vh - 170px);
}

.editor-panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 16px 36px rgba(2, 132, 199, 0.08);
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  padding: 12px;
}

.members-card {
  min-height: 280px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel-count {
  font-size: 12px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.13);
  border-radius: 999px;
  padding: 2px 8px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  max-height: 40vh;
}

.doc-item {
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  border-radius: 10px;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.doc-item:hover {
  transform: translateY(-1px);
  border-color: #38bdf8;
  box-shadow: 0 10px 20px rgba(56, 189, 248, 0.12);
}

.doc-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.doc-name {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.doc-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.invite-row {
  display: grid;
  grid-template-columns: 1fr 124px auto;
  gap: 8px;
  margin-bottom: 10px;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 30vh;
  overflow: auto;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
  background: #f8fafc;
}

.avatar-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #0c4a6e;
  background: linear-gradient(135deg, #bae6fd, #bbf7d0);
}

.member-text {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-role {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.editor-panel {
  padding: 14px;
}

.editor-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.editor-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.title-input {
  flex: 1;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-tip {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

:deep(.editor-textarea textarea) {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  line-height: 1.7;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 12px;
}

.empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .top-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .invite-row {
    grid-template-columns: 1fr;
  }
}
</style>
