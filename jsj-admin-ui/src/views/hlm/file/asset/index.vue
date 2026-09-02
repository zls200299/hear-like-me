<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="类型">
        <el-select v-model="queryParams.assetType" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="opt in assetTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="扩展名">
        <el-select v-model="queryParams.fileExt" clearable placeholder="全部" style="width: 120px">
          <el-option v-for="ext in fileExtOptions" :key="ext" :label="ext" :value="ext" />
        </el-select>
      </el-form-item>
      <el-form-item label="文件名">
        <el-input v-model="queryParams.originalFilename" clearable placeholder="文件名关键字" style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="类型" width="160">
        <template #default="{ row }">
          <el-tag :type="dictTag(ASSET_TYPE_DICT, row.assetType)">{{ dictLabel(ASSET_TYPE_DICT, row.assetType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件名" prop="originalFilename" min-width="180" show-overflow-tooltip />
      <el-table-column label="扩展名" prop="fileExt" width="80" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTag(FILE_STATUS_DICT, row.status)">{{ dictLabel(FILE_STATUS_DICT, row.status) }}</el-tag>
        </template>
      </el-table-column>
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
import { ASSET_TYPE_DICT, FILE_STATUS_DICT, dictLabel, dictTag } from '@/utils/hlmDict'

const loading = ref(false)
const total = ref(0)
const list = ref<FileAsset[]>([])
let audioEl: HTMLAudioElement | null = null

const assetTypeOptions = Object.keys(ASSET_TYPE_DICT).map((value) => ({
  value,
  label: ASSET_TYPE_DICT[value].label
}))
const fileExtOptions = ['mp3', 'wav', 'm4a', 'aac', 'jpg', 'jpeg', 'png', 'webp']

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  assetType: '',
  fileExt: '',
  originalFilename: ''
})

function formatSize(size?: number) {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function isAudio(row: FileAsset) {
  return row.assetType?.includes('AUDIO') || row.mimeType?.startsWith('audio/')
}

function resetQuery() {
  queryParams.assetType = ''
  queryParams.fileExt = ''
  queryParams.originalFilename = ''
  queryParams.currentPage = 1
  getList()
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
