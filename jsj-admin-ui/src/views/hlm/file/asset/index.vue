<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="180" show-overflow-tooltip />
      <el-table-column label="类型" prop="assetType" width="160" />
      <el-table-column label="文件名" prop="originalFilename" min-width="180" show-overflow-tooltip />
      <el-table-column label="扩展名" prop="fileExt" width="80" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button v-if="isAudio(row)" link type="primary" @click="play(row.id)">试听</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :total="total"
      v-model:page="queryParams.currentPage"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts" name="HlmFileAsset">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFileAsset, removeFileAsset, type FileAsset } from '@/api/hlm/fileAsset'
import { parseMiniPage, formatDateTime } from '@/api/hlm/common'
import { miniPreviewUrl } from '@/utils/miniRequest'

const loading = ref(false)
const total = ref(0)
const list = ref<FileAsset[]>([])
let audioEl: HTMLAudioElement | null = null

const queryParams = reactive({ currentPage: 1, pageSize: 10 })

function formatSize(size?: number) {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function isAudio(row: FileAsset) {
  return row.assetType?.includes('AUDIO') || row.mimeType?.startsWith('audio/')
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listFileAsset({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function play(id?: string) {
  if (!id) return
  if (!audioEl) audioEl = new Audio()
  audioEl.src = miniPreviewUrl(id)
  audioEl.play().catch(() => ElMessage.warning('无法播放'))
}

function handleDelete(row: FileAsset) {
  ElMessageBox.confirm(`确认删除文件「${row.originalFilename || row.id}」？`, '提示', { type: 'warning' }).then(() => {
    removeFileAsset(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>
