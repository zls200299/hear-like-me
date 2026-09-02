<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="标题">
        <el-input v-model="queryParams.title" clearable placeholder="题目标题" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增题目</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="编码" prop="questionCode" width="140" />
      <el-table-column label="标题" prop="title" min-width="160" />
      <el-table-column label="正确答案" prop="nChannels" width="100" align="center">
        <template #default="{ row }">{{ row.nChannels }} 通道</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="音频" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="row.audioAssetId" link type="primary" @click="playAudio(row.audioAssetId)">试听</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
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

    <el-dialog :title="dialogTitle" v-model="open" width="680px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="题目编码" prop="questionCode">
          <el-input v-model="form.questionCode" placeholder="vowel-8ch-001" />
        </el-form-item>
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="挑战音频" prop="audioAssetId">
          <div class="audio-row">
            <el-input v-model="form.audioAssetId" placeholder="上传后自动填入 assetId" readonly style="flex: 1" />
            <el-upload :show-file-list="false" :http-request="handleUpload" accept="audio/*">
              <el-button type="primary" plain>上传音频</el-button>
            </el-upload>
            <el-button v-if="form.audioAssetId" @click="playAudio(form.audioAssetId)">试听</el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案通道" prop="nChannels">
          <el-radio-group v-model="form.nChannels">
            <el-radio :value="2">2</el-radio>
            <el-radio :value="4">4</el-radio>
            <el-radio :value="8">8</el-radio>
            <el-radio :value="16">16</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="form.status" style="width: 200px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="答对提示">
          <el-input v-model="form.correctTip" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="答错提示">
          <el-input v-model="form.wrongTip" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HlmChallengeQuestion">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { listChallenge, saveChallenge, removeChallenge, type HearingChallenge } from '@/api/hlm/challenge'
import { parseMiniPage, uploadAudio } from '@/api/hlm/common'
import { miniPreviewUrl } from '@/utils/miniRequest'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<HearingChallenge[]>([])
const formRef = ref<FormInstance>()
let audioEl: HTMLAudioElement | null = null

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  title: '',
  status: ''
})

const defaultForm = (): HearingChallenge => ({
  questionCode: '',
  title: '',
  description: '',
  audioAssetId: '',
  nChannels: 8,
  carrier: 'noise',
  fLo: 150,
  fHi: 7000,
  envCut: 160,
  spread: 0.15,
  noiseLevel: 0,
  envAmp: 2.6,
  wetMix: 0.9,
  compressEnabled: 1,
  normalizePeak: 0.89,
  status: 'DRAFT',
  sortOrder: 0,
  correctTip: '',
  wrongTip: ''
})

const form = ref<HearingChallenge>(defaultForm())
const rules: FormRules = {
  questionCode: [{ required: true, message: '请输入题目编码', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  audioAssetId: [{ required: true, message: '请上传挑战音频', trigger: 'change' }],
  nChannels: [{ required: true, message: '请选择正确答案', trigger: 'change' }]
}

function statusLabel(s?: string) {
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' } as Record<string, string>)[s || ''] || s
}
function statusTag(s?: string) {
  return ({ DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'warning' } as Record<string, string>)[s || ''] || 'info'
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listChallenge({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增挑战题目'
  open.value = true
}

function handleEdit(row: HearingChallenge) {
  form.value = { ...defaultForm(), ...row }
  dialogTitle.value = '编辑挑战题目'
  open.value = true
}

function handleUpload(options: UploadRequestOptions) {
  uploadAudio(options.file as File).then((res) => {
    form.value.audioAssetId = res.data.assetId
    ElMessage.success('音频上传成功')
  }).catch(() => options.onError?.(new Error('upload failed') as any))
}

function playAudio(assetId: string | number) {
  if (!audioEl) audioEl = new Audio()
  audioEl.src = miniPreviewUrl(assetId)
  audioEl.play().catch(() => ElMessage.warning('无法播放，请确认 jsj-mini 已启动'))
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    saveChallenge(form.value).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row: HearingChallenge) {
  ElMessageBox.confirm(`确认删除题目「${row.title}」？`, '提示', { type: 'warning' }).then(() => {
    removeChallenge(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>

<style scoped>
.audio-row {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}
</style>
