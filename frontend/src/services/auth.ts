import http from './http'
import { LoginRequest, RegisterRequest, AuthResponse, User } from '@/types'

class AuthService {
  async login(data: LoginRequest): Promise<string | AuthResponse> {
    return http.post<string | AuthResponse>('/auth/login', {
      identifier: data.identifier,
      password: data.password
    })
  }

  async register(data: RegisterRequest): Promise<User> {
    return http.post<User>('/auth/register', data)
  }

  async logout(): Promise<void> {
    return Promise.resolve()
  }

  async getCurrentUser(): Promise<User> {
    return http.get<User>('/auth/me')
  }

  async updateProfile(userData: Partial<User>): Promise<User> {
    return http.put<User>('/users/profile', userData)
  }

  async changePassword(oldPassword: string, newPassword: string): Promise<string> {
    return http.post<string>('/users/change-password', {
      oldPassword,
      newPassword
    })
  }
}

export default new AuthService()