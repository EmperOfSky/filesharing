import http from './http'

export interface SystemMetrics {
  timestamp: string
  heapMemoryUsed: number
  heapMemoryMax: number
  heapMemoryUsage: number
  nonHeapMemoryUsed: number
  nonHeapMemoryMax: number
  threadCount: number
  peakThreadCount: number
  daemonThreadCount: number
  uptime: number
  systemLoadAverage: number
}

export interface HealthComponentInfo {
  status?: string
  details?: Record<string, unknown>
  [key: string]: unknown
}

export interface HealthCheckResult {
  timestamp: string
  status: string | { status?: string; code?: string }
  components: Record<string, HealthComponentInfo>
}

export interface MetricPoint {
  timestamp: string
  value: number
}

export interface MetricHistory {
  metricName: string
  dataPoints: MetricPoint[]
  count: number
}

export interface MetricStats {
  minimum: number
  maximum: number
  average: number
  sum: number
}

export interface PerformanceTrend {
  metricName: string
  trendDirection: string
  severity: string
}

export interface PerformanceReport {
  generatedAt: string
  periodDays: number
  cpuUsageStats: MetricStats
  memoryUsageStats: MetricStats
  diskIoStats: MetricStats
  networkTrafficStats: MetricStats
  healthStatusStats: Record<string, number>
  totalAlerts: number
  trends: PerformanceTrend[]
  error?: string
}

export interface MonitoringAlert {
  id: string
  level: string
  title: string
  message: string
  timestamp: string
  status: string
  closedAt?: string
}

export interface MonitoringAlertListResponse {
  alerts: MonitoringAlert[]
  count: number
  total: number
}

export interface MonitoringStatistics {
  activeMetrics: string
  monitoredComponents: string
  alertCount: number
  lastCheckTime: number
}

class MonitoringService {
  async getSystemMetrics(): Promise<SystemMetrics> {
    return http.get<SystemMetrics>('/monitoring/metrics')
  }

  async getHealthCheck(): Promise<HealthCheckResult> {
    return http.get<HealthCheckResult>('/monitoring/health')
  }

  async getMetricHistory(metricName: string, hours = 24): Promise<MetricHistory> {
    return http.get<MetricHistory>('/monitoring/metrics/history', {
      params: {
        metricName,
        hours
      }
    })
  }

  async getPerformanceReport(days = 7): Promise<PerformanceReport> {
    return http.get<PerformanceReport>('/monitoring/report', {
      params: { days }
    })
  }

  async getAlerts(params: { level?: string; status?: string; limit?: number } = {}): Promise<MonitoringAlertListResponse> {
    return http.get<MonitoringAlertListResponse>('/monitoring/alerts', {
      params
    })
  }

  async closeAlert(alertId: string): Promise<void> {
    return http.put<void>(`/monitoring/alerts/${alertId}/close`)
  }

  async getStatistics(): Promise<MonitoringStatistics> {
    return http.get<MonitoringStatistics>('/monitoring/statistics')
  }
}

export default new MonitoringService()