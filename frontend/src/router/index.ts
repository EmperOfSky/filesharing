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
      path: '/pickup-space',
      name: 'pickup-space-public',
      component: () => import('@/pages/PickupSpacePage.vue')
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
          path: 'quick-transfer',
          name: 'quick-transfer-center',
          component: () => import('@/pages/QuickTransferCenterPage.vue')
        },
        {
          path: 'quick-transfer/share',
          name: 'quick-transfer-share',
          component: () => import('@/pages/FileCodeBoxSharePage.vue')
        },
        {
          path: 'pickup-space',
          name: 'pickup-space',
          component: () => import('@/pages/PickupSpacePage.vue')
        },
        {
          path: 'quick-transfer/config',
          name: 'quick-transfer-config',
          component: () => import('@/pages/FileCodeBoxConfigPage.vue')
        },
        {
          path: 'quick-transfer/records',
          name: 'quick-transfer-records',
          component: () => import('@/pages/FileCodeBoxRecordsPage.vue')
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
          path: 'recycle-bin',
          name: 'recycle-bin',
          component: () => import('@/pages/RecycleBinPage.vue')
        },
        {
          path: 'recommendations',
          name: 'recommendations',
          component: () => import('@/pages/RecommendationPage.vue')
        },
        {
          path: 'backup',
          name: 'backup',
          component: () => import('@/pages/BackupPage.vue')
        },
        {
          path: 'system-load',
          name: 'system-load',
          component: () => import('@/pages/SystemLoadPage.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'collaboration',
          name: 'collaboration-projects',
          component: () => import('@/pages/CollaborationProjectsPage.vue')
        },
        {
          path: 'collaboration/:projectId',
          name: 'collaboration-workspace',
          component: () => import('@/pages/CollaborationWorkspacePage.vue'),
          props: true
        },
        {
          path: 'compat-share',
          redirect: '/dashboard/quick-transfer/share'
        },
        {
          path: 'compat-config',
          redirect: '/dashboard/quick-transfer/config'
        },
        {
          path: 'compat-records',
          redirect: '/dashboard/quick-transfer/records'
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
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  const pickupCode = typeof to.query.code === 'string' ? to.query.code.trim() : ''
  const isDashboardPickupRoute = to.name === 'pickup-space'

  // 兼容旧链接：未登录访问 /dashboard/pickup-space?code=... 时跳转到公共取件页
  if (!authStore.isAuthenticated && isDashboardPickupRoute && pickupCode) {
    next({ path: '/pickup-space', query: { code: pickupCode } })
    return
  }

  if (authStore.isAuthenticated && !authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      next('/login')
      return
    }
  }
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next('/dashboard')
  } else if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    next('/dashboard')
  } else {
    next()
  }
})

export default router