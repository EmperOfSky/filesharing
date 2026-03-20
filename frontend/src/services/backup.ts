import http from './http'
import type {
  BackupAsyncRequestResult,
  BackupCleanupResult,
  BackupConfig,
  BackupInfo,
  BackupResult,
  BackupStatistics,
  BackupTask,
  BackupType,
  BackupValidationResult
} from '@/types'

class BackupService {
  async createFullBackup(backupName: string, includeFiles = true): Promise<BackupResult> {
    return http.post<BackupResult>('/backup/full', null, {
      params: { backupName, includeFiles }
    })
  }

  async createIncrementalBackup(backupName: string, sinceTime: string): Promise<BackupResult> {
    return http.post<BackupResult>('/backup/incremental', null, {
      params: {
        backupName,
        sinceTimeString: sinceTime
      }
    })
  }

  async createBackupAsync(payload: {
    backupName: string
    backupType: BackupType
    includeFiles?: boolean
    sinceTime?: string
  }): Promise<BackupAsyncRequestResult> {
    return http.post<BackupAsyncRequestResult>('/backup/async', payload)
  }

  async listBackups(): Promise<BackupInfo[]> {
    const data = await http.get<BackupInfo[]>('/backup/list')
    return data || []
  }

  async restoreFromBackup(backupPath: string, restoreFiles = true): Promise<{ taskId: string; success: boolean; message: string }> {
    return http.post<{ taskId: string; success: boolean; message: string }>('/backup/restore', null, {
      params: { backupPath, restoreFiles }
    })
  }

  async deleteBackup(backupPath: string): Promise<string> {
    return http.delete<string>('/backup/delete', {
      params: { backupPath }
    })
  }

  async getBackupTaskStatus(taskId: string): Promise<BackupTask> {
    return http.get<BackupTask>(`/backup/task/${taskId}`)
  }

  async cleanupExpiredBackups(daysToKeep = 30): Promise<BackupCleanupResult> {
    return http.delete<BackupCleanupResult>('/backup/cleanup', {
      params: { daysToKeep }
    })
  }

  async getBackupStatistics(): Promise<BackupStatistics> {
    return http.get<BackupStatistics>('/backup/statistics')
  }

  async validateBackup(backupPath: string): Promise<BackupValidationResult> {
    return http.post<BackupValidationResult>('/backup/validate', null, {
      params: { backupPath }
    })
  }

  async exportBackupConfig(): Promise<BackupConfig> {
    return http.get<BackupConfig>('/backup/config/export')
  }

  async importBackupConfig(config: BackupConfig): Promise<string> {
    return http.post<string>('/backup/config/import', config)
  }
}

export default new BackupService()
