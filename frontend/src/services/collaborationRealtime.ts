import type { RealtimeWsMessage } from '@/types/collaboration'

interface RealtimeCallbacks {
  onOpen?: () => void
  onClose?: () => void
  onError?: (event: Event) => void
  onMessage?: (message: RealtimeWsMessage) => void
}

interface ConnectOptions extends RealtimeCallbacks {
  token: string
  documentId: number
  userName?: string
}

class CollaborationRealtimeClient {
  private socket: WebSocket | null = null
  private callbacks: RealtimeCallbacks = {}
  private shouldReconnect = false
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 3
  private reconnectTimer: number | null = null
  private currentToken = ''
  private currentDocumentId: number | null = null
  private currentUserName = ''

  connect(options: ConnectOptions) {
    this.callbacks = {
      onOpen: options.onOpen,
      onClose: options.onClose,
      onError: options.onError,
      onMessage: options.onMessage
    }
    this.currentToken = options.token
    this.currentDocumentId = options.documentId
    this.currentUserName = options.userName || ''
    this.shouldReconnect = true

    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.sendJoin(options.documentId, this.currentUserName)
      return
    }

    this.cleanupSocket()

    const wsUrl = `${this.getWsBaseUrl()}/ws/collaboration?token=${encodeURIComponent(options.token)}`
    this.socket = new WebSocket(wsUrl)

    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      if (this.currentDocumentId != null) {
        this.sendJoin(this.currentDocumentId, this.currentUserName)
      }
      this.callbacks.onOpen?.()
    }

    this.socket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data) as RealtimeWsMessage
        this.callbacks.onMessage?.(parsed)
      } catch {
        // 非JSON消息忽略
      }
    }

    this.socket.onerror = (event) => {
      this.callbacks.onError?.(event)
    }

    this.socket.onclose = () => {
      this.callbacks.onClose?.()
      this.socket = null
      if (this.shouldReconnect) {
        this.tryReconnect()
      }
    }
  }

  disconnect() {
    this.shouldReconnect = false
    this.sendLeave()
    this.cleanupSocket()
  }

  sendEdit(operation: string, position?: number) {
    this.sendMessage({
      type: 'EDIT_OPERATION',
      operation,
      position,
      timestamp: Date.now()
    })
  }

  sendCursor(position: number) {
    this.sendMessage({
      type: 'CURSOR_POSITION',
      position,
      timestamp: Date.now()
    })
  }

  sendChat(content: string) {
    this.sendMessage({
      type: 'CHAT_MESSAGE',
      content,
      timestamp: Date.now()
    })
  }

  requestSync() {
    this.sendMessage({
      type: 'REQUEST_SYNC',
      timestamp: Date.now()
    })
  }

  lockBlock(blockId: number) {
    this.sendMessage({
      type: 'LOCK_BLOCK',
      blockId,
      timestamp: Date.now()
    })
  }

  unlockBlock(blockId: number) {
    this.sendMessage({
      type: 'UNLOCK_BLOCK',
      blockId,
      timestamp: Date.now()
    })
  }

  updateBlock(blockId: number, content: string, version?: number) {
    this.sendMessage({
      type: 'UPDATE_BLOCK',
      blockId,
      content,
      version,
      timestamp: Date.now()
    })
  }

  createBlock(afterBlockId: number) {
    this.sendMessage({
      type: 'CREATE_BLOCK',
      afterBlockId,
      timestamp: Date.now()
    })
  }

  private sendJoin(documentId: number, userName?: string) {
    this.sendMessage({
      type: 'JOIN_DOCUMENT',
      documentId: String(documentId),
      userName,
      timestamp: Date.now()
    })
  }

  private sendLeave() {
    this.sendMessage({
      type: 'LEAVE_DOCUMENT',
      timestamp: Date.now()
    })
  }

  private sendMessage(message: RealtimeWsMessage) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return
    }
    this.socket.send(JSON.stringify(message))
  }

  private tryReconnect() {
    if (!this.currentToken || this.currentDocumentId == null) {
      return
    }
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return
    }

    this.reconnectAttempts += 1
    const delay = Math.min(4000, this.reconnectAttempts * 1000)

    if (this.reconnectTimer != null) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    this.reconnectTimer = window.setTimeout(() => {
      this.connect({
        token: this.currentToken,
        documentId: this.currentDocumentId as number,
        userName: this.currentUserName,
        ...this.callbacks
      })
    }, delay)
  }

  private cleanupSocket() {
    if (this.reconnectTimer != null) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.socket) {
      this.socket.onopen = null
      this.socket.onmessage = null
      this.socket.onclose = null
      this.socket.onerror = null
      if (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING) {
        this.socket.close()
      }
      this.socket = null
    }
  }

  private getWsBaseUrl(): string {
    const envWsBase = (import.meta as any).env?.VITE_WS_BASE_URL as string | undefined
    if (envWsBase && envWsBase.trim()) {
      return envWsBase.replace(/\/$/, '')
    }

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const hostname = window.location.hostname
    const frontendPort = window.location.port

    let targetPort = frontendPort
    if ((hostname === 'localhost' || hostname === '127.0.0.1') && ['3000', '5173', '4173'].includes(frontendPort)) {
      targetPort = '8080'
    }

    return `${protocol}://${hostname}${targetPort ? `:${targetPort}` : ''}`
  }
}

export default new CollaborationRealtimeClient()
