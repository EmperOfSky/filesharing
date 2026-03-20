import http from './http'
import type {
  PaginatedResponse,
  RecommendationAnalytics,
  RecommendationCleanupResult,
  RecommendationListResponse,
  RecommendationType,
  SmartRecommendation
} from '@/types'

class RecommendationService {
  async generateRecommendations(): Promise<SmartRecommendation[]> {
    const data = await http.post<SmartRecommendation[]>('/recommendations/generate')
    return data || []
  }

  async getRecommendations(page = 0, size = 10): Promise<PaginatedResponse<SmartRecommendation>> {
    const result = await http.get<RecommendationListResponse>('/recommendations', {
      params: { page, size }
    })

    const content = Array.isArray(result?.content)
      ? result.content
      : Array.isArray(result?.recommendations)
        ? result.recommendations
        : []

    const number = typeof result?.number === 'number'
      ? result.number
      : typeof result?.currentPage === 'number'
        ? result.currentPage
        : page

    const totalPages = typeof result?.totalPages === 'number'
      ? result.totalPages
      : 0

    const totalElements = typeof result?.totalElements === 'number'
      ? result.totalElements
      : content.length

    const pageSize = typeof result?.size === 'number'
      ? result.size
      : size

    const numberOfElements = typeof result?.numberOfElements === 'number'
      ? result.numberOfElements
      : content.length

    const first = typeof result?.first === 'boolean'
      ? result.first
      : number <= 0

    const last = typeof result?.last === 'boolean'
      ? result.last
      : totalPages <= 0 || number >= totalPages - 1

    return {
      content,
      totalPages,
      totalElements,
      number,
      size: pageSize,
      numberOfElements,
      first,
      last
    }
  }

  async markAsViewed(id: number): Promise<void> {
    await http.put(`/recommendations/${id}/view`, {})
  }

  async markAsAdopted(id: number): Promise<void> {
    await http.put(`/recommendations/${id}/adopt`, {})
  }

  async getAnalytics(): Promise<RecommendationAnalytics> {
    return http.get<RecommendationAnalytics>('/recommendations/analytics')
  }

  async getSimilarRecommendations(itemId: number, type: RecommendationType): Promise<SmartRecommendation[]> {
    const data = await http.get<SmartRecommendation[]>(`/recommendations/similar/${itemId}`, {
      params: { type }
    })
    return data || []
  }

  async cleanupExpiredRecommendations(): Promise<RecommendationCleanupResult> {
    return http.delete<RecommendationCleanupResult>('/recommendations/cleanup')
  }
}

export default new RecommendationService()
