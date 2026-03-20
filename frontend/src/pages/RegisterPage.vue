<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Message } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const registerFormRef = ref<FormInstance>()

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应在 3 到 20 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
})

const handleRegister = async (formEl?: FormInstance) => {
  if (!formEl) return

  await formEl.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.register({
        username: registerForm.username,
        email: registerForm.email,
        password: registerForm.password,
        confirmPassword: registerForm.confirmPassword
      })
      ElMessage.success('注册成功，请登录')
      setTimeout(() => {
        router.push('/login')
      }, 800)
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || '注册失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="modern-auth-page">
    <div class="auth-card base-card">
      <!-- 左侧：品牌展示 -->
      <section class="auth-showcase">
        <div class="showcase-content">
          <span class="kicker-tag">Create Account</span>
          <h1>加入协作工作区</h1>
          <p class="description">注册账号，开启您的智能文件管理与团队协作之旅。</p>
          
          <div class="highlights">
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><FolderOpened /></el-icon></div>
              <div>
                <strong>全能工作台</strong>
                <p>文件管理、快传、回收站一站式集成</p>
              </div>
            </div>
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><Connection /></el-icon></div>
              <div>
                <strong>实时协作</strong>
                <p>项目进度、文档编辑、多人在线协作</p>
              </div>
            </div>
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><RefreshRight /></el-icon></div>
              <div>
                <strong>数据安全</strong>
                <p>支持备份恢复与细粒度的权限控制</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧：注册面板 -->
      <section class="auth-panel">
        <div class="panel-head">
          <h2>注册新账号</h2>
          <p>填写以下信息，开通您的团队工作区</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          @keyup.enter="handleRegister(registerFormRef)"
          class="modern-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="registerForm.username" size="large" placeholder="您的称呼" :prefix-icon="User" class="modern-input" />
          </el-form-item>

          <el-form-item label="电子邮箱" prop="email">
            <el-input v-model="registerForm.email" size="large" placeholder="name@domain.com" :prefix-icon="Message" class="modern-input" />
          </el-form-item>

          <el-form-item label="设置密码" prop="password">
            <el-input v-model="registerForm.password" size="large" type="password" show-password placeholder="至少 6 位" :prefix-icon="Lock" class="modern-input" />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" size="large" type="password" show-password placeholder="再次输入密码" :prefix-icon="Lock" class="modern-input" />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" color="#6366f1" size="large" :loading="loading" @click="handleRegister(registerFormRef)" round class="login-btn">
              立即注册
            </el-button>
            <el-button size="large" @click="goToLogin" round plain class="register-btn">
              返回登录
            </el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.modern-auth-page {
  --primary-color: #6366f1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  padding: 20px;
}

.auth-card {
  display: grid;
  grid-template-columns: 480px 420px;
  max-width: 900px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

/* 左侧 showcase */
.auth-showcase {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  padding: 48px;
  display: flex;
  align-items: center;
}
.kicker-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: rgba(255,255,255,0.2);
  padding: 6px 14px;
  border-radius: 20px;
  margin-bottom: 24px;
}
.showcase-content h1 { font-size: 32px; margin: 0 0 20px 0; }
.description { font-size: 15px; opacity: 0.9; line-height: 1.6; }
.highlights { display: flex; flex-direction: column; gap: 24px; margin-top: 40px; }
.highlight-item { display: flex; gap: 16px; align-items: flex-start; }
.icon-wrap { width: 40px; height: 40px; background: rgba(255,255,255,0.1); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.highlight-item strong { font-size: 15px; display: block; margin-bottom: 4px; }
.highlight-item p { font-size: 13px; opacity: 0.8; margin: 0; }

/* 右侧面板 */
.auth-panel { padding: 48px; }
.panel-head { margin-bottom: 32px; }
.panel-head h2 { font-size: 24px; color: #0f172a; margin: 0 0 8px 0; }
.panel-head p { font-size: 14px; color: #64748b; margin: 0; }

.modern-input :deep(.el-input__wrapper) { border-radius: 10px; padding: 4px 12px; }
.form-actions { display: flex; flex-direction: column; gap: 12px; margin-top: 32px; }
.login-btn, .register-btn { width: 100%; margin: 0 !important; }

/* 响应式 */
@media (max-width: 900px) {
  .auth-card { grid-template-columns: 1fr; max-width: 450px; }
  .auth-showcase { display: none; }
}
</style>