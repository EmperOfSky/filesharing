import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/auth'
import { User, LoginRequest, RegisterRequest } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  const isAuthenticated = computed(() => !!token.value)

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials)
      const authToken = typeof response === 'string' ? response : response.token

      if (!authToken) {
        throw new Error('登录响应缺少 token')
      }

      token.value = authToken
      localStorage.setItem('token', authToken)

      if (typeof response === 'object' && response.user) {
        user.value = response.user
      } else {
        // 后端当前只返回 token，登录后主动拉取一次用户信息
        await fetchCurrentUser()
      }

      return response
    } catch (error) {
      throw error
    }
  }

  const register = async (userData: RegisterRequest) => {
    try {
      const createdUser = await authService.register(userData)
      user.value = createdUser
      return createdUser
    } catch (error) {
      throw error
    }
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    authService.logout().catch(() => {})
  }

  const fetchCurrentUser = async () => {
    if (!token.value) return
    
    try {
      const currentUser = await authService.getCurrentUser()
      user.value = currentUser
    } catch (error) {
      logout()
      throw error
    }
  }

  const updateProfile = async (userData: Partial<User>) => {
    try {
      const updatedUser = await authService.updateProfile(userData)
      user.value = updatedUser
      return updatedUser
    } catch (error) {
      throw error
    }
  }

  const changePassword = async (oldPassword: string, newPassword: string) => {
    try {
      return await authService.changePassword(oldPassword, newPassword)
    } catch (error) {
      throw error
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    login,
    register,
    logout,
    fetchCurrentUser,
    updateProfile,
    changePassword
  }
})