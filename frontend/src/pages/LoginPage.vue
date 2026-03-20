<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const loginFormRef = ref<FormInstance>()

const loginForm = reactive({
  identifier: '',
  password: ''
})

const loginRules = reactive<FormRules>({
  identifier: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度应为 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
})

const handleLogin = async (formEl?: FormInstance) => {
  if (!formEl) return

  try {
    await formEl.validate()
    
    loading.value = true
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    if (error !== false && !error?.hasOwnProperty('identifier') && !error?.hasOwnProperty('password')) {
      const message = error?.response?.data?.message || error?.message || '登录失败'
      ElMessage.error(message)
    }
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="modern-auth-page">
    <div class="auth-card base-card">
      <!-- 左侧：品牌展示与价值主张 -->
      <section class="auth-showcase">
        <div class="showcase-content">
          <span class="kicker-tag">Secure Access</span>
          <h1>文件共享与协作工作区</h1>
          <p class="description">高效的文件流转，安全的存储策略，实时的团队协作。一切，从这里开始。</p>
          
          <div class="highlights">
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><Document /></el-icon></div>
              <div>
                <strong>全能文件管理</strong>
                <p>上传、预览、下载与短链分享一体化</p>
              </div>
            </div>
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><Promotion /></el-icon></div>
              <div>
                <strong>极速快传</strong>
                <p>支持切片上传与秒传，管理发件与取件中心</p>
              </div>
            </div>
            <div class="highlight-item">
              <div class="icon-wrap"><el-icon><Connection /></el-icon></div>
              <div>
                <strong>实时协作</strong>
                <p>项目、文档、评论与在线成员即时同步</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧：登录面板 -->
      <section class="auth-panel">
        <div class="panel-head">
          <h2>账号登录</h2>
          <p>请输入您的凭证以继续使用服务</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          @keyup.enter="handleLogin(loginFormRef)"
          class="modern-form"
        >
          <el-form-item label="账号" prop="identifier">
            <el-input
              v-model="loginForm.identifier"
              size="large"
              placeholder="用户名或邮箱"
              :prefix-icon="User"
              class="modern-input"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              class="modern-input"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" color="#6366f1" size="large" :loading="loading" @click="handleLogin(loginFormRef)" round class="login-btn">
              进入工作台
            </el-button>
            <el-button size="large" @click="goToRegister" round plain class="register-btn">
              注册新账号
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
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* Showcase Styles */
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

/* Panel Styles */
.auth-panel { padding: 48px; background: #fff; }
.panel-head { margin-bottom: 32px; }
.panel-head h2 { font-size: 24px; color: #0f172a; margin: 0 0 8px 0; }
.panel-head p { font-size: 14px; color: #64748b; margin: 0; }

.modern-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}

.form-actions { display: flex; flex-direction: column; gap: 12px; margin-top: 32px; }
.login-btn, .register-btn { width: 100%; margin: 0 !important; }

/* Responsive */
@media (max-width: 900px) {
  .auth-card { grid-template-columns: 1fr; max-width: 450px; }
  .auth-showcase { display: none; }
}
</style>