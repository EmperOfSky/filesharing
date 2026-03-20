// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  role?: 'USER' | 'ADMIN'
  status?: 'ACTIVE' | 'DISABLED' | 'SUSPENDED'
  createdAt: string
  lastLoginAt?: string
}

// 文件相关类型
export interface FileItem {
  id: number
  originalName: string
  storageName: string
  fileSize: number
  contentType: string
  extension: string
  md5Hash: string
  status: 'UPLOADING' | 'AVAILABLE' | 'DELETED'
  isPublic: boolean
  downloadCount: number
  previewCount: number
  shareCount: number
  uploader: User
  folderId?: number
  createdAt: string
  updatedAt: string
  deletedAt?: string
}

export interface Folder {
  id: number
  name: string
  parentId?: number
  ownerId: number
  description?: string
  folderPath?: string
  createdAt: string
  updatedAt: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  numberOfElements: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// 认证相关类型
export interface LoginRequest {
  identifier: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
}

export interface AuthResponse {
  token: string
  user?: User
}

// 搜索相关类型
export interface SearchRequest {
  keyword: string
  fileType?: string
  minSize?: number
  maxSize?: number
  page?: number
  size?: number
}

export interface SearchResponse {
  files: FileItem[]
  folders: Folder[]
  totalResults: number
}

export type RecycleBinItemType = 'FILE' | 'FOLDER'

export interface RecycleBinItem {
  id: number
  itemId: number
  itemType: RecycleBinItemType | string
  originalName: string
  originalPath: string
  fileSize: number
  fileType: string
  deletedByName: string
  deletedAt: string
  expireAt: string
  isRecoverable: boolean
  deleteReason?: string
}

export interface RestoreResult {
  success: boolean
  message: string
  restoredItemId?: number
  restoredItemType?: string
  restorePath?: string
}

export interface RecycleBinStats {
  totalItems: number
  fileCount: number
  folderCount: number
  expiredCount: number
  recoverableCount: number
  oldestItemDate?: string
  newestItemDate?: string
}

export interface ExpiringRecycleItem {
  id: number
  itemName: string
  itemType: string
  expireTime: string
  hoursLeft: number
}

export interface BatchOperationResult {
  totalCount: number
  successCount: number
  failedCount: number
  failedItems: string[]
}

export type RecommendationType = 'FILE' | 'FOLDER' | 'TAG' | 'COLLABORATION' | 'SEARCH_RESULT'

export type RecommendationSourceType =
  | 'AI_MODEL'
  | 'USER_BEHAVIOR'
  | 'COLLABORATION_PATTERN'
  | 'CONTENT_SIMILARITY'

export interface SmartRecommendation {
  id: number
  recommendationType: RecommendationType | string
  itemId?: number
  reason?: string
  relevanceScore?: number
  sourceType?: RecommendationSourceType | string
  sourceModelId?: number
  isViewed: boolean
  isAdopted: boolean
  viewedAt?: string
  adoptedAt?: string
  tags?: string
  createdAt?: string
  expireAt?: string
}

export interface RecommendationAnalytics {
  totalRecommendations: number
  viewedRecommendations: number
  adoptedRecommendations: number
  viewRate: number
  adoptionRate: number
  typeDistribution?: Record<string, number>
  sourceDistribution?: Record<string, number>
}

export interface RecommendationListResponse {
  content?: SmartRecommendation[]
  recommendations?: SmartRecommendation[]
  number?: number
  currentPage?: number
  totalPages?: number
  totalElements?: number
  numberOfElements?: number
  size?: number
  first?: boolean
  last?: boolean
}

export interface RecommendationCleanupResult {
  message?: string
  timestamp?: number
}

export type BackupType = 'FULL' | 'INCREMENTAL'

export interface BackupResult {
  taskId: string
  success: boolean
  message: string
  backupPath: string
}

export interface BackupTask {
  taskId: string
  backupName: string
  backupType: BackupType | string
  status: string
  startTime?: string
  endTime?: string
  success?: boolean
  errorMessage?: string
  dbBackupPath?: string
  filesBackupPath?: string
  metadataPath?: string
  backedUpFileCount?: number
}

export interface BackupInfo {
  backupName: string
  backupPath: string
  backupType: BackupType | string
  createTime: string
  includeFiles: boolean
  totalFileSize: number
  fileCount: number
  valid: boolean
}

export interface BackupCleanupResult {
  startTime?: string
  endTime?: string
  success: boolean
  message: string
  deletedCount: number
  deletedBackups: string[]
  failedDeletions: string[]
}

export interface BackupStatistics {
  totalBackups: number
  totalBackupSize: number
  fullBackupCount: number
  incrementalBackupCount: number
  latestBackupTime?: string
  backupBasePath?: string
  maxBackupSize?: string
}

export interface BackupValidationResult {
  backupPath: string
  isValid: boolean
  validationTime: number
  message: string
}

export interface BackupConfig {
  backupBasePath?: string
  maxBackupSize?: string
  compressionLevel?: number
  retentionDays?: number
  autoBackupEnabled?: boolean
  exportTime?: number
  [key: string]: any
}

export interface BackupAsyncRequestResult {
  taskId: string
  requestId?: string
  message: string
  status: string
}