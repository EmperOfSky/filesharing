<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const navItems = computed(() => {
  const base = [
    { index: '1', title: '仪表板', route: '/dashboard', icon: 'House' },
    { index: '2', title: '文件管理', route: '/dashboard/files', icon: 'Document' },
    { index: '3', title: '搜索', route: '/dashboard/search', icon: 'Search' },
    { index: '4', title: '分享管理', route: '/dashboard/shares', icon: 'Share' },
    { index: '5', title: '兼容分享', route: '/dashboard/compat-share', icon: 'Connection' },
    { index: '6', title: '个人中心', route: '/dashboard/profile', icon: 'User' }
  ]
  if (isAdmin.value) {
    base.splice(5, 0, { index: '7', title: '兼容配置', route: '/dashboard/compat-config', icon: 'Setting' })
    base.splice(6, 0, { index: '8', title: '兼容记录', route: '/dashboard/compat-records', icon: 'List' })
  }
  return base
})

const activeIndex = computed(() => {
  const item = navItems.value.find((nav) => nav.route === route.path)
  if (item) {
    return item.index
  }
  if (route.path === '/dashboard') {
    return '1'
  }
  return '1'
})

const handleSelect = (key: string) => {
  const item = navItems.value.find(nav => nav.index === key)
  if (item) {
    router.push(item.route)
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    authStore.logout()
    router.push('/login')
  } catch {
    // 用户取消操作
  }
}

onMounted(() => {
  // 初始化时获取当前用户信息
  if (authStore.token) {
    authStore.fetchCurrentUser().catch(() => {
      authStore.logout()
      router.push('/login')
    })
  }
})
</script>

<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <div class="logo">
          <el-icon size="24"><FolderOpened /></el-icon>
          <span class="logo-text">文件共享系统</span>
        </div>
      </div>
      
      <div class="header-center">
        <el-menu
          :default-active="activeIndex"
          class="nav-menu"
          mode="horizontal"
          @select="handleSelect"
        >
          <el-menu-item 
            v-for="item in navItems" 
            :key="item.index" 
            :index="item.index"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </div>
      
      <div class="header-right">
        <el-dropdown>
          <div class="user-info">
            <el-avatar :size="32" :src="authStore.user?.avatar">
              {{ authStore.user?.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span class="username">{{ authStore.user?.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/dashboard/profile')">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item @click="handleLogout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    
    <el-main class="layout-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: white;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  flex: 0 0 auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav-menu {
  border: none;
  background: transparent;
}

.header-right {
  flex: 0 0 auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.layout-main {
  padding: 20px;
  background-color: #f5f5f5;
  overflow-y: auto;
}
</style>