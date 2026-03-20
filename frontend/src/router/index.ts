import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/dashboard'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/RegisterPage.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/s/:shareKey',
      name: 'share-access',
      component: () => import('@/pages/ShareAccessPage.vue')
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard-main',
          component: () => import('@/pages/Dashboard.vue')
        },
        {
          path: 'files',
          name: 'files',
          component: () => import('@/pages/FileManager.vue')
        },
        {
          path: 'preview/:id',
          name: 'file-preview',
          component: () => import('@/pages/FilePreview.vue'),
          props: true
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/pages/SearchPage.vue')
        },
        {
          path: 'shares',
          name: 'shares',
          component: () => import('@/pages/ShareManagePage.vue')
        },
        {
          path: 'compat-share',
          name: 'compat-share',
          component: () => import('@/pages/FileCodeBoxSharePage.vue')
        },
        {
          path: 'compat-config',
          name: 'compat-config',
          component: () => import('@/pages/FileCodeBoxConfigPage.vue')
        },
        {
          path: 'compat-records',
          name: 'compat-records',
          component: () => import('@/pages/FileCodeBoxRecordsPage.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/pages/UserProfile.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router