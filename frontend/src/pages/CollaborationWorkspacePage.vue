<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import collaborationService from '@/services/collaboration'
import collaborationRealtime from '@/services/collaborationRealtime'
import UiStatCard from '@/components/ui/UiStatCard.vue'
import type {
  CollaborationComment,
  CollaborationProject,
  CollaborativeDocument,
  ProjectMember,
  ProjectStatistics,
  RealtimeCollaborator,
  RealtimeDocumentBlock,
  RealtimeDocumentState,
  RealtimeWsMessage
} from '@/types/collaboration'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = computed(() => Number(route.params.projectId))

const pageLoading = ref(false)
const savingDoc = ref(false)
const creatingDoc = ref(false)
const addingComment = ref(false)
const loadingMembers = ref(false)
const invitingMember = ref(false)
const realtimeConnecting = ref(false)
const realtimeConnected = ref(false)
const realtimeSyncing = ref(false)
const sendingRealtimeChat = ref(false)

const project = ref<CollaborationProject | null>(null)
const projectStats = ref<ProjectStatistics | null>(null)
const documents = ref<CollaborativeDocument[]>([])
const selectedDocument = ref<CollaborativeDocument | null>(null)
const comments = ref<CollaborationComment[]>([])
const members = ref<ProjectMember[]>([])
const realtimeCollaborators = ref<RealtimeCollaborator[]>([])
const realtimeState = ref<RealtimeDocumentState | null>(null)
const documentBlocks = ref<RealtimeDocumentBlock[]>([])
const realtimeChatMessages = ref<Array<{ id: number; userName: string; content: string; timestamp: number; self: boolean }>>([])
const realtimeChatInput = ref('')
const activeRealtimeDocumentId = ref<number | null>(null)
const pendingFocusAfterBlockId = ref<number | null>(null)

const blockInputTimers = new Map<number, number>()
const blockInputRefs = new Map<number, any>()
const blockShadowContent = new Map<number, string>()

let collaboratorPollingTimer: number | null = null
let realtimeEditTimer: number | null = null
let suppressRealtimeSubmit = false

const docForm = reactive({
  title: '',
  content: ''
})

const showCreateDocDialog = ref(false)
const createDocForm = reactive({
  title: '',
  documentType: 'TEXT' as 'TEXT' | 'MARKDOWN' | 'WIKI'
})

const memberForm = reactive({
  email: '',
  role: 'MEMBER' as 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER' | 'EDITOR'
})

const newComment = ref('')

const canEditCurrent = computed(() => Boolean(selectedDocument.value?.canEdit))
const hasSelectedDocument = computed(() => Boolean(selectedDocument.value?.id))
const currentUserId = computed(() => Number(authStore.user?.id || 0))
const sortedBlocks = computed(() => [...documentBlocks.value].sort((a, b) => a.orderIndex - b.orderIndex))
const lastBlockId = computed(() => {
  if (!sortedBlocks.value.length) return null
  return sortedBlocks.value[sortedBlocks.value.length - 1].id
})
const realtimeStatusText = computed(() => {
  if (realtimeConnected.value) return '实时在线'
  if (realtimeConnecting.value) return '连接中'
  return '离线'
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatRealtimeTime = (timestamp?: number) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleTimeString()
}

const pushRealtimeChat = (payload: { userName?: string; content?: string; timestamp?: number; self?: boolean }) => {
  if (!payload.content) return

  realtimeChatMessages.value.push({
    id: Date.now() + Math.floor(Math.random() * 1000),
    userName: payload.userName || '系统',
    content: payload.content,
    timestamp: payload.timestamp || Date.now(),
    self: Boolean(payload.self)
  })

  if (realtimeChatMessages.value.length > 80) {
    realtimeChatMessages.value = realtimeChatMessages.value.slice(-80)
  }
}

const stopCollaboratorPolling = () => {
  if (collaboratorPollingTimer != null) {
    window.clearInterval(collaboratorPollingTimer)
    collaboratorPollingTimer = null
  }
}

const startCollaboratorPolling = (documentId: number) => {
  stopCollaboratorPolling()
  collaboratorPollingTimer = window.setInterval(() => {
    void loadRealtimeCollaborators(documentId)
  }, 10000)
}

const loadRealtimeCollaborators = async (documentId?: number) => {
  const targetDocumentId = documentId || selectedDocument.value?.id
  if (!targetDocumentId) return

  try {
    realtimeCollaborators.value = await collaborationService.getRealtimeCollaborators(targetDocumentId)
  } catch {
    realtimeCollaborators.value =[]
  }
}

const applyRealtimeStateToEditor = (state: RealtimeDocumentState) => {
  realtimeState.value = state
  if (!selectedDocument.value || selectedDocument.value.id !== state.documentId) return

  const nextContent = state.content || ''
  if (docForm.content === nextContent) return

  suppressRealtimeSubmit = true
  docForm.content = nextContent
  suppressRealtimeSubmit = false
  selectedDocument.value.content = nextContent
}

const mergeBlocksToContent = () => sortedBlocks.value.map((block) => block.content || '').join('\n\n')

const isLockedByOther = (block: RealtimeDocumentBlock) => {
  return Boolean(block.lockedByUserId && block.lockedByUserId !== currentUserId.value)
}

const isLockedBySelf = (block: RealtimeDocumentBlock) => {
  return Boolean(block.lockedByUserId && block.lockedByUserId === currentUserId.value)
}

const formatLockDuration = (lockedAt?: number) => {
  if (!lockedAt) return ''
  const elapsed = Date.now() - lockedAt
  if (elapsed < 1000) return '刚刚'
  if (elapsed < 60_000) return `${Math.floor(elapsed / 1000)} 秒前`
  if (elapsed < 3_600_000) return `${Math.floor(elapsed / 60_000)} 分钟前`
  return formatRealtimeTime(lockedAt)
}

const getBlockLockTone = (block: RealtimeDocumentBlock) => {
  if (!block.lockedByUserId) return 'idle'
  if (isLockedBySelf(block)) return 'self'
  return 'other'
}

const getBlockLockText = (block: RealtimeDocumentBlock) => {
  if (!block.lockedByUserId) return '可编辑'
  if (isLockedBySelf(block)) return '你正在编辑'
  return `${block.lockedUserName || `用户${block.lockedByUserId}`} 正在编辑`
}

const getBlockLockDetail = (block: RealtimeDocumentBlock) => {
  if (!block.lockedByUserId) return '未上锁'
  const duration = formatLockDuration(block.lockedAt)
  if (!duration) return isLockedBySelf(block) ? '你已持有锁' : '协作者已持有锁'
  return isLockedBySelf(block) ? `持有于 ${duration}` : `锁定于 ${duration}`
}

const updateBlockShadow = (block: RealtimeDocumentBlock) => {
  blockShadowContent.set(block.id, block.content || '')
}

const getCurrentBlockById = (blockId: number) => {
  return documentBlocks.value.find((item) => item.id === blockId) || null
}

const upsertBlock = (incoming: RealtimeDocumentBlock) => {
  const index = documentBlocks.value.findIndex((block) => block.id === incoming.id)
  if (index >= 0) {
    const current = documentBlocks.value[index]
    const persistedContent = blockShadowContent.get(incoming.id) ?? current.content ?? ''
    const hasLocalChanges = (current.content || '') !== persistedContent
    const shouldPreserveLocalContent = hasLocalChanges && (incoming.content || '') === persistedContent

    documentBlocks.value[index] = {
      ...current,
      ...incoming,
      content: shouldPreserveLocalContent ? current.content : (incoming.content || '')
    }
  } else {
    documentBlocks.value.push(incoming)
  }
  updateBlockShadow(incoming)
  docForm.content = mergeBlocksToContent()
  if (selectedDocument.value) {
    selectedDocument.value.content = docForm.content
  }
}

const persistBlock = async (blockId: number) => {
  const documentId = selectedDocument.value?.id
  if (!documentId) return null

  let latestBlock = getCurrentBlockById(blockId)
  if (!latestBlock || latestBlock.id <= 0) {
    return latestBlock
  }

  if (isLockedByOther(latestBlock)) {
    throw new Error('该段落正在被其他协作者编辑，请先同步')
  }

  if (!isLockedBySelf(latestBlock)) {
    const locked = await collaborationService.lockRealtimeBlock(documentId, blockId)
    upsertBlock(locked)
    latestBlock = getCurrentBlockById(blockId)
  }

  if (!latestBlock) {
    return null
  }

  const updated = await collaborationService.updateRealtimeBlock(documentId, blockId, {
    content: latestBlock.content || '',
    version: latestBlock.version
  })
  upsertBlock(updated)
  return updated
}

const flushPendingBlockUpdate = async (blockId: number, releaseAfterPersist = false) => {
  const timer = blockInputTimers.get(blockId)
  if (timer != null) {
    window.clearTimeout(timer)
    blockInputTimers.delete(blockId)
  }

  try {
    const block = getCurrentBlockById(blockId)
    if (!block) {
      return
    }
    const persistedContent = blockShadowContent.get(blockId) || ''
    if ((block.content || '') !== persistedContent) {
      await persistBlock(blockId)
    }
  } finally {
    if (releaseAfterPersist) {
      const latestBlock = getCurrentBlockById(blockId)
      if (latestBlock?.lockedByUserId === currentUserId.value) {
        await unlockBlock(blockId)
      }
    }
  }
}

const flushAllBlockUpdates = async () => {
  for (const block of sortedBlocks.value) {
    await flushPendingBlockUpdate(block.id)
  }
}

const loadRealtimeBlocks = async (documentId?: number) => {
  const targetDocumentId = documentId || selectedDocument.value?.id
  if (!targetDocumentId) return

  try {
    const blocks = await collaborationService.getRealtimeBlocks(targetDocumentId)
    
    if (!blocks || blocks.length === 0) {
      // Auto-initialize blocks for documents that don't have any
      console.warn(`Document ${targetDocumentId} has no blocks, attempting auto-initialization...`)
      const initialized = await collaborationService.initializeRealtimeBlocks(targetDocumentId, { content: selectedDocument.value?.content || '' })
      documentBlocks.value = Array.isArray(initialized) ? initialized : []
    } else {
      documentBlocks.value = [...blocks].sort((a, b) => a.orderIndex - b.orderIndex)
    }
    
    blockShadowContent.clear()
    documentBlocks.value.forEach((block) => updateBlockShadow(block))

    docForm.content = mergeBlocksToContent()
    if (selectedDocument.value) {
      selectedDocument.value.content = docForm.content
    }
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载文档段落失败'
    ElMessage.error(message)
    // Create fallback single block to allow editing
    if (!documentBlocks.value || documentBlocks.value.length === 0) {
      documentBlocks.value = [{
        id: -Math.floor(Math.random() * 1e9),
        documentId: targetDocumentId,
        content: selectedDocument.value?.content || '',
        orderIndex: 0,
        version: 1
      }]
    }
  }
}

const registerBlockInputRef = (blockId: number, instance: any) => {
  if (!instance) {
    blockInputRefs.delete(blockId)
    return
  }
  blockInputRefs.set(blockId, instance)
}

const focusOnBlock = async (blockId: number) => {
  await nextTick()
  const inputRef = blockInputRefs.get(blockId)
  const innerTextarea = inputRef?.textarea || inputRef?.$el?.querySelector?.('textarea')
  if (innerTextarea?.focus) {
    innerTextarea.focus()
  }
}

const createNewBlockBelow = async (afterBlockId: number) => {
  const documentId = selectedDocument.value?.id
  if (!documentId) return null

  pendingFocusAfterBlockId.value = afterBlockId
  try {
    const created = await collaborationService.createRealtimeBlock(documentId, { afterBlockId })
    upsertBlock(created)
    await focusOnBlock(created.id)
    return created
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '新增段落失败'
    ElMessage.error(message)
    return null
  } finally {
    pendingFocusAfterBlockId.value = null
  }
}

const lockBlock = async (blockId: number) => {
  const documentId = selectedDocument.value?.id
  if (!documentId) return
  try {
    const locked = await collaborationService.lockRealtimeBlock(documentId, blockId)
    upsertBlock(locked)
  } catch {
    // lock conflict handled by ws/next sync
  }
}

const unlockBlock = async (blockId: number) => {
  const documentId = selectedDocument.value?.id
  if (!documentId) return
  try {
    const unlocked = await collaborationService.unlockRealtimeBlock(documentId, blockId)
    upsertBlock(unlocked)
  } catch {
    // ignore unlock failures on focus switch
  }
}

const onBlockFocus = (block: RealtimeDocumentBlock) => {
  if (!canEditCurrent.value || isLockedByOther(block)) return
  void lockBlock(block.id)
}

const onBlockBlur = (block: RealtimeDocumentBlock) => {
  if (!canEditCurrent.value) return
  void flushPendingBlockUpdate(block.id, true)
}

const onBlockInput = (block: RealtimeDocumentBlock) => {
  if (!canEditCurrent.value) return

  if (isLockedByOther(block)) {
    block.content = blockShadowContent.get(block.id) || ''
    void createNewBlockBelow(block.id)
    ElMessage.info('检测到协作冲突，已为您创建新段落')
    return
  }

  const timer = blockInputTimers.get(block.id)
  if (timer != null) {
    window.clearTimeout(timer)
  }

  const nextTimer = window.setTimeout(async () => {
    try {
      await persistBlock(block.id)
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || '更新段落失败，已自动同步'
      ElMessage.warning(message)
      const documentId = selectedDocument.value?.id
      if (documentId) {
        await loadRealtimeBlocks(documentId)
      }
    } finally {
      blockInputTimers.delete(block.id)
    }
  }, 500)

  blockInputTimers.set(block.id, nextTimer)
}

const onBlockEnter = (block: RealtimeDocumentBlock, event: KeyboardEvent) => {
  if (!canEditCurrent.value || isLockedByOther(block)) return
  if (event.isComposing) return
  void createNewBlockBelow(block.id)
}

const syncRealtimeState = async (documentId?: number) => {
  const targetDocumentId = documentId || selectedDocument.value?.id
  if (!targetDocumentId) return

  realtimeSyncing.value = true
  try {
    const state = await collaborationService.syncRealtimeState(targetDocumentId)
    applyRealtimeStateToEditor(state)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '同步实时状态失败'
    ElMessage.error(message)
  } finally {
    realtimeSyncing.value = false
  }
}

const handleRealtimeWsMessage = (message: RealtimeWsMessage) => {
  if (message.type === 'ERROR') {
    ElMessage.error(message.content || '实时协作消息异常')
    return
  }

  if (message.type === 'SYSTEM') {
    if (message.status === 'connected') {
      realtimeConnected.value = true
      realtimeConnecting.value = false
    }
    return
  }

  if (message.type === 'USER_JOIN' || message.type === 'USER_LEAVE') {
    void loadRealtimeCollaborators(activeRealtimeDocumentId.value || undefined)
    const actionText = message.type === 'USER_JOIN' ? '加入了协作' : '离开了协作'
    pushRealtimeChat({
      userName: '系统',
      content: `${message.userName || message.userId || '用户'}${actionText}`
    })
    return
  }

  if ((message.type === 'DOCUMENT_STATE' || message.type === 'DOCUMENT_UPDATED') && message.content) {
    try {
      const parsed = JSON.parse(message.content) as RealtimeDocumentState
      if (parsed?.documentId) {
        applyRealtimeStateToEditor(parsed)
      }
    } catch {
      // ignore invalid state payload
    }
    return
  }

  if (message.type === 'EDIT_OPERATION') {
    void syncRealtimeState(activeRealtimeDocumentId.value || undefined)
    return
  }

  if (
    message.type === 'BLOCK_LOCKED' ||
    message.type === 'BLOCK_UNLOCKED' ||
    message.type === 'BLOCK_UPDATED' ||
    message.type === 'BLOCK_CREATED'
  ) {
    if (message.content) {
      try {
        const parsed = JSON.parse(message.content) as RealtimeDocumentBlock
        if (parsed?.id) {
          upsertBlock(parsed)
          if (
            message.type === 'BLOCK_CREATED' &&
            pendingFocusAfterBlockId.value != null &&
            message.afterBlockId === pendingFocusAfterBlockId.value
          ) {
            void focusOnBlock(parsed.id)
          }
        }
      } catch {
        // ignore invalid block payload
      }
    }
    return
  }

  if (message.type === 'CHAT_MESSAGE') {
    pushRealtimeChat({
      userName: message.userName || '协作者',
      content: message.content,
      timestamp: message.timestamp,
      self: message.userId === String(authStore.user?.id || '')
    })
  }
}

const stopRealtimeCollaboration = async () => {
  const previousDocumentId = activeRealtimeDocumentId.value

  if (realtimeEditTimer != null) {
    window.clearTimeout(realtimeEditTimer)
    realtimeEditTimer = null
  }

  if (documentBlocks.value.length > 0) {
    try {
      await flushAllBlockUpdates()
    } catch {
      // ignore pending block flush errors during teardown
    }
  }

  blockInputTimers.forEach((timerId) => window.clearTimeout(timerId))
  blockInputTimers.clear()
  blockInputRefs.clear()
  blockShadowContent.clear()

  stopCollaboratorPolling()
  collaborationRealtime.disconnect()

  if (previousDocumentId) {
    try {
      await collaborationService.leaveRealtime(previousDocumentId)
    } catch {
      // ignore leave errors
    }
  }

  activeRealtimeDocumentId.value = null
  realtimeConnected.value = false
  realtimeConnecting.value = false
  realtimeCollaborators.value =[]
  realtimeState.value = null
  documentBlocks.value = []
  realtimeChatMessages.value =[]
  realtimeChatInput.value = ''
}

const startRealtimeCollaboration = async (documentId: number) => {
  if (!authStore.token) return

  if (activeRealtimeDocumentId.value === documentId && (realtimeConnected.value || realtimeConnecting.value)) {
    return
  }

  await stopRealtimeCollaboration()
  realtimeConnecting.value = true

  try {
    const session = await collaborationService.initializeRealtimeSession(documentId)
    await collaborationService.joinRealtime(documentId)

    activeRealtimeDocumentId.value = documentId
    realtimeState.value = session.initialState || null

    if (session.initialState) {
      applyRealtimeStateToEditor(session.initialState)
    }

    realtimeCollaborators.value = session.collaborators ||[]

    collaborationRealtime.connect({
      token: authStore.token,
      documentId,
      userName: authStore.user?.username,
      onOpen: () => {
        realtimeConnected.value = true
        realtimeConnecting.value = false
        collaborationRealtime.requestSync()
      },
      onClose: () => {
        realtimeConnected.value = false
      },
      onError: () => {
        realtimeConnected.value = false
      },
      onMessage: handleRealtimeWsMessage
    })

    await loadRealtimeCollaborators(documentId)
    await loadRealtimeBlocks(documentId)
    startCollaboratorPolling(documentId)
  } catch (error: any) {
    realtimeConnecting.value = false
    realtimeConnected.value = false
    const message = error?.response?.data?.message || error?.message || '开启实时协作失败'
    ElMessage.warning(message)
  }
}

const buildRealtimePatchPayload = (
  previousContent: string,
  currentContent: string,
  version?: number
) => {
  if (previousContent === currentContent) return null

  const previousLength = previousContent.length
  const currentLength = currentContent.length

  let prefixLength = 0
  while (
    prefixLength < previousLength &&
    prefixLength < currentLength &&
    previousContent.charCodeAt(prefixLength) === currentContent.charCodeAt(prefixLength)
  ) {
    prefixLength += 1
  }

  let previousSuffix = previousLength - 1
  let currentSuffix = currentLength - 1
  while (
    previousSuffix >= prefixLength &&
    currentSuffix >= prefixLength &&
    previousContent.charCodeAt(previousSuffix) === currentContent.charCodeAt(currentSuffix)
  ) {
    previousSuffix -= 1
    currentSuffix -= 1
  }

  const removedLength = Math.max(0, previousSuffix - prefixLength + 1)
  const insertedContent = currentSuffix >= prefixLength ? currentContent.slice(prefixLength, currentSuffix + 1) : ''

  if (removedLength === 0 && insertedContent.length > 0) {
    return {
      type: 'insert' as const,
      position: prefixLength,
      content: insertedContent,
      version,
      timestamp: Date.now()
    }
  }

  if (removedLength > 0 && insertedContent.length === 0) {
    return {
      type: 'delete' as const,
      position: prefixLength,
      length: removedLength,
      version,
      timestamp: Date.now()
    }
  }

  return {
    type: 'update' as const,
    position: prefixLength,
    length: removedLength,
    content: insertedContent,
    version,
    timestamp: Date.now()
  }
}

const submitRealtimeEdit = async () => {
  const documentId = selectedDocument.value?.id
  if (!documentId || activeRealtimeDocumentId.value !== documentId) return
  if (!canEditCurrent.value) return

  const currentContent = docForm.content || ''
  const previousContent = realtimeState.value?.content ?? selectedDocument.value?.content ?? ''

  if (currentContent === previousContent) return

  const payload = buildRealtimePatchPayload(
    previousContent,
    currentContent,
    realtimeState.value?.version
  )
  if (!payload) return

  try {
    const result = await collaborationService.submitRealtimeEdit(documentId, payload)

    if (!result.success) {
      await syncRealtimeState(documentId)
      return
    }

    if (result.newState) {
      realtimeState.value = result.newState
      if (selectedDocument.value) {
        selectedDocument.value.content = result.newState.content || ''
      }
    }

  } catch {
    await syncRealtimeState(documentId)
  }
}

const onEditorInput = () => {
  if (suppressRealtimeSubmit) return
  if (!selectedDocument.value?.id || !realtimeConnected.value) return

  if (realtimeEditTimer != null) {
    window.clearTimeout(realtimeEditTimer)
  }

  realtimeEditTimer = window.setTimeout(() => {
    void submitRealtimeEdit()
  }, 400)
}

const sendRealtimeChat = async () => {
  const documentId = selectedDocument.value?.id
  if (!documentId || activeRealtimeDocumentId.value !== documentId || !realtimeConnected.value) {
    ElMessage.warning('当前文档未连接实时协作')
    return
  }

  const content = realtimeChatInput.value.trim()
  if (!content) {
    ElMessage.warning('聊天内容不能为空')
    return
  }

  sendingRealtimeChat.value = true
  try {
    const sent = await collaborationService.sendRealtimeChat(documentId, {
      content,
      messageType: 'text'
    })
    if (!sent) {
      ElMessage.error('发送实时消息失败')
      return
    }
    collaborationRealtime.sendChat(content)
    realtimeChatInput.value = ''
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '发送实时消息失败'
    ElMessage.error(message)
  } finally {
    sendingRealtimeChat.value = false
  }
}

const normalizeProjectId = () => {
  if (!Number.isFinite(projectId.value) || projectId.value <= 0) {
    ElMessage.error('无效的项目 ID')
    router.push('/dashboard/collaboration')
    return false
  }
  return true
}

const loadProject = async () => {
  project.value = await collaborationService.getProjectById(projectId.value)
}

const loadProjectStats = async () => {
  try {
    projectStats.value = await collaborationService.getProjectStatistics(projectId.value)
  } catch {
    projectStats.value = null
  }
}

const loadDocuments = async () => {
  const result = await collaborationService.getProjectDocuments(projectId.value, 0, 100)
  documents.value = result.content ||[]

  if (documents.value.length === 0) {
    await stopRealtimeCollaboration()
    selectedDocument.value = null
    docForm.title = ''
    docForm.content = ''
    documentBlocks.value = []
    comments.value =[]
    return
  }

  const currentId = selectedDocument.value?.id
  const matched = documents.value.find((doc) => doc.id === currentId)
  const target = matched || documents.value[0]
  await selectDocument(target.id)
}

const loadMembers = async () => {
  loadingMembers.value = true
  try {
    const result = await collaborationService.getProjectMembers(projectId.value, 0, 100)
    members.value = result.content ||[]
  } finally {
    loadingMembers.value = false
  }
}

const loadComments = async (documentId: number) => {
  const result = await collaborationService.getDocumentComments(documentId, 0, 200)
  comments.value = result.content ||[]
}

const selectDocument = async (documentId: number) => {
  if (activeRealtimeDocumentId.value && activeRealtimeDocumentId.value !== documentId) {
    await stopRealtimeCollaboration()
  }

  const document = await collaborationService.getDocumentById(documentId)
  selectedDocument.value = document
  docForm.title = document.title || ''
  docForm.content = document.content || ''
  await loadComments(documentId)
  await startRealtimeCollaboration(documentId)
}

const loadAll = async () => {
  if (!normalizeProjectId()) return

  pageLoading.value = true
  try {
    await Promise.all([loadProject(), loadProjectStats(), loadMembers()])
    await loadDocuments()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '加载协作工作区失败'
    ElMessage.error(message)
  } finally {
    pageLoading.value = false
  }
}

const saveDocument = async () => {
  if (!selectedDocument.value?.id) {
    ElMessage.warning('请先选择文档')
    return
  }
  if (!docForm.title.trim()) {
    ElMessage.warning('文档标题不能为空')
    return
  }

  savingDoc.value = true
  try {
    await flushAllBlockUpdates()
    const mergedContent = mergeBlocksToContent()
    const updated = await collaborationService.updateDocument(selectedDocument.value.id, {
      title: docForm.title.trim()
    })
    selectedDocument.value = updated
    if (realtimeState.value) {
      realtimeState.value = {
        ...realtimeState.value,
        content: mergedContent,
        lastModified: Date.now()
      }
    }
    docForm.content = mergedContent
    ElMessage.success('文档已保存')
    await loadDocuments()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '保存文档失败'
    ElMessage.error(message)
  } finally {
    savingDoc.value = false
  }
}

const toggleLock = async () => {
  if (!selectedDocument.value?.id) return

  try {
    const changed = selectedDocument.value.isLocked
      ? await collaborationService.unlockDocument(selectedDocument.value.id)
      : await collaborationService.lockDocument(selectedDocument.value.id)
    selectedDocument.value = changed
    ElMessage.success(changed.isLocked ? '文档已锁定' : '文档已解锁')
    await loadDocuments()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '切换锁定状态失败'
    ElMessage.error(message)
  }
}

const openCreateDocDialog = () => {
  createDocForm.title = ''
  createDocForm.documentType = 'TEXT'
  showCreateDocDialog.value = true
}

const submitCreateDocument = async () => {
  if (!createDocForm.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }

  creatingDoc.value = true
  try {
    const created = await collaborationService.createDocument({
      projectId: projectId.value,
      title: createDocForm.title.trim(),
      documentType: createDocForm.documentType
    })
    ElMessage.success('文档创建成功')
    showCreateDocDialog.value = false
    await loadDocuments()
    await selectDocument(created.id)
    await loadProjectStats()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '创建文档失败'
    ElMessage.error(message)
  } finally {
    creatingDoc.value = false
  }
}

const removeDocument = async () => {
  if (!selectedDocument.value?.id) return

  try {
    await ElMessageBox.confirm('确认删除当前文档？该操作不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await collaborationService.deleteDocument(selectedDocument.value.id)
    ElMessage.success('文档已删除')
    await loadDocuments()
    await loadProjectStats()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      const message = error?.response?.data?.message || error?.message || '删除文档失败'
      ElMessage.error(message)
    }
  }
}

const submitComment = async () => {
  if (!selectedDocument.value?.id) {
    ElMessage.warning('请先选择文档')
    return
  }
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  addingComment.value = true
  try {
    await collaborationService.addComment(selectedDocument.value.id, {
      content: newComment.value.trim()
    })
    newComment.value = ''
    await loadComments(selectedDocument.value.id)
    await loadDocuments()
    ElMessage.success('评论已发布')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '发布评论失败'
    ElMessage.error(message)
  } finally {
    addingComment.value = false
  }
}

const removeComment = async (comment: CollaborationComment) => {
  try {
    await collaborationService.deleteComment(comment.id)
    ElMessage.success('评论已删除')
    if (selectedDocument.value?.id) {
      await loadComments(selectedDocument.value.id)
      await loadDocuments()
    }
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '删除评论失败'
    ElMessage.error(message)
  }
}

const addMember = async () => {
  if (!memberForm.email.trim()) {
    ElMessage.warning('请输入成员邮箱')
    return
  }

  invitingMember.value = true
  try {
    await collaborationService.addProjectMember(projectId.value, {
      email: memberForm.email.trim(),
      role: memberForm.role
    })
    memberForm.email = ''
    memberForm.role = 'MEMBER'
    await Promise.all([loadMembers(), loadProject(), loadProjectStats()])
    ElMessage.success('成员添加成功')
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '添加成员失败'
    ElMessage.error(message)
  } finally {
    invitingMember.value = false
  }
}

const removeMember = async (member: ProjectMember) => {
  try {
    await collaborationService.removeProjectMember(projectId.value, member.id)
    ElMessage.success('成员已移除')
    await Promise.all([loadMembers(), loadProject(), loadProjectStats()])
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '移除成员失败'
    ElMessage.error(message)
  }
}

watch(
  () => route.params.projectId,
  async () => {
    await stopRealtimeCollaboration()
    await loadAll()
  }
)

onMounted(async () => {
  await loadAll()
})

onBeforeUnmount(async () => {
  await stopRealtimeCollaboration()
})
</script>

<template>
  <div class="modern-collab-app" v-loading="pageLoading">
    <!-- 顶部工作区 Header -->
    <header class="app-header base-card">
      <div class="header-main">
        <div class="header-intro">
          <span class="kicker-tag">Workspace</span>
          <h1 class="project-name">{{ project?.projectName || '协作工作区' }}</h1>
        </div>
        <div class="header-actions">
          <el-button round @click="router.push('/dashboard/collaboration')">
            <el-icon class="el-icon--left"><Back /></el-icon>返回列表
          </el-button>
          <el-button type="primary" color="#6366f1" round @click="openCreateDocDialog">
            <el-icon class="el-icon--left"><Plus /></el-icon>新建文档
          </el-button>
          <el-button round :loading="realtimeSyncing" :disabled="!hasSelectedDocument" @click="syncRealtimeState()">
            <el-icon class="el-icon--left"><Refresh /></el-icon>同步状态
          </el-button>
        </div>
      </div>
      
      <div class="header-stats-strip">
        <div class="stat-item">
          <span class="label">成员</span>
          <strong class="val">{{ projectStats?.totalMembers ?? 0 }}</strong>
        </div>
        <el-divider direction="vertical" />
        <div class="stat-item">
          <span class="label">文档</span>
          <strong class="val">{{ projectStats?.totalDocuments ?? 0 }}</strong>
        </div>
        <el-divider direction="vertical" />
        <div class="stat-item">
          <span class="label">在线协作</span>
          <strong class="val text-primary">{{ realtimeCollaborators.length }}</strong>
        </div>
        <el-divider direction="vertical" />
        <div class="stat-item">
          <span class="label">状态</span>
          <strong class="val">
            <span class="status-dot" :class="realtimeConnected ? 'is-online' : 'is-offline'"></span>
            {{ realtimeStatusText }}
          </strong>
        </div>
      </div>
    </header>

    <!-- 主体三栏沉浸式布局 -->
    <div class="app-layout">
      <!-- 左栏：导航与成员库 -->
      <aside class="left-sidebar">
        <!-- 文档库 -->
        <section class="sidebar-panel base-card">
          <div class="panel-header">
            <h2 class="panel-title">文档库</h2>
            <el-tag size="small" type="info" round>{{ documents.length }}</el-tag>
          </div>
          <div class="panel-body custom-scrollbar">
            <div v-if="documents.length" class="doc-nav-list">
              <div
                v-for="doc in documents"
                :key="doc.id"
                class="doc-nav-item"
                :class="{ 'is-active': selectedDocument?.id === doc.id }"
                @click="selectDocument(doc.id)"
              >
                <div class="item-main">
                  <el-icon class="doc-icon"><Document /></el-icon>
                  <span class="doc-title">{{ doc.title }}</span>
                  <el-icon v-if="doc.isLocked" class="lock-icon"><Lock /></el-icon>
                </div>
                <div class="item-meta">
                  <span class="tag">{{ doc.documentType || 'TEXT' }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无文档" :image-size="80" />
          </div>
        </section>

        <!-- 项目成员 -->
        <section class="sidebar-panel base-card">
          <div class="panel-header">
            <h2 class="panel-title">项目成员</h2>
            <el-tag size="small" type="success" round>{{ members.length }}</el-tag>
          </div>
          <div class="panel-body member-section custom-scrollbar">
            <div class="member-invite-bar">
              <el-input v-model="memberForm.email" placeholder="输入邮箱邀请..." class="invite-input" />
              <div class="invite-actions">
                <el-select v-model="memberForm.role" style="width: 100px;">
                  <el-option label="管理员" value="ADMIN" />
                  <el-option label="成员" value="MEMBER" />
                  <el-option label="查看" value="VIEWER" />
                </el-select>
                <el-button type="primary" color="#6366f1" :loading="invitingMember" @click="addMember" icon="Plus" circle />
              </div>
            </div>

            <div v-if="members.length" class="member-list" v-loading="loadingMembers">
              <div v-for="row in members" :key="row.id" class="member-item">
                <div class="avatar-wrap">
                  {{ (row.user?.username || row.user?.email || '?').charAt(0).toUpperCase() }}
                </div>
                <div class="member-info">
                  <div class="name">{{ row.user?.username || row.user?.email || '-' }}</div>
                  <div class="role">{{ row.role || '-' }}</div>
                </div>
                <el-button link type="danger" size="small" @click="removeMember(row)">移除</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无成员" :image-size="60" />
          </div>
        </section>
      </aside>

      <!-- 中栏：核心编辑器 -->
      <main class="center-editor base-card">
        <div class="editor-header">
          <div class="editor-status">
            <el-tag v-if="realtimeConnected" type="success" effect="light" round>实时已连接</el-tag>
            <el-tag v-if="selectedDocument?.isLocked" type="warning" effect="light" round>文档锁定中</el-tag>
            <el-tag v-if="selectedDocument?.canEdit === false" type="info" effect="light" round>只读模式</el-tag>
          </div>
          <div class="editor-actions">
            <span v-if="selectedDocument" class="update-tip">最后更新：{{ formatDateTime(selectedDocument?.updatedAt || selectedDocument?.createdAt) }}</span>
            <el-button :disabled="!hasSelectedDocument" @click="toggleLock" round size="small">
              {{ selectedDocument?.isLocked ? '解锁' : '锁定' }}
            </el-button>
            <el-button type="danger" plain :disabled="!hasSelectedDocument" @click="removeDocument" round size="small">删除</el-button>
            <el-button type="primary" color="#6366f1" :loading="savingDoc" :disabled="!canEditCurrent" @click="saveDocument" round size="small">保存</el-button>
          </div>
        </div>

        <div v-if="hasSelectedDocument" class="editor-canvas custom-scrollbar">
          <el-input
            v-model="docForm.title"
            placeholder="无标题文档"
            :disabled="!canEditCurrent"
            class="editor-title-input"
          />

          <div class="block-editor-container">
            <div
              v-for="block in sortedBlocks"
              :key="block.id"
              class="doc-block"
              :class="{ 'is-locked': isLockedByOther(block), 'is-self-locked': isLockedBySelf(block) }"
            >
              <div class="block-toolbar">
                <div class="block-lock-state">
                  <span class="lock-chip" :class="`is-${getBlockLockTone(block)}`">{{ getBlockLockText(block) }}</span>
                  <span class="lock-detail">{{ getBlockLockDetail(block) }}</span>
                </div>

                <div class="toolbar-actions" v-if="canEditCurrent">
                  <el-button size="small" text @mousedown.prevent @click="createNewBlockBelow(block.id)">
                    在下方新增
                  </el-button>
                  <el-button
                    v-if="block.lockedByUserId === currentUserId"
                    size="small"
                    text
                    type="warning"
                    @mousedown.prevent
                    @click="unlockBlock(block.id)"
                  >
                    释放锁
                  </el-button>
                </div>
              </div>

              <el-input
                :ref="(el: any) => registerBlockInputRef(block.id, el)"
                v-model="block.content"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 8 }"
                placeholder="输入段落内容..."
                :disabled="!canEditCurrent || isLockedByOther(block)"
                class="editor-content-input"
                @focus="onBlockFocus(block)"
                @blur="onBlockBlur(block)"
                @input="onBlockInput(block)"
                @keydown.enter.exact.prevent="onBlockEnter(block, $event)"
              />
            </div>

            <div class="block-actions">
              <el-button
                v-if="canEditCurrent && lastBlockId"
                type="primary"
                plain
                round
                @click="createNewBlockBelow(lastBlockId)"
              >
                新增段落
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="editor-empty">
          <el-empty description="请在左侧选择或新建一个文档" image-size="160" />
        </div>
      </main>

      <!-- 右栏：交流与协作 -->
      <aside class="right-sidebar">
        <!-- 实时聊天 -->
        <section class="sidebar-panel base-card chat-panel">
          <div class="panel-header">
            <h2 class="panel-title">实时讨论区</h2>
            <div class="online-avatars">
               <el-tooltip v-for="person in realtimeCollaborators" :key="person.userId" :content="person.username || person.userId" placement="top">
                 <div class="mini-avatar" :class="{'is-active': person.status === 'online'}">
                    {{ (person.username || person.userId).charAt(0).toUpperCase() }}
                 </div>
               </el-tooltip>
            </div>
          </div>
          
          <div class="chat-history custom-scrollbar">
            <div v-if="realtimeChatMessages.length === 0" class="empty-hint">还没人说话，来打个招呼吧~</div>
            <div
              v-for="item in realtimeChatMessages"
              :key="item.id"
              class="chat-message"
              :class="{ 'is-self': item.self, 'is-system': item.userName === '系统' }"
            >
              <template v-if="item.userName === '系统'">
                <div class="system-msg">{{ item.content }}</div>
              </template>
              <template v-else>
                <div class="msg-meta">{{ item.userName }} <span class="time">{{ formatRealtimeTime(item.timestamp) }}</span></div>
                <div class="chat-bubble">{{ item.content }}</div>
              </template>
            </div>
          </div>

          <div class="chat-input-bar">
            <el-input
              v-model="realtimeChatInput"
              placeholder="发条消息..."
              :disabled="!hasSelectedDocument || !realtimeConnected"
              @keyup.enter="sendRealtimeChat"
              class="rounded-input"
            >
              <template #append>
                <el-button type="primary" :loading="sendingRealtimeChat" :disabled="!hasSelectedDocument || !realtimeConnected" @click="sendRealtimeChat">
                  <el-icon><Position /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
        </section>

        <!-- 文档评论留存 -->
        <section class="sidebar-panel base-card comments-panel">
          <div class="panel-header">
            <h2 class="panel-title">文档批注</h2>
            <el-tag size="small" type="info" round>{{ comments.length }}</el-tag>
          </div>
          <div class="comments-list custom-scrollbar">
            <el-empty v-if="comments.length === 0" description="暂无批注" :image-size="60" />
            <div v-for="comment in comments" :key="comment.id" class="comment-card">
              <div class="comment-header">
                <strong>{{ comment.author?.username || '匿名用户' }}</strong>
                <span class="time">{{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
              <div class="comment-actions" v-if="comment.canDelete">
                <el-button link type="danger" size="small" @click="removeComment(comment)">删除</el-button>
              </div>
            </div>
          </div>
          <div class="comment-input-bar">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              placeholder="添加您的批注..."
              :disabled="!hasSelectedDocument"
              resize="none"
              class="minimal-textarea"
            />
            <div class="bar-actions">
               <el-button type="primary" color="#6366f1" size="small" :loading="addingComment" :disabled="!hasSelectedDocument" @click="submitComment" round>
                 提交批注
               </el-button>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <!-- 新建文档弹窗 -->
    <el-dialog v-model="showCreateDocDialog" title="📄 新建协作文档" width="480px" class="modern-dialog" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="文档标题" required>
          <el-input v-model="createDocForm.title" maxlength="100" placeholder="例如：Q4 产品规划草案" size="large" />
        </el-form-item>
        <el-form-item label="文档格式" required>
          <el-select v-model="createDocForm.documentType" style="width: 100%" size="large">
            <el-option label="普通文本 (TEXT)" value="TEXT" />
            <el-option label="Markdown 格式" value="MARKDOWN" />
            <el-option label="知识库 Wiki" value="WIKI" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDocDialog = false" round>取消</el-button>
          <el-button type="primary" color="#6366f1" :loading="creatingDoc" @click="submitCreateDocument" round>
            创建文档
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 全局变量与底色设定 */
.modern-collab-app {
  --primary-color: #6366f1; /* 靛蓝主题 */
  --primary-light: #e0e7ff;
  --text-main: #0f172a;
  --text-regular: #334155;
  --text-muted: #64748b;
  --bg-page: #f1f5f9; /* 比普通页面稍深的背景，凸显编辑器白纸感 */
  --bg-card: #ffffff;
  --border-color: #e2e8f0;

  background-color: var(--bg-page);
  min-height: calc(100vh - 60px);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 隐藏原生滚动条，使用自定义细滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }

/* 基础卡片样式 */
.base-card {
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

/* 顶部 Header */
.app-header {
  padding: 20px 24px;
}
.header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.kicker-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 4px 10px;
  border-radius: 20px;
  margin-bottom: 8px;
}
.project-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}
.header-actions {
  display: flex;
  gap: 12px;
}

/* 状态条 */
.header-stats-strip {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 20px;
  background: #f8fafc;
  border-radius: 12px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.stat-item .label { font-size: 13px; color: var(--text-muted); }
.stat-item .val { font-size: 15px; font-weight: 600; color: var(--text-main); }
.text-primary { color: var(--primary-color) !important; }

/* 状态圆点 */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.is-online { background: #10b981; box-shadow: 0 0 0 3px #d1fae5; }
.status-dot.is-offline { background: #94a3b8; }

/* 三栏布局 */
.app-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 320px;
  gap: 20px;
  height: calc(100vh - 220px); /* 撑满屏幕底部 */
  min-height: 600px;
}

/* 侧边栏通用面板 */
.left-sidebar, .right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}
.sidebar-panel {
  flex: 1;
  min-height: 0; /* 允许内部滚动 */
}
.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: var(--text-main);
}
.panel-body {
  padding: 12px;
  overflow-y: auto;
  flex: 1;
}

/* 文档列表导航式设计 */
.doc-nav-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.doc-nav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-regular);
}
.doc-nav-item:hover {
  background: #f1f5f9;
}
.doc-nav-item.is-active {
  background: var(--primary-light);
  color: var(--primary-color);
  font-weight: 500;
}
.item-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.doc-icon { font-size: 16px; }
.doc-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}
.lock-icon { font-size: 12px; color: #f59e0b; }
.item-meta .tag {
  font-size: 11px;
  background: #e2e8f0;
  color: var(--text-muted);
  padding: 2px 6px;
  border-radius: 4px;
}
.doc-nav-item.is-active .item-meta .tag {
  background: #c7d2fe;
  color: var(--primary-color);
}

/* 项目成员库 */
.member-section { display: flex; flex-direction: column; }
.member-invite-bar {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.invite-actions { display: flex; gap: 8px; }

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.member-item:hover { background: #f8fafc; }
.avatar-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e2e8f0;
  color: var(--text-regular);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}
.member-info { flex: 1; min-width: 0; }
.member-info .name { font-size: 14px; font-weight: 500; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
.member-info .role { font-size: 12px; color: var(--text-muted); margin-top: 2px;}

/* 中心编辑器 (Notion Style) */
.center-editor {
  height: 100%;
}
.editor-header {
  padding: 16px 24px;
  border-bottom: 1px solid transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-tip { font-size: 12px; color: var(--text-muted); }

.editor-canvas {
  flex: 1;
  padding: 40px 60px;
  overflow-y: auto;
}
/* 深度修改 Element Plus 输入框样式，达到无边框纸张效果 */
.editor-title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0;
  background: transparent;
}
.editor-title-input :deep(input) {
  font-size: 32px;
  font-weight: 800;
  color: var(--text-main);
  height: auto;
  line-height: 1.4;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}
.block-editor-container {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.doc-block {
  position: relative;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: #ffffff;
  transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
}

.doc-block:focus-within {
  border-color: #c7d2fe;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.doc-block.is-locked {
  background: #fff7ed;
  border-style: dashed;
  border-color: #fdba74;
}

.doc-block.is-self-locked {
  background: #eef2ff;
  border-color: #a5b4fc;
}

.block-toolbar {
  padding: 10px 12px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  opacity: 0;
  transform: translateY(-2px);
  pointer-events: none;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.doc-block:hover .block-toolbar,
.doc-block:focus-within .block-toolbar,
.doc-block.is-locked .block-toolbar,
.doc-block.is-self-locked .block-toolbar {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.block-lock-state {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.lock-chip {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.lock-chip.is-idle {
  color: #475569;
  background: #e2e8f0;
}

.lock-chip.is-self {
  color: #3730a3;
  background: #c7d2fe;
}

.lock-chip.is-other {
  color: #92400e;
  background: #fed7aa;
}

.lock-detail {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-actions :deep(.el-button) {
  padding: 4px 8px;
}

.editor-content-input :deep(.el-textarea__inner) {
  box-shadow: none !important;
  border: none;
  background: transparent;
  padding: 14px 16px;
  font-size: 16px;
  line-height: 1.75;
  color: #1e293b;
  resize: none;
  min-height: 108px !important;
}

.editor-content-input :deep(.el-textarea__inner:disabled) {
  color: #64748b;
  cursor: not-allowed;
}

.block-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-start;
}

.editor-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 右侧在线头像 */
.online-avatars { display: flex; }
.mini-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #cbd5e1;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  border: 2px solid #fff;
  margin-left: -8px;
  position: relative;
}
.mini-avatar:first-child { margin-left: 0; }
.mini-avatar.is-active { background: #10b981; }

/* 实时聊天气泡 */
.chat-panel { flex: 3; }
.chat-history {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f8fafc;
}
.empty-hint { text-align: center; font-size: 12px; color: #94a3b8; margin-top: 20px;}
.system-msg {
  align-self: center;
  font-size: 11px;
  background: #e2e8f0;
  color: var(--text-muted);
  padding: 4px 12px;
  border-radius: 12px;
}
.chat-message { display: flex; flex-direction: column; gap: 4px; max-width: 85%; }
.chat-message.is-self { align-self: flex-end; align-items: flex-end; }
.msg-meta { font-size: 11px; color: var(--text-muted); margin: 0 4px; }
.msg-meta .time { margin-left: 6px; opacity: 0.7;}
.chat-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  border-bottom-left-radius: 4px;
}
.chat-message.is-self .chat-bubble {
  background: var(--primary-color);
  color: #ffffff;
  border-color: var(--primary-color);
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 4px;
}

.chat-input-bar {
  padding: 12px;
  border-top: 1px solid var(--border-color);
}
:deep(.rounded-input .el-input__wrapper) { border-radius: 20px 0 0 20px; }
:deep(.rounded-input .el-input-group__append) { border-radius: 0 20px 20px 0; background: var(--primary-light); color: var(--primary-color); border: none;}

/* 文档批注卡片 */
.comments-panel { flex: 4; }
.comments-list {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
}
.comment-card {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  position: relative;
}
.comment-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.comment-header strong { font-size: 13px; color: var(--text-main); }
.comment-header .time { font-size: 11px; color: var(--text-muted); }
.comment-content { font-size: 13px; color: var(--text-regular); margin: 0; line-height: 1.5; }
.comment-actions { margin-top: 8px; text-align: right; }

.comment-input-bar {
  padding: 12px;
  border-top: 1px solid var(--border-color);
}
.minimal-textarea :deep(.el-textarea__inner) {
  background: #f8fafc;
  border: none;
  border-radius: 8px;
  box-shadow: none;
  font-size: 13px;
}
.minimal-textarea :deep(.el-textarea__inner:focus) { background: #fff; box-shadow: 0 0 0 1px var(--primary-color) !important;}
.bar-actions { margin-top: 8px; display: flex; justify-content: flex-end; }

/* 弹窗及响应式 */
:deep(.modern-dialog) { border-radius: 20px; overflow: hidden; }
:deep(.modern-dialog .el-dialog__header) { background: #f8fafc; padding: 24px; border-bottom: 1px solid var(--border-color); margin: 0;}
:deep(.modern-dialog .el-dialog__title) { font-weight: 700; font-size: 18px; }
:deep(.modern-dialog .el-dialog__body) { padding: 24px; }

@media (max-width: 1200px) {
  .app-layout { grid-template-columns: 240px 1fr; }
  .right-sidebar { display: none; } /* 中屏下隐藏侧边聊天，可考虑抽屉组件 */
  .editor-canvas { padding: 24px; }
}

@media (max-width: 768px) {
  .app-layout { grid-template-columns: 1fr; height: auto; }
  .left-sidebar { display: none; }
  .header-main { flex-direction: column; align-items: flex-start; gap: 16px;}
  .header-stats-strip { flex-wrap: wrap; }
  .editor-canvas { padding: 16px; }
  .editor-title-input :deep(input) { font-size: 26px; }
  .block-editor-container { margin-top: 16px; gap: 10px; }
  .block-toolbar {
    padding-top: 8px;
    gap: 8px;
    flex-wrap: wrap;
    opacity: 1;
    transform: none;
    pointer-events: auto;
  }
  .block-lock-state {
    flex: 1;
    min-width: 100%;
  }
  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
