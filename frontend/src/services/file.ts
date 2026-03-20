import http from './http'
import { FileItem, Folder, PaginatedResponse, SearchRequest, SearchResponse } from '@/types'

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
}

export default new FileService()