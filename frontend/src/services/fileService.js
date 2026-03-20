import axios from 'axios';

const API_BASE_URL = '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Let browser/axios set multipart boundary automatically for FormData requests.
    if (config.data instanceof FormData && config.headers) {
      delete config.headers['Content-Type'];
      delete config.headers['content-type'];
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error?.config?.url || '';
    const isPublicShareApi = typeof requestUrl === 'string' && requestUrl.startsWith('/shares/public/');
    if (error.response?.status === 401 && !isPublicShareApi) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 统一从 axios 响应中解包真实业务数据：兼容后端直接返回 Map/DTO 或 使用 ApiResponse 包装的情况
const extractData = (resp) => {
  if (!resp) return resp;
  // axios response object
  const d = resp.data;
  if (d === undefined) return resp;
  // 如果后端使用 ApiResponse { code,msg,data }
  if (d && typeof d === 'object' && Object.prototype.hasOwnProperty.call(d, 'data')) {
    return d.data;
  }
  // 直接返回 body（Map/DTO）
  return d;
};

const fileService = {
  // 上传文件：支持传入 FormData 或 File
  uploadFile: async (fileOrFormData, folderId, onUploadProgress) => {
    let form;
    if (fileOrFormData instanceof FormData) {
      form = fileOrFormData;
    } else {
      form = new FormData();
      form.append('file', fileOrFormData);
      if (folderId !== undefined && folderId !== null) {
        form.append('folderId', String(folderId));
      }
    }

    const response = await apiClient.post('/files/upload', form, {
      onUploadProgress: (progressEvent) => {
        if (onUploadProgress && progressEvent.lengthComputable) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onUploadProgress(percentCompleted);
        }
      },
    });
    return extractData(response);
  },

  // 生产级分片上传方法（并发、重试、断点续传、进度回调）
  chunkUpload: async (file, options = {}) => {
    const {
      chunkSize = 5 * 1024 * 1024,
      concurrency = 3,
      maxRetries = 3,
      onProgress = null,
      onChunkUploaded = null,
    } = options;

    // 1. 初始化上传，获取 uploadId
    const initResp = await apiClient.post(
      '/upload/chunk/init',
      new URLSearchParams({ filename: file.name }),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
    );
    const initBody = extractData(initResp);
    const uploadId = initBody?.uploadId || initBody?.id || initBody;
    if (!uploadId) throw new Error('无法获取 uploadId');

    const totalSize = file.size;
    const chunkCount = Math.ceil(totalSize / chunkSize);

    // 保存进度以支持断点续传（仅客户端层面）
    const resumeKey = `upload_resume_${uploadId}`;
    const saved = JSON.parse(localStorage.getItem(resumeKey) || 'null');
    const uploadedMap = saved && saved.uploadedMap ? saved.uploadedMap : {};

    const chunks = Array.from({ length: chunkCount }, (_, i) => {
      const start = i * chunkSize;
      const end = Math.min(start + chunkSize, totalSize);
      return { index: i, start, end, size: end - start };
    });

    let uploadedBytes = Object.keys(uploadedMap).reduce((acc, k) => acc + (uploadedMap[k] ? (chunks[k] ? chunks[k].size : 0) : 0), 0);
    onProgress && onProgress(Math.round((uploadedBytes * 100) / totalSize));

    // 内部上传单片函数
    const uploadSingle = async (chunk) => {
      if (uploadedMap[chunk.index]) return; // 已上传
      let attempts = 0;
      while (attempts <= maxRetries) {
        try {
          const part = file.slice(chunk.start, chunk.end);
          const form = new FormData();
          form.append('uploadId', uploadId);
          form.append('chunkIndex', chunk.index);
          form.append('chunk', part, file.name);

          await apiClient.post('/upload/chunk', form, {
            onUploadProgress: (e) => {
              // 局部进度不做精确累加，只用已完成块汇总
            },
          });

          uploadedMap[chunk.index] = true;
          uploadedBytes += chunk.size;
          localStorage.setItem(resumeKey, JSON.stringify({ uploadId, uploadedMap, updatedAt: Date.now() }));

          onChunkUploaded && onChunkUploaded(chunk.index);
          onProgress && onProgress(Math.round((uploadedBytes * 100) / totalSize));
          return;
        } catch (err) {
          attempts++;
          if (attempts > maxRetries) throw err;
          // small delay before retry
          await new Promise((r) => setTimeout(r, 500 * attempts));
        }
      }
    };

    // 并发控制
    const queue = chunks.slice();
    const workers = new Array(Math.max(1, concurrency)).fill(0).map(async () => {
      while (queue.length > 0) {
        const chunk = queue.shift();
        if (!chunk) break;
        // 跳过已上传
        if (uploadedMap[chunk.index]) continue;
        await uploadSingle(chunk);
      }
    });

    await Promise.all(workers);

    // 完成合并
    const completeForm = new FormData();
    completeForm.append('uploadId', uploadId);
    completeForm.append('filename', file.name);
    completeForm.append('contentType', file.type || 'application/octet-stream');
    if (options.folderId !== undefined && options.folderId !== null) {
      completeForm.append('folderId', String(options.folderId));
    }

    const completeResp = await apiClient.post('/upload/chunk/complete', completeForm);
    const completeBody = extractData(completeResp);

    // 上传完成后清理本地断点数据
    localStorage.removeItem(resumeKey);

    return completeBody;
  },

  // 获取文件列表
  getFiles: async (params = {}) => {
    const response = await apiClient.get('/files', { params });
    return extractData(response);
  },

  // 获取文件详情
  getFileById: async (fileId) => {
    const response = await apiClient.get(`/files/${fileId}`);
    return extractData(response);
  },

  // 下载文件
  downloadFile: async (fileId) => {
    const response = await apiClient.get(`/files/${fileId}/download`, {
      responseType: 'blob',
    });
    return extractData(response);
  },

  // 预览文件
  previewFile: async (fileId, params = {}) => {
    const response = await apiClient.get(`/preview/${fileId}`, { params });
    return extractData(response);
  },

  // 删除文件
  deleteFile: async (fileId) => {
    const response = await apiClient.delete(`/files/${fileId}`);
    return extractData(response);
  },

  // 批量删除文件
  batchDeleteFiles: async (fileIds) => {
    const response = await apiClient.post('/files/batch-delete', { fileIds });
    return extractData(response);
  },

  // 重命名文件
  renameFile: async (fileId, newName) => {
    const response = await apiClient.put(`/files/${fileId}/rename`, { newName });
    return extractData(response);
  },

  // 移动文件
  moveFile: async (fileId, targetFolderId) => {
    const response = await apiClient.put(`/files/${fileId}/move`, { targetFolderId });
    return extractData(response);
  },

  // 复制文件
  copyFile: async (fileId, targetFolderId) => {
    const response = await apiClient.post(`/files/${fileId}/copy`, { targetFolderId });
    return extractData(response);
  },

  // 获取文件版本
  getFileVersions: async (fileId) => {
    const response = await apiClient.get(`/files/${fileId}/versions`);
    return extractData(response);
  },

  // 恢复文件版本
  restoreFileVersion: async (fileId, versionId) => {
    const response = await apiClient.post(`/files/${fileId}/versions/${versionId}/restore`);
    return extractData(response);
  },

  // 搜索文件
  searchFiles: async (keyword, filters = {}) => {
    const params = { keyword, ...filters };
    const response = await apiClient.get('/files/search', { params });
    return extractData(response);
  },

  // 获取回收站文件
  getRecycleBin: async (page = 0, size = 20) => {
    const response = await apiClient.get('/files/recycle-bin', { params: { page, size } });
    return extractData(response);
  },

  // 恢复回收站文件
  restoreFromRecycleBin: async (itemId) => {
    const response = await apiClient.post(`/files/recycle-bin/${itemId}/restore`, {});
    return extractData(response);
  },

  // 永久删除回收站文件
  permanentDelete: async (itemId) => {
    const response = await apiClient.delete(`/files/recycle-bin/${itemId}`);
    return extractData(response);
  },

  // 创建文件夹
  createFolder: async (folderData) => {
    const response = await apiClient.post('/files/folders', folderData);
    return extractData(response);
  },

  // 获取文件夹内容
  getFolderContents: async (folderId, params = {}) => {
    const response = await apiClient.get(`/files/folders/${folderId}`, { params });
    return extractData(response);
  },

  // 删除文件夹
  deleteFolder: async (folderId) => {
    const response = await apiClient.delete(`/files/folders/${folderId}`);
    return extractData(response);
  },

  // 重命名文件夹
  renameFolder: async (folderId, newName) => {
    const response = await apiClient.put(`/files/folders/${folderId}/rename`, { newName });
    return extractData(response);
  },

  // 获取文件统计信息
  getFileStatistics: async () => {
    const response = await apiClient.get('/files/statistics/files');
    return extractData(response);
  },

  // 获取用户存储使用情况
  getStorageUsage: async () => {
    const response = await apiClient.get('/files/statistics/storage');
    return extractData(response);
  },

  // 获取最近文件
  getRecentFiles: async (limit = 10) => {
    const response = await apiClient.get('/files/recent', { params: { limit } });
    return extractData(response);
  },

  // 获取收藏文件
  getFavoriteFiles: async () => {
    const response = await apiClient.get('/files/favorites');
    return extractData(response);
  },

  // 添加收藏
  addFavorite: async (fileId) => {
    const response = await apiClient.post(`/files/${fileId}/favorite`);
    return extractData(response);
  },

  // 移除收藏
  removeFavorite: async (fileId) => {
    const response = await apiClient.delete(`/files/${fileId}/favorite`);
    return extractData(response);
  },

  // 创建文件短链分享
  getFileShareLink: async (fileId, shareData = {}) => {
    const payload = {
      contentId: fileId,
      shareType: 'FILE',
      title: shareData.title || null,
      description: shareData.description || null,
      password: shareData.password || null,
      expireTime: shareData.expireTime || null,
      maxAccessCount: shareData.maxAccessCount ?? 0,
      allowDownload: shareData.allowDownload !== false,
    };
    const response = await apiClient.post('/shares', payload);
    return extractData(response);
  },

  // 获取我的分享列表
  getMyShares: async (page = 0, size = 20) => {
    const response = await apiClient.get('/shares/mine', { params: { page, size } });
    return extractData(response);
  },

  // 禁用分享
  disableShare: async (shareId) => {
    const response = await apiClient.put(`/shares/${shareId}/disable`);
    return extractData(response);
  },

  // 启用分享
  enableShare: async (shareId) => {
    const response = await apiClient.put(`/shares/${shareId}/enable`);
    return extractData(response);
  },

  // 删除分享
  deleteShare: async (shareId) => {
    const response = await apiClient.delete(`/shares/${shareId}`);
    return extractData(response);
  },

  // 获取分享监控详情
  getShareMonitoring: async (shareId, limit = 20) => {
    const response = await apiClient.get(`/shares/${shareId}/monitoring`, {
      params: { limit }
    });
    return extractData(response);
  },

  // 获取公开分享元信息
  getSharedFile: async (shareKey) => {
    const response = await apiClient.get(`/shares/public/${shareKey}`);
    return extractData(response);
  },

  // 访问公开分享（密码校验 + 获取一次性下载授权）
  accessSharedFile: async (shareKey, password = '') => {
    const response = await apiClient.post(`/shares/public/${shareKey}/access`, { password });
    return extractData(response);
  },

  // 通过一次性授权令牌下载分享文件
  downloadSharedFile: async (shareKey, accessToken) => {
    const response = await apiClient.post(
      `/shares/public/${shareKey}/download`,
      { accessToken },
      { responseType: 'blob' }
    );
    return response.data;
  }
};

export default fileService;