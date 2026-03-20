export interface UserSimple {
  id: number
  username: string
  email?: string
  nickname?: string
  avatar?: string
}

export interface PageResult<T> {
  content: T[]
  totalPages: number
  totalElements: number
  numberOfElements: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface CollaborationProject {
  id: number
  projectName: string
  description?: string
  tags?: string
  status?: string
  owner?: UserSimple
  memberCount?: number
  documentCount?: number
  createdAt?: string
  updatedAt?: string
  currentUserIsOwner?: boolean
  currentUserIsMember?: boolean
}

export interface ProjectMember {
  id: number
  user?: UserSimple
  role?: string
  status?: string
  invitedByEmail?: string
  canEdit?: boolean
  canManage?: boolean
}

export interface CollaborativeDocument {
  id: number
  title: string
  description?: string
  documentType?: string
  status?: string
  content?: string
  createdBy?: UserSimple
  lastEditedBy?: UserSimple
  projectId?: number
  projectName?: string
  createdAt?: string
  updatedAt?: string
  lastEditedAt?: string
  isLocked?: boolean
  lockedBy?: UserSimple
  commentCount?: number
  canEdit?: boolean
  canDelete?: boolean
}

export interface CollaborationComment {
  id: number
  content: string
  author?: UserSimple
  documentId?: number
  parentCommentId?: number
  createdAt?: string
  updatedAt?: string
  canEdit?: boolean
  canDelete?: boolean
  replyCount?: number
}

export interface ProjectStatistics {
  projectId: number
  projectName?: string
  totalMembers?: number
  totalDocuments?: number
  totalComments?: number
  activeDocuments?: number
  lockedDocuments?: number
}

export interface UserCollaborationStats {
  userId: number
  username?: string
  totalProjects?: number
  totalDocuments?: number
  totalComments?: number
  projectsOwned?: number
  projectsParticipated?: number
}

export interface CreateProjectPayload {
  projectName: string
  description?: string
  tags?: string
}

export interface UpdateProjectPayload {
  projectName?: string
  description?: string
  tags?: string
}

export interface AddMemberPayload {
  email: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER' | 'EDITOR'
}

export interface UpdateMemberRolePayload {
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER' | 'EDITOR'
  permissions?: string
}

export interface CreateDocumentPayload {
  title: string
  description?: string
  documentType: 'TEXT' | 'MARKDOWN' | 'WIKI'
  projectId: number
}

export interface UpdateDocumentPayload {
  title?: string
  description?: string
  content?: string
}

export interface CreateCommentPayload {
  content: string
  parentCommentId?: number
}

export interface UpdateCommentPayload {
  content: string
}

export interface RealtimeDocumentState {
  documentId: number
  content: string
  version: number
  metadata?: Record<string, unknown>
  lastModified?: number
}

export interface RealtimeDocumentBlock {
  id: number
  documentId: number
  content: string
  orderIndex: number
  lockedByUserId?: number
  lockedUserName?: string
  lockedAt?: number
  version: number
}

export interface RealtimeCollaborator {
  userId: string
  username: string
  avatar?: string
  cursorPosition?: string
  lastActiveTime?: number
  status?: string
}

export interface RealtimeSession {
  documentId: number
  sessionId: string
  collaborators: RealtimeCollaborator[]
  initialState?: RealtimeDocumentState
  createdAt?: number
}

export interface RealtimeEditPayload {
  type: 'insert' | 'delete' | 'update'
  content?: string
  position?: number
  length?: number
  version?: number
  timestamp?: number
}

export interface RealtimeConflictResolution {
  type: 'auto-resolved' | 'manual-required' | string
  description?: string
  resolution?: string
}

export interface RealtimeEditResult {
  success: boolean
  message?: string
  newState?: RealtimeDocumentState
  conflicts?: RealtimeConflictResolution[]
}

export interface RealtimeUpdateBlockPayload {
  content: string
  version?: number
}

export interface RealtimeCreateBlockPayload {
  afterBlockId: number
}

export interface RealtimeChatPayload {
  content: string
  messageType?: string
}

export interface RealtimeStatistics {
  activeSessions: number
  totalCollaborators: number
  documentsBeingEdited: number
  averageCollaboratorsPerDocument: number
  editOperationStats?: Record<string, number>
}

export interface RealtimeWsMessage {
  type:
    | 'JOIN_DOCUMENT'
    | 'LEAVE_DOCUMENT'
    | 'EDIT_OPERATION'
    | 'CURSOR_POSITION'
    | 'CHAT_MESSAGE'
    | 'REQUEST_SYNC'
    | 'USER_JOIN'
    | 'USER_LEAVE'
    | 'DOCUMENT_STATE'
    | 'DOCUMENT_UPDATED'
    | 'LOCK_BLOCK'
    | 'UNLOCK_BLOCK'
    | 'UPDATE_BLOCK'
    | 'CREATE_BLOCK'
    | 'BLOCK_LOCKED'
    | 'BLOCK_UNLOCKED'
    | 'BLOCK_UPDATED'
    | 'BLOCK_CREATED'
    | 'SYSTEM'
    | 'ERROR'
  userId?: string
  userName?: string
  documentId?: string
  content?: string
  operation?: string
  position?: number
  blockId?: number
  afterBlockId?: number
  version?: number
  status?: string
  timestamp?: number
}
