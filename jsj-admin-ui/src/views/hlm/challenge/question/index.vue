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
        <el-form-item label="模拟音频" prop="audioBankId">
          <el-select
            v-model="form.audioBankId"
            filterable
            placeholder="从模拟音频库选择已生成的音频"
            style="width: 100%"
            @change="handleAudioSelected"
          >
            <el-option
              v-for="audio in audioOptions"
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
          <div v-if="selectedAudio" class="selected-audio">
            <div class="selected-main">
              <span class="channel-badge">{{ selectedAudio.nChannels }}</span>
              <div>
                <strong>{{ selectedAudio.title }}</strong>
                <small>正确答案已自动锁定为 {{ selectedAudio.nChannels }} 通道</small>
              </div>
            </div>
            <el-button link type="primary" @click="playAudio(selectedAudio.outputAssetId)">试听模拟音</el-button>
          </div>
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
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listChallenge, saveChallenge, removeChallenge, type HearingChallenge } from '@/api/hlm/challenge'
import { listChallengeAudio, type ChallengeAudio } from '@/api/hlm/challengeAudio'
import { parseMiniPage } from '@/api/hlm/common'
import { miniPreviewUrl } from '@/utils/miniRequest'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<HearingChallenge[]>([])
const audioOptions = ref<ChallengeAudio[]>([])
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
  audioBankId: '',
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
  audioBankId: [{ required: true, message: '请选择模拟音频', trigger: 'change' }]
}

const selectedAudio = computed(() => audioOptions.value.find(item => String(item.id) === String(form.value.audioBankId)))

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

function loadAudioOptions() {
  return listChallengeAudio({ currentPage: 1, pageSize: 500, status: 'READY' }).then((res) => {
    audioOptions.value = parseMiniPage(res).list
  })
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增挑战题目'
  open.value = true
  loadAudioOptions()
}

function handleEdit(row: HearingChallenge) {
  form.value = { ...defaultForm(), ...row }
  dialogTitle.value = '编辑挑战题目'
  open.value = true
  loadAudioOptions()
}

function handleAudioSelected(id?: string) {
  const audio = audioOptions.value.find(item => String(item.id) === String(id))
  if (!audio) return
  form.value.audioAssetId = audio.outputAssetId
  form.value.nChannels = audio.nChannels
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

onMounted(() => {
  getList()
  loadAudioOptions()
})
</script>

<style scoped>
.audio-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}
.audio-option small {
  color: #98a4b5;
}
.selected-audio {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 10px;
  padding: 12px 14px;
  border: 1px solid #b7dfca;
  border-radius: 9px;
  background: #f2faf6;
}
.selected-main {
  display: flex;
  align-items: center;
  gap: 11px;
}
.selected-main strong,
.selected-main small {
  display: block;
  line-height: 1.45;
}
.selected-main small {
  color: #668173;
}
.channel-badge {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #d9f3e5;
  color: #238557;
  font-size: 18px;
  font-weight: 700;
}
</style>
