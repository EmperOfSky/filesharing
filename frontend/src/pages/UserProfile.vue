<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Calendar } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import UiStatCard from '@/components/ui/UiStatCard.vue'

const authStore = useAuthStore()
const loading = ref(false)
const statsLoading = ref(false)
const activeTab = ref('profile')
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  username: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const recentActivities = ref<Array<{ timestamp: string; description: string }>>([])

const accountStats = reactive({
  files: 0,
  folders: 0,
  shares: 0
})

const roleLabel = computed(() => {
  return authStore.user?.role || '普通用户'
})

const accountAgeDays = computed(() => {
  if (!authStore.user?.createdAt) return 0
  const createdAt = new Date(authStore.user.createdAt)
  const now = new Date()
  return Math.floor((now.getTime() - createdAt.getTime()) / (1000 * 60 * 60 * 24))
})

const joinedAtText = computed(() => {
  if (!authStore.user?.createdAt) return '未知'
  return new Date(authStore.user.createdAt).toLocaleDateString('zh-CN')
})

const profileRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应在3-20个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
})

const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
})

const loadUserProfile = () => {
  if (authStore.user) {
    profileForm.username = authStore.user.username
    profileForm.email = authStore.user.email
    profileForm.avatar = authStore.user.avatar || ''
  }
}

const handleProfileUpdate = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.updateProfile({
          username: profileForm.username,
          email: profileForm.email
        })
        ElMessage.success('个人信息更新成功')
      } catch (error: any) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handlePasswordChange = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
        ElMessage.success('密码修改成功')
        // 重置表单
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleAvatarUpload = async (file: File) => {
  try {
    // 这里应该上传头像到服务器
    const reader = new FileReader()
    reader.onload = (e) => {
      profileForm.avatar = e.target?.result as string
    }
    reader.readAsDataURL(file)
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error('头像上传失败')
  }
}

const handleAvatarChange = (file: UploadFile) => {
  const raw = file.raw as File | undefined
  if (!raw) {
    ElMessage.warning('未获取到头像文件')
    return
  }
  handleAvatarUpload(raw)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    authStore.logout()
    window.location.href = '/login'
  } catch {
    // 用户取消操作
  }
}

onMounted(() => {
  loadUserProfile()
  loadAccountStats()
})

const loadAccountStats = async () => {
  statsLoading.value = true
  try {
    // TODO: 从后端 API 获取统计数据
    // 临时使用默认值
    accountStats.files = 0
    accountStats.folders = 0
    accountStats.shares = 0
    
    // TODO: 从后端 API 获取最近动态
    recentActivities.value = []
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计信息失败')
  } finally {
    statsLoading.value = false
  }
}
</script>

<template>
  <div class="modern-workspace user-profile-page">
    <header class="workspace-header base-card">
      <div class="header-content">
        <div class="header-intro">
          <div class="kicker-tag">Profile Center</div>
          <h1 class="title">个人中心</h1>
          <p class="description" style="color: var(--fs-text-2); margin-top: 8px;">把账号资料、安全设置和使用概况整合成一个更清晰的个人中心，您可以在这里修改信息及查看统计。</p>
          <div class="hero-badges" style="margin-top: 16px; display: flex; gap: 12px;">
            <span class="hero-badge" style="display: flex; align-items: center; gap: 6px; color: var(--fs-text-2); background: var(--fs-bg-1); padding: 4px 12px; border-radius: 99px; font-size: 13px;">
              <el-icon><User /></el-icon>
              当前角色：{{ roleLabel }}
            </span>
            <span class="hero-badge" style="display: flex; align-items: center; gap: 6px; color: var(--fs-text-2); background: var(--fs-bg-1); padding: 4px 12px; border-radius: 99px; font-size: 13px;">
              <el-icon><Calendar /></el-icon>
              已注册 {{ accountAgeDays }} 天
            </span>
          </div>
        </div>

        <div class="header-stats">
          <UiStatCard label="文件数" :value="accountStats.files" hint="来自当前账号文件统计" />
          <UiStatCard label="目录数" :value="accountStats.folders" hint="便于快速掌握当前结构规模" />
          <UiStatCard label="分享数" :value="accountStats.shares" hint="短链分享累计数量" />
          <UiStatCard label="状态" :value="authStore.isAuthenticated ? '在线' : '离线'" hint="登录状态会影响个人中心操作" />
        </div>
      </div>
    </header>

    <main class="workspace-main split-grid profile-grid">
      <section class="base-card profile-side" style="padding: 24px;">
        <div class="avatar-panel">
          <el-avatar :size="108" :src="profileForm.avatar" style="border: 2px solid var(--fs-border-color); font-size: 36px; background: var(--fs-bg-1); color: var(--fs-text-1);">
            {{ profileForm.username.charAt(0).toUpperCase() }}
          </el-avatar>
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
          >
            <el-button plain round>替换头像</el-button>
          </el-upload>
        </div>

        <div class="profile-meta">
          <h2 style="font-size: 20px; font-weight: 600; margin-bottom: 8px; color: var(--fs-text-1);">{{ profileForm.username || '未命名用户' }}</h2>
          <p style="color: var(--fs-text-2); margin-bottom: 12px;">{{ profileForm.email || '暂无邮箱' }}</p>
          <span style="display: inline-block; padding: 4px 12px; background: var(--fs-bg-1); border-radius: 99px; font-size: 13px; color: var(--fs-text-2);">加入时间：{{ joinedAtText }}</span>
        </div>

        <div class="stack-grid profile-side-stats">
          <UiStatCard label="账号角色" :value="roleLabel" />
          <UiStatCard label="账号时长" :value="`${accountAgeDays} 天`" />
        </div>

        <el-button type="danger" plain style="width: 100%;" @click="handleLogout" round>
          退出登录
        </el-button>
      </section>

      <section class="base-card" style="padding: 24px;">
        <el-tabs v-model="activeTab" class="modern-tabs">
          <el-tab-pane label="个人资料" name="profile">
            <div style="padding: 16px 0;">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-position="top"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="profileForm.username" size="large" />
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" size="large" />
                </el-form-item>

                <div class="form-actions" style="margin-top: 32px;">
                  <el-button type="primary" size="large" :loading="loading" @click="handleProfileUpdate(profileFormRef)" round>
                    保存修改
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="安全设置" name="password">
            <div style="padding: 16px 0;">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-position="top"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password size="large" />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password size="large" />
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password size="large" />
                </el-form-item>

                <div class="form-actions" style="margin-top: 32px;">
                  <el-button type="primary" size="large" :loading="loading" @click="handlePasswordChange(passwordFormRef)" round>
                    更新密码
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="使用概况" name="stats">
            <div v-loading="statsLoading" class="stack-grid" style="padding: 16px 0; gap: 32px;">
              <div class="stats-cards">
                <UiStatCard label="文件" :value="accountStats.files" />
                <UiStatCard label="目录" :value="accountStats.folders" />
                <UiStatCard label="分享" :value="accountStats.shares" />
              </div>

              <div class="activity-panel">
                <h3 style="font-size: 16px; font-weight: 500; margin-bottom: 20px; color: var(--fs-text-1);">最近动态</h3>
                <el-timeline>
                  <el-timeline-item
                    v-for="(activity, index) in recentActivities"
                    :key="index"
                    :timestamp="activity.timestamp"
                    placement="top"
                  >
                    <div style="color: var(--fs-text-2);">{{ activity.description }}</div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </main>
  </div>
</template>

<style scoped>
.split-grid {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 24px;
}

.profile-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.profile-side-stats {
  display: grid;
  gap: 12px;
  width: 100%;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.profile-meta {
  text-align: center;
}

.profile-meta h2 {
  font-size: 24px;
  color: var(--fs-text-1);
}

.profile-meta p {
  margin-top: 10px;
  color: var(--fs-text-2);
}

.profile-meta span {
  margin-top: 16px;
  display: inline-block;
  padding: 4px 12px;
  background: var(--fs-bg-2);
  border-radius: 99px;
  font-size: 13px;
  color: var(--fs-text-2);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

:deep(.modern-tabs .el-tabs__item) {
  font-size: 15px;
  padding: 0 24px;
}

@media (max-width: 900px) {
  .split-grid {
    grid-template-columns: 1fr;
  }
}
</style>
