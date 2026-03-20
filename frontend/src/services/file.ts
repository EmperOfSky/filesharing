import http from './http'
import {
  BatchOperationResult,
  ExpiringRecycleItem,
  FileItem,
  Folder,
  PaginatedResponse,
  RecycleBinItem,
  RecycleBinStats,
  RestoreResult,
  SearchRequest,
  SearchResponse
} from '@/types'

class FileService {
  async uploadFile(file: File, folderId?: number): Promise<FileItem> {
    const formData = new FormData()
    formData.append('file', file)
    if (folderId) {
      formData.append('folderId', folderId.toString())
    }

    return http.post<FileItem>('/files/upload', formData)
  }

  async getFiles(page = 0, size = 10, folderId?: number): Promise<PaginatedResponse<FileItem>> {
    const params: Record<string, any> = { page, size }
    if (folderId) params.folderId = folderId
    
    return http.get<PaginatedResponse<FileItem>>('/files', { params })
  }

  async getFileById(id: number): Promise<FileItem> {
    return http.get<FileItem>(`/files/${id}`)
  }

  async deleteFile(id: number): Promise<void> {
    return http.delete(`/files/${id}`)
  }

  async downloadFile(id: number): Promise<Blob> {
    const response = await http.get(`/files/${id}/download`, {
      responseType: 'blob'
    })
    return response as unknown as Blob
  }

  async searchFiles(params: SearchRequest): Promise<SearchResponse> {
    return http.get<SearchResponse>('/files/search', { params })
  }

  // 文件夹相关
  async createFolder(name: string, parentId?: number): Promise<Folder> {
    return http.post<Folder>('/folders', { name, parentId })
  }

  async getFolders(): Promise<Folder[]> {
    return http.get<Folder[]>('/folders')
  }

  async deleteFolder(id: number): Promise<void> {
    return http.delete(`/folders/${id}`)
  }

  // 回收站相关
  async moveFileToRecycleBin(id: number, deleteReason?: string): Promise<string> {
    return http.post<string>(`/recycle-bin/files/${id}`, { deleteReason })
  }

  async moveFolderToRecycleBin(id: number, deleteReason?: string): Promise<string> {
    return http.post<string>(`/recycle-bin/folders/${id}`, { deleteReason })
  }

  async getRecycleBin(page = 0, size = 20, itemType?: string): Promise<PaginatedResponse<RecycleBinItem>> {
    const params: Record<string, any> = { page, size }
    if (itemType && itemType !== 'ALL') {
      params.itemType = itemType
    }
    return http.get<PaginatedResponse<RecycleBinItem>>('/recycle-bin', { params })
  }

  async searchRecycleBin(keyword: string, page = 0, size = 20): Promise<PaginatedResponse<RecycleBinItem>> {
    return http.get<PaginatedResponse<RecycleBinItem>>('/recycle-bin/search', {
      params: { keyword, page, size }
    })
  }

  async restoreRecycleBinItem(recycleBinId: number): Promise<RestoreResult> {
    return http.post<RestoreResult>(`/recycle-bin/${recycleBinId}/restore`, {})
  }

  async restoreRecycleBinItemToFolder(recycleBinId: number, targetFolderId: number): Promise<RestoreResult> {
    return http.post<RestoreResult>(`/recycle-bin/${recycleBinId}/restore-to`, { targetFolderId })
  }

  async permanentlyDeleteRecycleBinItem(recycleBinId: number): Promise<string> {
    return http.delete<string>(`/recycle-bin/${recycleBinId}`)
  }

  async emptyRecycleBin(): Promise<string> {
    return http.delete<string>('/recycle-bin/empty')
  }

  async getRecycleBinStats(): Promise<RecycleBinStats> {
    return http.get<RecycleBinStats>('/recycle-bin/stats')
  }

  async getRecycleBinExpiring(hours = 24): Promise<ExpiringRecycleItem[]> {
    return http.get<ExpiringRecycleItem[]>('/recycle-bin/expiring', {
      params: { hours }
    })
  }

  async batchRestoreRecycleBinItems(recycleBinIds: number[]): Promise<BatchOperationResult> {
    return http.post<BatchOperationResult>('/recycle-bin/batch/restore', { recycleBinIds })
  }

  async batchDeleteRecycleBinItems(recycleBinIds: number[]): Promise<BatchOperationResult> {
    return http.post<BatchOperationResult>('/recycle-bin/batch/delete', { recycleBinIds })
  }
}

export default new FileService()