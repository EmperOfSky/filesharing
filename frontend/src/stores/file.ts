import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import fileService from '@/services/file'
import { FileItem, Folder } from '@/types'

export const useFileStore = defineStore('file', () => {
  const files = ref<FileItem[]>([])
  const folders = ref<Folder[]>([])
  const currentFolderId = ref<number | null>(null)
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0
  })

  const currentFiles = computed(() => {
    if (currentFolderId.value === null) {
      return files.value.filter(file => !file.folderId)
    }
    return files.value.filter(file => file.folderId === currentFolderId.value)
  })

  const currentFolders = computed(() => {
    if (currentFolderId.value === null) {
      return folders.value.filter(folder => !folder.parentId)
    }
    return folders.value.filter(folder => folder.parentId === currentFolderId.value)
  })

  const loadFiles = async (page = 0, size = 10) => {
    try {
      const response = await fileService.getFiles(page, size, currentFolderId.value || undefined)
      files.value = response.content
      pagination.value = {
        page: response.number,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      }
      return response
    } catch (error) {
      throw error
    }
  }

  const loadFolders = async () => {
    try {
      const folderList = await fileService.getFolders()
      folders.value = folderList
      return folderList
    } catch (error) {
      throw error
    }
  }

  const uploadFile = async (file: File) => {
    try {
      const uploadedFile = await fileService.uploadFile(file, currentFolderId.value || undefined)
      files.value.push(uploadedFile)
      return uploadedFile
    } catch (error) {
      throw error
    }
  }

  const deleteFile = async (id: number) => {
    try {
      await fileService.deleteFile(id)
      files.value = files.value.filter(file => file.id !== id)
    } catch (error) {
      throw error
    }
  }

  const createFolder = async (name: string) => {
    try {
      const newFolder = await fileService.createFolder(name, currentFolderId.value || undefined)
      folders.value.push(newFolder)
      return newFolder
    } catch (error) {
      throw error
    }
  }

  const deleteFolder = async (id: number) => {
    try {
      await fileService.deleteFolder(id)
      folders.value = folders.value.filter(folder => folder.id !== id)
      // 同时删除该文件夹下的文件
      files.value = files.value.filter(file => file.folderId !== id)
    } catch (error) {
      throw error
    }
  }

  const navigateToFolder = (folderId: number | null) => {
    currentFolderId.value = folderId
  }

  const searchFiles = async (keyword: string, fileType?: string) => {
    try {
      const response = await fileService.searchFiles({
        keyword,
        fileType,
        page: 0,
        size: 20
      })
      return response
    } catch (error) {
      throw error
    }
  }

  return {
    files,
    folders,
    currentFolderId,
    pagination,
    currentFiles,
    currentFolders,
    loadFiles,
    loadFolders,
    uploadFile,
    deleteFile,
    createFolder,
    deleteFolder,
    navigateToFolder,
    searchFiles
  }
})