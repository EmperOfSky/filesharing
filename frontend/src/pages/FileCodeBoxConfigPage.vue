<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import fileCodeBoxService, { type FileCodeBoxAdminConfig } from '@/services/fileCodeBox'

const loading = ref(false)
const saving = ref(false)

const allExpireStyles = ['day', 'hour', 'minute', 'forever', 'count']

const form = reactive({
  openUpload: true,
  uploadSizeMb: 10,
  uploadCount: 10,
  uploadMinute: 1,
  errorCount: 10,
  errorMinute: 1,
  maxSaveSeconds: 0,
  expireStyles: ['day', 'hour', 'minute', 'forever', 'count'] as string[],
  presignExpireSeconds: 900,
  downloadTokenTtlSeconds: 600
})

const applyConfig = (cfg: FileCodeBoxAdminConfig) => {
  form.openUpload = cfg.open_upload
  form.uploadSizeMb = Number((cfg.upload_size / 1024 / 1024).toFixed(2))
  form.uploadCount = cfg.upload_count
  form.uploadMinute = cfg.upload_minute
  form.errorCount = cfg.error_count
  form.errorMinute = cfg.error_minute
  form.maxSaveSeconds = cfg.max_save_seconds
  form.expireStyles = [...cfg.expire_styles]
  form.presignExpireSeconds = cfg.presign_expire_seconds
  form.downloadTokenTtlSeconds = cfg.download_token_ttl_seconds
}

const loadConfig = async () => {
  try {
    loading.value = true
    const cfg = await fileCodeBoxService.getAdminConfig()
    applyConfig(cfg)
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '加载配置失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  if (form.expireStyles.length === 0) {
    ElMessage.warning('请至少选择一种过期策略')
    return
  }

  try {
    saving.value = true
    const payload = {
      open_upload: form.openUpload,
      upload_size: Math.max(1, Math.round(form.uploadSizeMb * 1024 * 1024)),
      upload_count: form.uploadCount,
      upload_minute: form.uploadMinute,
      error_count: form.errorCount,
      error_minute: form.errorMinute,
      max_save_seconds: form.maxSaveSeconds,
      expire_styles: form.expireStyles,
      presign_expire_seconds: form.presignExpireSeconds,
      download_token_ttl_seconds: form.downloadTokenTtlSeconds
    }

    const updated = await fileCodeBoxService.updateAdminConfig(payload)
    applyConfig(updated)
    ElMessage.success('兼容配置已更新')
  } catch (error: any) {
    const message = error?.response?.data?.data || error?.response?.data?.message || error?.message || '保存配置失败'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="fcb-config-page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>FileCodeBox 兼容配置</span>
          <div class="actions">
            <el-button @click="loadConfig" :disabled="loading || saving">刷新</el-button>
            <el-button type="primary" @click="saveConfig" :loading="saving">保存</el-button>
          </div>
        </div>
      </template>

      <el-form label-width="220px" class="config-form">
        <el-form-item label="允许游客上传">
          <el-switch v-model="form.openUpload" />
        </el-form-item>

        <el-form-item label="单文件最大大小 (MB)">
          <el-input-number v-model="form.uploadSizeMb" :min="1" :precision="2" :step="1" />
        </el-form-item>

        <el-form-item label="上传频控次数">
          <el-input-number v-model="form.uploadCount" :min="1" />
        </el-form-item>

        <el-form-item label="上传频控窗口 (分钟)">
          <el-input-number v-model="form.uploadMinute" :min="1" />
        </el-form-item>

        <el-form-item label="取件错误次数">
          <el-input-number v-model="form.errorCount" :min="1" />
        </el-form-item>

        <el-form-item label="取件错误窗口 (分钟)">
          <el-input-number v-model="form.errorMinute" :min="1" />
        </el-form-item>

        <el-form-item label="最长保存秒数 (0 为不限制)">
          <el-input-number v-model="form.maxSaveSeconds" :min="0" />
        </el-form-item>

        <el-form-item label="允许的过期策略">
          <el-checkbox-group v-model="form.expireStyles">
            <el-checkbox v-for="style in allExpireStyles" :key="style" :label="style">{{ style }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="预签名会话有效期 (秒)">
          <el-input-number v-model="form.presignExpireSeconds" :min="60" :step="60" />
        </el-form-item>

        <el-form-item label="下载令牌有效期 (秒)">
          <el-input-number v-model="form.downloadTokenTtlSeconds" :min="60" :step="60" />
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="配置变更说明"
        description="当前页面会立即更新后端运行时配置，便于联调与灰度验证。"
      />
    </el-card>
  </div>
</template>

<style scoped>
.fcb-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
}

.config-form {
  margin-bottom: 16px;
}
</style>
