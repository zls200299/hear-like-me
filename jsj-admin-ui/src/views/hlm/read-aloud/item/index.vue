<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="分类">
        <el-select v-model="queryParams.categoryId" clearable placeholder="全部" style="width: 160px">
          <el-option
            v-for="c in categoryOptions"
            :key="c.id"
            :label="c.nameCn"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input
          v-model="queryParams.keyword"
          clearable
          placeholder="标题或编码"
          @keyup.enter="getList"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下架" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增卡片</el-button>
      </el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="图片" width="72" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.imageAssetId"
            :src="miniPreviewUrl(row.imageAssetId)"
            fit="cover"
            class="thumb"
            :preview-src-list="[miniPreviewUrl(row.imageAssetId)]"
            preview-teleported
          />
          <span v-else class="thumb-empty">-</span>
        </template>
      </el-table-column>
      <el-table-column label="编码" prop="itemCode" width="120" show-overflow-tooltip />
      <el-table-column label="标题" prop="titleCn" min-width="140" />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTag(READ_ITEM_STATUS_DICT, row.status)">
            {{ dictLabel(READ_ITEM_STATUS_DICT, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模拟音频" width="100" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.processedAudioAssetId"
            link
            type="primary"
            @click="playAudio(row.processedAudioAssetId)"
          >试听</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog :title="dialogTitle" v-model="open" width="640px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" filterable placeholder="选择分类" style="width: 100%">
            <el-option
              v-for="c in categoryOptions"
              :key="c.id"
              :label="c.nameCn"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="apple-001" />
        </el-form-item>
        <el-form-item label="中文标题" prop="titleCn">
          <el-input v-model="form.titleCn" placeholder="卡片主文案，如「苹果」" />
        </el-form-item>
        <el-form-item label="英文标题">
          <el-input v-model="form.titleEn" />
        </el-form-item>
        <el-form-item label="中文描述">
          <el-input v-model="form.descriptionCn" type="textarea" :rows="2" placeholder="卡片副标题，选填" />
        </el-form-item>
        <el-form-item label="卡片图片">
          <div class="upload-row">
            <el-upload
              :show-file-list="false"
              :http-request="handleImageUpload"
              :before-upload="beforeImageUpload"
              accept=".jpg,.jpeg,.png,.webp,image/*"
              :disabled="imageUploading"
            >
              <el-button type="primary" plain icon="Upload" :loading="imageUploading">
                {{ form.imageAssetId ? '重新上传' : '上传图片' }}
              </el-button>
            </el-upload>
            <template v-if="form.imageAssetId">
              <el-image
                :src="miniPreviewUrl(form.imageAssetId)"
                fit="cover"
                class="thumb"
                :preview-src-list="[miniPreviewUrl(form.imageAssetId)]"
                preview-teleported
              />
              <el-button link type="danger" @click="clearImage">清除</el-button>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="模拟音频">
          <div class="audio-source">
            <el-radio-group v-model="audioSource" class="mb8">
              <el-radio-button value="library">点读音频库</el-radio-button>
              <el-radio-button value="upload">本地上传</el-radio-button>
            </el-radio-group>

            <template v-if="audioSource === 'library'">
              <el-select
                v-model="form.audioBankId"
                filterable
                clearable
                placeholder="选择已生成完成的点读音频"
                style="width: 100%"
                @change="handleAudioBankSelected"
              >
                <el-option
                  v-for="audio in readyAudioOptions"
                  :key="audio.id"
                  :label="`${audio.title} · ${audio.nChannels} 通道`"
                  :value="audio.id"
                >
                  <div class="audio-option">
                    <span>{{ audio.title }}</span>
                    <small>{{ audio.audioCode }} · {{ audio.nChannels }} 通道</small>
                  </div>
                </el-option>
              </el-select>
              <div v-if="form.processedAudioAssetId" class="upload-row" style="margin-top: 8px">
                <el-button link type="primary" @click="playAudio(form.processedAudioAssetId!)">试听模拟音</el-button>
              </div>
            </template>

            <template v-else>
              <div class="upload-row">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleAudioUpload"
                  :before-upload="beforeAudioUpload"
                  accept=".mp3,.wav,.m4a,.aac,audio/*"
                  :disabled="audioUploading"
                >
                  <el-button type="primary" plain icon="Upload" :loading="audioUploading">
                    {{ form.processedAudioAssetId ? '重新上传' : '上传模拟音频' }}
                  </el-button>
                </el-upload>
                <template v-if="form.processedAudioAssetId">
                  <span class="upload-file">{{ audioFileName || `文件 ID：${form.processedAudioAssetId}` }}</span>
                  <el-button link type="primary" @click="playAudio(form.processedAudioAssetId!)">试听</el-button>
                  <el-button link type="danger" @click="clearAudio">清除</el-button>
                </template>
              </div>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="form.status" style="width: 200px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HlmReadItem">
import { ref, reactive, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listReadItem,
  saveReadItem,
  removeReadItem,
  type ReadAloudItem
} from '@/api/hlm/readItem'
import { listReadCategory, type ReadAloudCategory } from '@/api/hlm/readCategory'
import { listReadAudio, type ReadAloudAudio } from '@/api/hlm/readAudio'
import { parseMiniPage, uploadAudio, uploadImage } from '@/api/hlm/common'
import { miniPreviewUrl } from '@/utils/miniRequest'
import { togglePreviewByAssetId } from '@/utils/hlmAudioPreview'
import { READ_ITEM_STATUS_DICT, dictLabel, dictTag } from '@/utils/hlmDict'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<ReadAloudItem[]>([])
const categoryOptions = ref<ReadAloudCategory[]>([])
const readyAudioOptions = ref<ReadAloudAudio[]>([])
const formRef = ref<FormInstance>()
const imageUploading = ref(false)
const audioUploading = ref(false)
const audioFileName = ref('')
const audioSource = ref<'library' | 'upload'>('library')

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  categoryId: '' as string | number | '',
  keyword: '',
  status: ''
})

const defaultForm = (): ReadAloudItem => ({
  categoryId: '',
  itemCode: '',
  titleCn: '',
  titleEn: '',
  descriptionCn: '',
  imageAssetId: undefined,
  audioBankId: undefined,
  processedAudioAssetId: undefined,
  playMode: 'PROCESSED',
  status: 'DRAFT',
  sortOrder: 0
})

const form = ref<ReadAloudItem>(defaultForm())
const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  itemCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  titleCn: [{ required: true, message: '请输入中文标题', trigger: 'blur' }]
}

function categoryName(id?: string | number) {
  const hit = categoryOptions.value.find((c) => String(c.id) === String(id))
  return hit?.nameCn || '-'
}

function loadCategories() {
  return listReadCategory({ currentPage: 1, pageSize: 500 }).then((res) => {
    categoryOptions.value = parseMiniPage(res).list
  })
}

function loadReadyAudios() {
  return listReadAudio({ currentPage: 1, pageSize: 500, status: 'READY' }).then((res) => {
    readyAudioOptions.value = parseMiniPage(res).list
  })
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  const params: Record<string, unknown> = {
    currentPage: queryParams.currentPage,
    pageSize: queryParams.pageSize,
    keyword: queryParams.keyword || undefined,
    status: queryParams.status || undefined,
    categoryId: queryParams.categoryId || undefined
  }
  listReadItem(params).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function handleAdd() {
  form.value = defaultForm()
  audioFileName.value = ''
  audioSource.value = 'library'
  dialogTitle.value = '新增点读卡片'
  open.value = true
  loadCategories()
  loadReadyAudios()
}

function handleEdit(row: ReadAloudItem) {
  form.value = {
    ...defaultForm(),
    ...row,
    playMode: 'PROCESSED'
  }
  audioFileName.value = ''
  audioSource.value = row.audioBankId ? 'library' : 'upload'
  dialogTitle.value = '编辑点读卡片'
  open.value = true
  loadCategories()
  loadReadyAudios()
}

function handleAudioBankSelected(id?: string) {
  if (!id) {
    form.value.processedAudioAssetId = undefined
    return
  }
  const audio = readyAudioOptions.value.find((item) => String(item.id) === String(id))
  if (!audio?.outputAssetId) {
    ElMessage.warning('该音频尚未生成模拟声')
    form.value.audioBankId = undefined
    form.value.processedAudioAssetId = undefined
    return
  }
  form.value.processedAudioAssetId = audio.outputAssetId
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    if (audioSource.value === 'library') {
      if (!form.value.audioBankId) {
        if (form.value.status === 'PUBLISHED') {
          ElMessage.warning('发布前请从点读音频库选择模拟音频')
          return
        }
      }
    } else {
      form.value.audioBankId = undefined
      if (form.value.status === 'PUBLISHED' && !form.value.processedAudioAssetId) {
        ElMessage.warning('发布前请上传模拟音频')
        return
      }
    }
    if (form.value.status === 'PUBLISHED' && !form.value.processedAudioAssetId) {
      ElMessage.warning('发布前请配置模拟音频')
      return
    }
    const payload: ReadAloudItem = {
      ...form.value,
      playMode: 'PROCESSED',
      audioBankId: audioSource.value === 'library' ? form.value.audioBankId : undefined
    }
    saveReadItem(payload).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function beforeImageUpload(file: File) {
  if (!/\.(jpe?g|png|webp)$/i.test(file.name)) {
    ElMessage.error('仅支持 jpg / png / webp 图片')
    return false
  }
  return true
}

function beforeAudioUpload(file: File) {
  if (!/\.(mp3|wav|m4a|aac)$/i.test(file.name)) {
    ElMessage.error('仅支持 mp3 / wav / m4a / aac 音频')
    return false
  }
  return true
}

async function handleImageUpload(options: any) {
  const raw: File | undefined = options?.file?.raw ?? options?.file
  if (!raw) {
    options?.onError?.(new Error('未获取到文件'))
    return
  }
  imageUploading.value = true
  try {
    const res = await uploadImage(raw)
    form.value.imageAssetId = res.data.assetId
    ElMessage.success('图片上传成功')
    options?.onSuccess?.(res)
  } catch (e) {
    ElMessage.error((e as Error)?.message || '上传失败')
    options?.onError?.(e)
  } finally {
    imageUploading.value = false
  }
}

async function handleAudioUpload(options: any) {
  const raw: File | undefined = options?.file?.raw ?? options?.file
  if (!raw) {
    options?.onError?.(new Error('未获取到文件'))
    return
  }
  audioUploading.value = true
  try {
    const res = await uploadAudio(raw)
    form.value.audioBankId = undefined
    form.value.processedAudioAssetId = res.data.assetId
    audioFileName.value = res.data.fileName
    ElMessage.success('音频上传成功')
    options?.onSuccess?.(res)
  } catch (e) {
    ElMessage.error((e as Error)?.message || '上传失败')
    options?.onError?.(e)
  } finally {
    audioUploading.value = false
  }
}

function clearImage() {
  form.value.imageAssetId = undefined
}

function clearAudio() {
  form.value.audioBankId = undefined
  form.value.processedAudioAssetId = undefined
  audioFileName.value = ''
}

function playAudio(assetId: string | number) {
  togglePreviewByAssetId(assetId, () => ElMessage.warning('无法播放，请确认 jsj-mini 已启动'))
}

function handleDelete(row: ReadAloudItem) {
  ElMessageBox.confirm(`确认删除卡片「${row.titleCn}」？`, '提示', { type: 'warning' }).then(() => {
    removeReadItem(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(() => {
  Promise.all([loadCategories(), loadReadyAudios()]).then(getList)
})

onActivated(() => {
  loadCategories()
  loadReadyAudios()
})
</script>

<style scoped>
.thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
}
.thumb-empty {
  color: #c0c4cc;
}
.audio-source {
  width: 100%;
}
.mb8 {
  margin-bottom: 10px;
}
.audio-option {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.audio-option small {
  color: #909399;
  font-size: 12px;
}
.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  flex-wrap: wrap;
}
.upload-file {
  color: #909399;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
</style>
