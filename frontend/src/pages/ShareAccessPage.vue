<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import fileService from '@/services/fileService'

const route = useRoute()
const shareKey = String(route.params.shareKey || '')

const loading = ref(false)
const accessing = ref(false)
const downloading = ref(false)
const shareInfo = ref<any | null>(null)
const accessToken = ref('')
const password = ref('')

const loadShareInfo = async () => {
  if (!shareKey) {
    ElMessage.error('分享链接无效')
    return
  }

  try {
    loading.value = true
    const data = await fileService.getSharedFile(shareKey)
    shareInfo.value = data
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '获取分享信息失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const authorizeShare = async () => {
  try {
    accessing.value = true
    const data = await fileService.accessSharedFile(shareKey, password.value)
    accessToken.value = data.accessToken
    shareInfo.value = data.share
    ElMessage.success('访问授权成功')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '访问授权失败'
    ElMessage.error(message)
  } finally {
    accessing.value = false
  }
}

const downloadFile = async () => {
  if (!accessToken.value) {
    ElMessage.warning('请先点击访问授权')
    return
  }

  try {
    downloading.value = true
    const blob = await fileService.downloadSharedFile(shareKey, accessToken.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url

    const content = shareInfo.value?.sharedContent
    const fileName = content?.name || `shared-${shareKey}`
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载开始')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '下载失败'
    ElMessage.error(message)
  } finally {
    downloading.value = false
  }
}

onMounted(() => {
  loadShareInfo()
})
</script>

<template>
  <div class="share-page" v-loading="loading">
    <el-card class="share-card">
      <template #header>
        <div class="share-title">文件短链分享</div>
      </template>

      <div v-if="shareInfo" class="share-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ shareInfo.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="说明">{{ shareInfo.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分享者">{{ shareInfo.sharerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ shareInfo.shareType }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ shareInfo.status }}</el-descriptions-item>
          <el-descriptions-item label="内容名称">
            {{ shareInfo.sharedContent?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="有效期">
            {{ shareInfo.expireTime || '永久有效' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="auth-box" v-if="shareInfo.requiresPassword">
          <el-input
            v-model="password"
            type="password"
            show-password
            placeholder="请输入访问密码"
            style="max-width: 360px"
          />
        </div>

        <div class="actions">
          <el-button type="primary" :loading="accessing" @click="authorizeShare">访问授权</el-button>
          <el-button
            type="success"
            :disabled="!accessToken || !shareInfo.allowDownload || shareInfo.shareType !== 'FILE'"
            :loading="downloading"
            @click="downloadFile"
          >
            下载文件
          </el-button>
        </div>
      </div>

      <el-empty v-else description="分享信息不可用" />
    </el-card>
  </div>
</template>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #eef6ff 0%, #f7f9fc 100%);
}

.share-card {
  width: 100%;
  max-width: 860px;
  border-radius: 14px;
}

.share-title {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

.share-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-box {
  margin-top: 8px;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
