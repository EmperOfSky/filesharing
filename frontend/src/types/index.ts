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
  createdAt: string
  updatedAt: string
}

// 分享相关类型
export interface ShareRecord {
  id: number
  shareKey: string
  shareType: 'FILE' | 'FOLDER'
  sharedContent?: {
    id: number
    name: string
    size?: number
    contentType?: string
    path?: string
  }
  sharerId: number
  sharerName: string
  title?: string
  description?: string
  requiresPassword: boolean
  allowDownload: boolean
  shortLink: string
  accessUrl: string
  downloadUrl?: string
  expireTime?: string
  maxAccessCount: number
  currentAccessCount: number
  status: 'ACTIVE' | 'EXPIRED' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
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