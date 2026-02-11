import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import authService from '../services/authService';

const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
      error: null,

      // 用户登录
      login: async (credentials) => {
        set({ loading: true, error: null });
        try {
          const response = await authService.login(credentials);
          const { user, token } = response.data;
          
          set({
            user,
            token,
            isAuthenticated: true,
            loading: false
          });
          
          return response;
        } catch (error) {
          set({ 
            error: error.response?.data?.message || '登录失败',
            loading: false 
          });
          throw error;
        }
      },

      // 用户注册
      register: async (userData) => {
        set({ loading: true, error: null });
        try {
          const response = await authService.register(userData);
          const { user, token } = response.data;
          
          set({
            user,
            token,
            isAuthenticated: true,
            loading: false
          });
          
          return response;
        } catch (error) {
          set({ 
            error: error.response?.data?.message || '注册失败',
            loading: false 
          });
          throw error;
        }
      },

      // 用户登出
      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          error: null
        });
      },

      // 获取当前用户信息
      getCurrentUser: async () => {
        set({ loading: true });
        try {
          const response = await authService.getCurrentUser();
          set({
            user: response.data,
            isAuthenticated: true,
            loading: false
          });
          return response.data;
        } catch (error) {
          set({ 
            error: error.response?.data?.message || '获取用户信息失败',
            loading: false 
          });
          throw error;
        }
      },

      // 清除错误
      clearError: () => set({ error: null }),

      // 更新用户信息
      updateUser: (userData) => set((state) => ({
        user: { ...state.user, ...userData }
      }))
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ 
        user: state.user, 
        token: state.token, 
        isAuthenticated: state.isAuthenticated 
      })
    }
  )
);

export default useAuthStore;