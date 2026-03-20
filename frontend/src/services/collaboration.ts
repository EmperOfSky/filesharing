import http from './http'
import type {
  AddMemberPayload,
  CollaborationComment,
  CollaborationProject,
  CollaborativeDocument,
  CreateCommentPayload,
  CreateDocumentPayload,
  CreateProjectPayload,
  PageResult,
  ProjectMember,
  ProjectStatistics,
  RealtimeChatPayload,
  RealtimeCollaborator,
  RealtimeCreateBlockPayload,
  RealtimeDocumentBlock,
  RealtimeDocumentState,
  RealtimeEditPayload,
  RealtimeEditResult,
  RealtimeUpdateBlockPayload,
  RealtimeSession,
  RealtimeStatistics,
  UpdateCommentPayload,
  UpdateDocumentPayload,
  UpdateMemberRolePayload,
  UpdateProjectPayload,
  UserCollaborationStats
} from '@/types/collaboration'

class CollaborationService {
  async createProject(payload: CreateProjectPayload): Promise<CollaborationProject> {
    return http.post<CollaborationProject>('/collaboration/projects', payload)
  }

  async getMyProjects(page = 0, size = 10): Promise<PageResult<CollaborationProject>> {
    return http.get<PageResult<CollaborationProject>>('/collaboration/projects', { params: { page, size } })
  }

  async searchProjects(keyword: string, page = 0, size = 10): Promise<PageResult<CollaborationProject>> {
    return http.get<PageResult<CollaborationProject>>('/collaboration/projects/search', {
      params: { keyword, page, size }
    })
  }

  async getProjectById(projectId: number): Promise<CollaborationProject> {
    return http.get<CollaborationProject>(`/collaboration/projects/${projectId}`)
  }

  async updateProject(projectId: number, payload: UpdateProjectPayload): Promise<CollaborationProject> {
    return http.put<CollaborationProject>(`/collaboration/projects/${projectId}`, payload)
  }

  async deleteProject(projectId: number): Promise<void> {
    return http.delete<void>(`/collaboration/projects/${projectId}`)
  }

  async getProjectMembers(projectId: number, page = 0, size = 20): Promise<PageResult<ProjectMember>> {
    return http.get<PageResult<ProjectMember>>(`/collaboration/projects/${projectId}/members`, {
      params: { page, size }
    })
  }

  async addProjectMember(projectId: number, payload: AddMemberPayload): Promise<ProjectMember> {
    return http.post<ProjectMember>(`/collaboration/projects/${projectId}/members`, payload)
  }

  async updateMemberRole(projectId: number, memberId: number, payload: UpdateMemberRolePayload): Promise<ProjectMember> {
    return http.put<ProjectMember>(`/collaboration/projects/${projectId}/members/${memberId}/role`, payload)
  }

  async removeProjectMember(projectId: number, memberId: number): Promise<void> {
    return http.delete<void>(`/collaboration/projects/${projectId}/members/${memberId}`)
  }

  async getProjectStatistics(projectId: number): Promise<ProjectStatistics> {
    return http.get<ProjectStatistics>(`/collaboration/projects/${projectId}/statistics`)
  }

  async createDocument(payload: CreateDocumentPayload): Promise<CollaborativeDocument> {
    return http.post<CollaborativeDocument>('/collaboration/documents', payload)
  }

  async getProjectDocuments(projectId: number, page = 0, size = 20): Promise<PageResult<CollaborativeDocument>> {
    return http.get<PageResult<CollaborativeDocument>>(`/collaboration/projects/${projectId}/documents`, {
      params: { page, size }
    })
  }

  async getDocumentById(documentId: number): Promise<CollaborativeDocument> {
    return http.get<CollaborativeDocument>(`/collaboration/documents/${documentId}`)
  }

  async updateDocument(documentId: number, payload: UpdateDocumentPayload): Promise<CollaborativeDocument> {
    return http.put<CollaborativeDocument>(`/collaboration/documents/${documentId}`, payload)
  }

  async deleteDocument(documentId: number): Promise<void> {
    return http.delete<void>(`/collaboration/documents/${documentId}`)
  }

  async lockDocument(documentId: number): Promise<CollaborativeDocument> {
    return http.post<CollaborativeDocument>(`/collaboration/documents/${documentId}/lock`)
  }

  async unlockDocument(documentId: number): Promise<CollaborativeDocument> {
    return http.post<CollaborativeDocument>(`/collaboration/documents/${documentId}/unlock`)
  }

  async getDocumentComments(documentId: number, page = 0, size = 50): Promise<PageResult<CollaborationComment>> {
    return http.get<PageResult<CollaborationComment>>(`/collaboration/documents/${documentId}/comments`, {
      params: { page, size }
    })
  }

  async addComment(documentId: number, payload: CreateCommentPayload): Promise<CollaborationComment> {
    return http.post<CollaborationComment>(`/collaboration/documents/${documentId}/comments`, payload)
  }

  async updateComment(commentId: number, payload: UpdateCommentPayload): Promise<CollaborationComment> {
    return http.put<CollaborationComment>(`/collaboration/comments/${commentId}`, payload)
  }

  async deleteComment(commentId: number): Promise<void> {
    return http.delete<void>(`/collaboration/comments/${commentId}`)
  }

  async getMyStatistics(): Promise<UserCollaborationStats> {
    return http.get<UserCollaborationStats>('/collaboration/users/me/statistics')
  }

  async initializeRealtimeSession(documentId: number): Promise<RealtimeSession> {
    return http.post<RealtimeSession>(`/collaboration/realtime/documents/${documentId}/session`)
  }

  async joinRealtime(documentId: number): Promise<boolean> {
    return http.post<boolean>(`/collaboration/realtime/documents/${documentId}/join`)
  }

  async leaveRealtime(documentId: number): Promise<boolean> {
    return http.post<boolean>(`/collaboration/realtime/documents/${documentId}/leave`)
  }

  async submitRealtimeEdit(documentId: number, payload: RealtimeEditPayload): Promise<RealtimeEditResult> {
    return http.post<RealtimeEditResult>(`/collaboration/realtime/documents/${documentId}/edit`, payload)
  }

  async syncRealtimeState(documentId: number): Promise<RealtimeDocumentState> {
    return http.get<RealtimeDocumentState>(`/collaboration/realtime/documents/${documentId}/sync`)
  }

  async getRealtimeCollaborators(documentId: number): Promise<RealtimeCollaborator[]> {
    return http.get<RealtimeCollaborator[]>(`/collaboration/realtime/documents/${documentId}/collaborators`)
  }

  async sendRealtimeChat(documentId: number, payload: RealtimeChatPayload): Promise<boolean> {
    return http.post<boolean>(`/collaboration/realtime/documents/${documentId}/chat`, payload)
  }

  async getRealtimeBlocks(documentId: number): Promise<RealtimeDocumentBlock[]> {
    return http.get<RealtimeDocumentBlock[]>(`/collaboration/realtime/documents/${documentId}/blocks`)
  }

  async initializeRealtimeBlocks(documentId: number, payload?: { content?: string }): Promise<RealtimeDocumentBlock[]> {
    return http.post<RealtimeDocumentBlock[]>(`/collaboration/realtime/documents/${documentId}/blocks/init`, payload || {})
  }

  async lockRealtimeBlock(documentId: number, blockId: number): Promise<RealtimeDocumentBlock> {
    return http.post<RealtimeDocumentBlock>(`/collaboration/realtime/documents/${documentId}/blocks/${blockId}/lock`)
  }

  async unlockRealtimeBlock(documentId: number, blockId: number): Promise<RealtimeDocumentBlock> {
    return http.post<RealtimeDocumentBlock>(`/collaboration/realtime/documents/${documentId}/blocks/${blockId}/unlock`)
  }

  async updateRealtimeBlock(
    documentId: number,
    blockId: number,
    payload: RealtimeUpdateBlockPayload
  ): Promise<RealtimeDocumentBlock> {
    return http.put<RealtimeDocumentBlock>(`/collaboration/realtime/documents/${documentId}/blocks/${blockId}`, payload)
  }

  async createRealtimeBlock(documentId: number, payload: RealtimeCreateBlockPayload): Promise<RealtimeDocumentBlock> {
    return http.post<RealtimeDocumentBlock>(`/collaboration/realtime/documents/${documentId}/blocks`, payload)
  }

  async getRealtimeStatistics(): Promise<RealtimeStatistics> {
    return http.get<RealtimeStatistics>('/collaboration/realtime/statistics')
  }
}

export default new CollaborationService()
