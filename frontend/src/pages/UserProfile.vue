<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
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
})
</script>

<template>
  <div class="user-profile">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="profile-sidebar">
          <div class="user-avatar">
            <el-avatar :size="100" :src="profileForm.avatar">
              {{ profileForm.username.charAt(0).toUpperCase() }}
            </el-avatar>
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
            >
              <el-button size="small" type="primary">更换头像</el-button>
            </el-upload>
          </div>
          
          <div class="user-info">
            <h3>{{ profileForm.username }}</h3>
            <p>{{ profileForm.email }}</p>
            <p class="member-since">注册时间: {{ authStore.user?.createdAt ? new Date(authStore.user.createdAt).toLocaleDateString() : '' }}</p>
          </div>
          
          <el-divider />
          
          <div class="quick-actions">
            <el-button 
              type="danger" 
              size="small" 
              @click="handleLogout"
              class="logout-btn"
            >
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="18">
        <el-card class="profile-content">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="个人信息" name="profile">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="100px"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="profileForm.username" />
                </el-form-item>
                
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" />
                </el-form-item>
                
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="loading"
                    @click="handleProfileUpdate(profileFormRef)"
                  >
                    保存更改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="loading"
                    @click="handlePasswordChange(passwordFormRef)"
                  >
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <el-tab-pane label="账户统计" name="stats">
              <div class="stats-grid">
                <el-card class="stat-card">
                  <div class="stat-content">
                    <div class="stat-icon bg-blue">
                      <el-icon size="24"><Document /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-number">0</div>
                      <div class="stat-label">文件数量</div>
                    </div>
                  </div>
                </el-card>
                
                <el-card class="stat-card">
                  <div class="stat-content">
                    <div class="stat-icon bg-green">
                      <el-icon size="24"><Coin /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-number">0 MB</div>
                      <div class="stat-label">存储使用</div>
                    </div>
                  </div>
                </el-card>
                
                <el-card class="stat-card">
                  <div class="stat-content">
                    <div class="stat-icon bg-orange">
                      <el-icon size="24"><Share /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-number">0</div>
                      <div class="stat-label">分享次数</div>
                    </div>
                  </div>
                </el-card>
                
                <el-card class="stat-card">
                  <div class="stat-content">
                    <div class="stat-icon bg-purple">
                      <el-icon size="24"><Download /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-number">0</div>
                      <div class="stat-label">下载次数</div>
                    </div>
                  </div>
                </el-card>
              </div>
              
              <div class="activity-section">
                <h4>最近活动</h4>
                <el-timeline>
                  <el-timeline-item
                    v-for="(activity, index) in recentActivities"
                    :key="index"
                    :timestamp="activity.timestamp"
                    placement="top"
                  >
                    {{ activity.description }}
                  </el-timeline-item>
                  <el-timeline-item>
                    <p>暂无活动记录</p>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.user-profile {
  padding: 20px;
}

.profile-sidebar {
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  margin-bottom: 20px;
}

.avatar-uploader {
  margin-top: 15px;
}

.user-info h3 {
  margin: 15px 0 5px 0;
  color: #333;
}

.user-info p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.member-since {
  font-size: 12px !important;
  color: #999 !important;
}

.quick-actions {
  padding: 10px 0;
}

.logout-btn {
  width: 100%;
}

.profile-content {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.bg-blue { background-color: #409eff; }
.bg-green { background-color: #67c23a; }
.bg-orange { background-color: #e6a23c; }
.bg-purple { background-color: #722ed1; }

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.activity-section h4 {
  margin-bottom: 20px;
  color: #333;
}
</style>