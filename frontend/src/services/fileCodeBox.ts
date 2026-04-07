import http from './http'

export interface FileCodeBoxShareResult {
  code: string
  name?: string
}

export interface FileCodeBoxSelectResult {
  code: string
  name: string
  size: number
  text: string
}

interface FileCodeBoxPresignInitResult {
  upload_id: string
  upload_url: string
  mode: 'direct' | 'proxy'
  expires_in: number
}

export interface FileCodeBoxChunkInitResult {
  upload_id: string
  chunk_size: number
  total_chunks: number
  uploaded_chunks: number[]
  existed?: boolean
}

export interface FileCodeBoxChunkStatusResult {
  upload_id: string
  file_name: string
  file_size: number
  chunk_size: number
  total_chunks: number
  uploaded_chunks: number[]
  progress: number
}

export interface FileCodeBoxRecordItem {
  id: number
  code: string
  share_type: 'FILE' | 'TEXT'
  text_content?: string
  display_name?: string
  content_type?: string
  storage_mode?: 'LOCAL' | 'CLOUD_DIRECT'
  storage_path?: string
  cloud_config_id?: number
  size_bytes: number
  expire_at?: string
  expired_count?: number
  used_count?: number
  created_ip?: string
  creator_user_id?: number
  status: 'ACTIVE' | 'EXPIRED' | 'DISABLED'
  created_at: string
  updated_at: string
  is_expired: boolean
  is_count_limited: boolean
  remain_count?: number
}

interface FileCodeBoxRecordPageResult {
  content: FileCodeBoxRecordItem[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface FileCodeBoxAdminConfig {
  open_upload: boolean
  upload_size: number
  upload_size_mb: number
  upload_count: number
  upload_minute: number
  error_count: number
  error_minute: number
  max_save_seconds: number
  expire_styles: string[]
  presign_expire_seconds: number
  download_token_ttl_seconds: number
}

interface FileCodeBoxAdminConfigUpdatePayload {
  open_upload?: boolean
  upload_size?: number
  upload_count?: number
  upload_minute?: number
  error_count?: number
  error_minute?: number
  max_save_seconds?: number
  expire_styles?: string[]
  presign_expire_seconds?: number
  download_token_ttl_seconds?: number
}

class FileCodeBoxService {
  async shareText(text: string, expireValue = 1, expireStyle = 'day'): Promise<FileCodeBoxShareResult> {
    const body = new URLSearchParams()
    body.set('text', text)
    body.set('expire_value', String(expireValue))
    body.set('expire_style', expireStyle)
    return http.post<FileCodeBoxShareResult>('/public/share/text', body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
  }

  async shareFile(file: File, expireValue = 1, expireStyle = 'day'): Promise<FileCodeBoxShareResult> {
    const form = new FormData()
    form.append('file', file)
    form.append('expire_value', String(expireValue))
    form.append('expire_style', expireStyle)
    return http.post<FileCodeBoxShareResult>('/public/share/file', form)
  }

  async selectByCode(code: string): Promise<FileCodeBoxSelectResult> {
    return http.post<FileCodeBoxSelectResult>('/public/share/select', { code })
  }

  async initPresignUpload(fileName: string, fileSize: number, expireValue = 1, expireStyle = 'day'): Promise<FileCodeBoxPresignInitResult> {
    return http.post<FileCodeBoxPresignInitResult>('/public/presign/upload/init', {
      file_name: fileName,
      file_size: fileSize,
      expire_value: expireValue,
      expire_style: expireStyle
    })
  }

  async uploadPresignProxy(uploadId: string, file: File): Promise<FileCodeBoxShareResult> {
    const form = new FormData()
    form.append('file', file)
    return http.put<FileCodeBoxShareResult>(`/public/presign/upload/proxy/${uploadId}`, form)
  }

  async confirmPresignUpload(uploadId: string, expireValue = 1, expireStyle = 'day'): Promise<FileCodeBoxShareResult> {
    return http.post<FileCodeBoxShareResult>(`/public/presign/upload/confirm/${uploadId}`, {
      expire_value: expireValue,
      expire_style: expireStyle
    })
  }

  async initChunkUpload(
    fileName: string,
    fileSize: number,
    chunkSize = 5 * 1024 * 1024,
    expireValue = 1,
    expireStyle = 'day'
  ): Promise<FileCodeBoxChunkInitResult> {
    return http.post<FileCodeBoxChunkInitResult>('/chunk/upload/init', {
      file_name: fileName,
      file_size: fileSize,
      chunk_size: chunkSize,
      expire_value: expireValue,
      expire_style: expireStyle
    })
  }

  async uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob): Promise<{ chunk_hash: string; skipped?: boolean }> {
    const form = new FormData()
    form.append('chunk', chunk)
    return http.post<{ chunk_hash: string; skipped?: boolean }>(`/chunk/upload/chunk/${uploadId}/${chunkIndex}`, form)
  }

  async getChunkUploadStatus(uploadId: string): Promise<FileCodeBoxChunkStatusResult> {
    return http.get<FileCodeBoxChunkStatusResult>(`/chunk/upload/status/${uploadId}`)
  }

  async completeChunkUpload(uploadId: string, expireValue = 1, expireStyle = 'day'): Promise<FileCodeBoxShareResult> {
    return http.post<FileCodeBoxShareResult>(`/chunk/upload/complete/${uploadId}`, {
      expire_value: expireValue,
      expire_style: expireStyle
    })
  }

  async cancelChunkUpload(uploadId: string): Promise<Record<string, unknown>> {
    return http.delete<Record<string, unknown>>(`/chunk/upload/${uploadId}`)
  }

  async getAdminConfig(): Promise<FileCodeBoxAdminConfig> {
    return http.get<FileCodeBoxAdminConfig>('/admin/quick-transfer/config')
  }

  async updateAdminConfig(payload: FileCodeBoxAdminConfigUpdatePayload): Promise<FileCodeBoxAdminConfig> {
    return http.put<FileCodeBoxAdminConfig>('/admin/quick-transfer/config', payload)
  }

  async getAdminRecords(params: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    share_type?: string
  }): Promise<FileCodeBoxRecordPageResult> {
    return http.get<FileCodeBoxRecordPageResult>('/admin/quick-transfer/records', { params })
  }

  async exportAdminRecordsCsv(params: {
    keyword?: string
    status?: string
    share_type?: string
  }): Promise<Blob> {
    return http.get<Blob>('/admin/quick-transfer/records/export', {
      params,
      responseType: 'blob' as any
    })
  }

  async updateAdminRecordStatus(recordId: number, status: 'ACTIVE' | 'DISABLED'): Promise<FileCodeBoxRecordItem> {
    return http.patch<FileCodeBoxRecordItem>(`/admin/quick-transfer/records/${recordId}/status`, { status })
  }

  async deleteAdminRecord(recordId: number): Promise<Record<string, unknown>> {
    return http.delete<Record<string, unknown>>(`/admin/quick-transfer/records/${recordId}`)
  }
}

export default new FileCodeBoxService()
