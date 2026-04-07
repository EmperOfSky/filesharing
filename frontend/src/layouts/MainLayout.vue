<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

interface NavItem {
  key: string
  title: string
  route: string
  icon: string
  match: string[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const mobileNavVisible = ref(false)

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const navItems = computed<NavItem[]>(() => [
  {
    key: 'dashboard',
    title: '工作台',
    route: '/dashboard',
    icon: 'House',
    match: ['/dashboard']
  },
  {
    key: 'quick-transfer',
    title: '快传中心',
    route: '/dashboard/quick-transfer',
    icon: 'Connection',
    match: ['/dashboard/quick-transfer']
  },
  {
    key: 'files',
    title: '文件管理',
    route: '/dashboard/files',
    icon: 'FolderOpened',
    match: ['/dashboard/files']
  },
  {
    key: 'search',
    title: '搜索',
    route: '/dashboard/search',
    icon: 'Search',
    match: ['/dashboard/search']
  },
  {
    key: 'shares',
    title: '分享管理',
    route: '/dashboard/shares',
    icon: 'Share',
    match: ['/dashboard/shares']
  },
  {
    key: 'recycle',
    title: '回收站',
    route: '/dashboard/recycle-bin',
    icon: 'Delete',
    match: ['/dashboard/recycle-bin']
  },
  {
    key: 'recommendation',
    title: '智能推荐',
    route: '/dashboard/recommendations',
    icon: 'Star',
    match: ['/dashboard/recommendations']
  },
  {
    key: 'collaboration',
    title: '协作文档',
    route: '/dashboard/collaboration',
    icon: 'EditPen',
    match: ['/dashboard/collaboration']
  },
  {
    key: 'pickup',
    title: '取件空间',
    route: '/dashboard/pickup-space',
    icon: 'FolderChecked',
    match: ['/dashboard/pickup-space']
  },
  {
    key: 'backup',
    title: '数据备份',
    route: '/dashboard/backup',
    icon: 'Coin',
    match: ['/dashboard/backup']
  }
])

const activeKey = computed(() => {
  const currentPath = route.path

  if (currentPath === '/dashboard' || currentPath === '/dashboard/') {
    return 'dashboard'
  }

  const matched = navItems.value.find((item) =>
    item.match.some((prefix) =>
      prefix === '/dashboard'
        ? currentPath === '/dashboard'
        : currentPath.startsWith(prefix)
    )
  )

  return matched?.key || 'dashboard'
})

const currentSection = computed(() => {
  const matched = navItems.value.find((item) => item.key === activeKey.value)
  return matched?.title || '工作台'
})

const roleLabel = computed(() => (isAdmin.value ? '管理员' : '成员账号'))

const navigateTo = (targetRoute: string) => {
  mobileNavVisible.value = false
  if (route.path !== targetRoute) {
    router.push(targetRoute)
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })

    authStore.logout()
    router.push('/login')
  } catch {
    // 用户取消退出
  }
}

onMounted(() => {
  if (!authStore.token) {
    router.push('/login')
    return
  }

  authStore.fetchCurrentUser().catch(() => {
    authStore.logout()
    router.push('/login')
  })
})
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-inner">
        <button class="brand-block" type="button" @click="navigateTo('/dashboard')">
          <div class="brand-mark">
            <el-icon :size="20"><FolderOpened /></el-icon>
          </div>
          <div class="brand-copy">
            <span class="brand-kicker">FILE SHARING STUDIO</span>
            <strong>文件共享中心</strong>
          </div>
        </button>

        <nav class="nav-cluster" aria-label="主导航">
          <button
            v-for="item in navItems"
            :key="item.key"
            type="button"
            class="nav-pill"
            :class="{ active: activeKey === item.key }"
            @click="navigateTo(item.route)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </button>
        </nav>

        <div class="header-right">
          <div class="header-chip">
            <span class="chip-label">当前区块</span>
            <strong>{{ currentSection }}</strong>
          </div>
          <div class="header-chip subtle">
            <span class="chip-label">账户角色</span>
            <strong>{{ roleLabel }}</strong>
          </div>
          <div v-if="isAdmin" class="header-tip">
          </div>
          <el-dropdown trigger="click">
            <button type="button" class="user-pill">
              <el-avatar :size="38" :src="authStore.user?.avatar">
                {{ authStore.user?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="user-copy">
                <strong>{{ authStore.user?.username || '未登录' }}</strong>
                <span>{{ authStore.user?.email || '点击查看账户操作' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="navigateTo('/dashboard/profile')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="navigateTo('/dashboard/quick-transfer')">
                  <el-icon><Connection /></el-icon>
                  快传中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            class="mobile-trigger"
            plain
            circle
            @click="mobileNavVisible = true"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
        </div>
      </div>
    </header>

    <main class="app-main">
      <div class="app-main-inner">
        <router-view />
      </div>
    </main>

    <el-drawer
      v-model="mobileNavVisible"
      title="导航"
      direction="rtl"
      size="320px"
      class="mobile-drawer"
    >
      <div class="mobile-drawer-inner">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          class="mobile-nav-item"
          :class="{ active: activeKey === item.key }"
          @click="navigateTo(item.route)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  padding: 20px 20px 0;
}

.header-inner {
  width: min(var(--fs-content-width), 100%);
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(148, 163, 184, 0.30);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.90);
  box-shadow: var(--fs-shadow-md);
  backdrop-filter: blur(22px);
}

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  border: none;
  padding: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--fs-brand-1) 0%, var(--fs-brand-2) 100%);
  color: white;
  box-shadow: 0 16px 34px rgba(14, 165, 233, 0.30);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.brand-kicker {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #0369a1;
}

.brand-copy strong {
  font-size: 18px;
  color: var(--fs-text-1);
}

.nav-cluster {
  flex: 1;
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 10px;
  min-width: 0;
}

.nav-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 11px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(248, 251, 255, 0.96);
  color: var(--fs-text-2);
  font-size: 12px;
  font-weight: 700;
  line-height: 2;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.nav-pill:hover {
  transform: translateY(-1px);
  border-color: rgba(129, 152, 237, 0.48);
  color: #d7ddff;
  background: rgba(34, 44, 88, 0.84);
}

.nav-pill.active {
  border-color: rgba(129, 152, 237, 0.52);
  background: linear-gradient(135deg, rgba(109, 120, 247, 0.34), rgba(243, 71, 183, 0.26));
  color: #f1f4ff;
  box-shadow: 0 14px 28px rgba(109, 120, 247, 0.26);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-chip {
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 14px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(248, 251, 255, 0.96);
}

.header-chip.subtle {
  background: rgba(241, 245, 249, 0.92);
}

.chip-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--fs-text-3);
}

.header-chip strong {
  font-size: 13px;
  color: var(--fs-text-1);
}

.header-tip {
  max-width: 220px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--fs-text-3);
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 7px 9px 7px 7px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(248, 251, 255, 0.96);
  color: inherit;
  cursor: pointer;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.user-copy strong {
  font-size: 13px;
  color: var(--fs-text-1);
}

.user-copy span {
  max-width: 180px;
  color: var(--fs-text-3);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-trigger {
  display: none;
}

.app-main {
  padding: 22px 20px 28px;
}

.app-main-inner {
  width: min(var(--fs-content-width), 100%);
  margin: 0 auto;
}

.mobile-drawer-inner {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--fs-text-2);
  font-weight: 700;
  cursor: pointer;
}

.mobile-nav-item.active {
  border-color: rgba(14, 165, 233, 0.4);
  color: #0f172a;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.16), rgba(37, 99, 235, 0.14));
}

@media (max-width: 1380px) {
  .header-chip,
  .header-tip {
    display: none;
  }
}

@media (max-width: 1180px) {
  .nav-cluster {
    display: none;
  }

  .mobile-trigger {
    display: inline-flex;
  }

  .header-inner {
    justify-content: space-between;
  }
}

@media (max-width: 720px) {
  .app-header {
    padding: 14px 14px 0;
  }

  .app-main {
    padding: 18px 14px 24px;
  }

  .header-inner {
    padding: 14px;
    gap: 12px;
  }

  .user-copy {
    display: none;
  }

  .brand-copy strong {
    font-size: 16px;
  }
}
</style>

