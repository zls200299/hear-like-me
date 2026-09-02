<template>
  <div class="app-container audio-library">
    <div class="page-head">
      <div>
        <h2>模拟音频库</h2>
        <p>先把原始声音处理成可复用的人工耳蜗模拟音频，再用于挑战题目。</p>
      </div>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增音频</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" clearable placeholder="名称或编码" @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="通道数">
          <el-select v-model="queryParams.nChannels" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="item in channelOptions" :key="item" :label="`${item} 通道`" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="待生成" value="DRAFT" />
            <el-option label="生成中" value="PROCESSING" />
            <el-option label="可使用" value="READY" />
            <el-option label="生成失败" value="FAILED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="list" class="audio-table">
      <el-table-column label="音频素材" min-width="230">
        <template #default="{ row }">
          <div class="audio-name">{{ row.title }}</div>
          <div class="audio-code">{{ row.audioCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="模拟参数" min-width="260">
        <template #default="{ row }">
          <div class="param-summary">
            <el-tag effect="plain">{{ row.nChannels }} 通道</el-tag>
            <span>{{ row.carrier === 'sine' ? '正弦载波' : '噪声载波' }}</span>
            <span>{{ row.fLo }}–{{ row.fHi }} Hz</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="版本" width="80" align="center">
        <template #default="{ row }">v{{ row.versionNo || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="试听" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="playAudio(row.sourceAssetId)">原声</el-button>
          <el-button v-if="row.outputAssetId" link type="success" @click="playAudio(row.outputAssetId)">模拟音</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="225" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" :loading="generatingId === row.id" @click="handleGenerate(row)">
            {{ row.outputAssetId ? '重新生成' : '生成' }}
          </el-button>
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

    <el-drawer v-model="open" :title="dialogTitle" size="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="editor-form">
        <section class="form-section">
          <div class="section-title"><span>1</span>素材信息</div>
          <div class="two-columns">
            <el-form-item label="音频编码" prop="audioCode">
              <el-input v-model="form.audioCode" placeholder="例如 vowel-a-8ch" />
            </el-form-item>
            <el-form-item label="音频名称" prop="title">
              <el-input v-model="form.title" placeholder="方便在题目中选择" />
            </el-form-item>
          </div>
          <el-form-item label="备注说明">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="记录声音内容、使用场景等" />
          </el-form-item>
          <el-form-item label="原始音频" prop="sourceAssetId">
            <div class="source-upload">
              <div class="source-state" :class="{ ready: form.sourceAssetId }">
                <el-icon><Headset /></el-icon>
                <div>
                  <strong>{{ uploadedFileName || (form.sourceAssetId ? '原始音频已上传' : '还没有上传音频') }}</strong>
                  <small>{{ form.sourceAssetId ? `资源 ID：${form.sourceAssetId}` : '支持 mp3、wav、m4a、aac' }}</small>
                </div>
              </div>
              <el-upload :show-file-list="false" :http-request="handleUpload" accept="audio/*">
                <el-button type="primary" plain>{{ form.sourceAssetId ? '重新上传' : '上传音频' }}</el-button>
              </el-upload>
              <el-button v-if="form.sourceAssetId" @click="playAudio(form.sourceAssetId)">试听原声</el-button>
            </div>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-title"><span>2</span>模拟参数</div>
          <el-alert title="题目的正确答案将自动采用这里设置的通道数" type="info" :closable="false" show-icon />
          <el-form-item label="有效通道数" prop="nChannels" class="channel-field">
            <el-radio-group v-model="form.nChannels">
              <el-radio-button v-for="item in channelOptions" :key="item" :value="item">{{ item }} 通道</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <div class="two-columns">
            <el-form-item label="载波类型" prop="carrier">
              <el-select v-model="form.carrier" style="width: 100%">
                <el-option label="噪声载波" value="noise" />
                <el-option label="正弦载波" value="sine" />
              </el-select>
            </el-form-item>
            <el-form-item label="包络截止频率" prop="envCut">
              <el-input-number v-model="form.envCut" :min="20" :max="500" :step="10" controls-position="right" />
            </el-form-item>
            <el-form-item label="频率下限（Hz）" prop="fLo">
              <el-input-number v-model="form.fLo" :min="20" :max="10000" :step="10" controls-position="right" />
            </el-form-item>
            <el-form-item label="频率上限（Hz）" prop="fHi">
              <el-input-number v-model="form.fHi" :min="100" :max="12000" :step="100" controls-position="right" />
            </el-form-item>
          </div>
          <el-form-item label="电流扩散">
            <div class="slider-row">
              <el-slider v-model="form.spread" :min="0" :max="1" :step="0.05" />
              <span>{{ Number(form.spread || 0).toFixed(2) }}</span>
            </div>
          </el-form-item>
          <el-form-item label="背景噪声">
            <div class="slider-row">
              <el-slider v-model="form.noiseLevel" :min="0" :max="1" :step="0.05" />
              <span>{{ Number(form.noiseLevel || 0).toFixed(2) }}</span>
            </div>
          </el-form-item>
        </section>

        <section v-if="form.outputAssetId" class="result-card">
          <div>
            <strong>模拟音频已生成</strong>
            <span>版本 v{{ form.versionNo }} · {{ form.nChannels }} 通道</span>
          </div>
          <el-button type="success" plain @click="playAudio(form.outputAssetId)">试听模拟音</el-button>
        </section>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="open = false">取消</el-button>
          <el-button @click="submitForm(false)">保存草稿</el-button>
          <el-button type="primary" :loading="savingAndGenerating" @click="submitForm(true)">保存并生成</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="HlmChallengeAudio">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Headset } from '@element-plus/icons-vue'
import { parseMiniPage, uploadAudio } from '@/api/hlm/common'
import {
  generateChallengeAudio,
  listChallengeAudio,
  removeChallengeAudio,
  saveChallengeAudio,
  type ChallengeAudio
} from '@/api/hlm/challengeAudio'
import { miniPreviewUrl } from '@/utils/miniRequest'

const channelOptions = [2, 4, 8, 16]
const loading = ref(false)
const open = ref(false)
const total = ref(0)
const list = ref<ChallengeAudio[]>([])
const formRef = ref<FormInstance>()
const dialogTitle = ref('新增模拟音频')
const uploadedFileName = ref('')
const generatingId = ref<string>()
const savingAndGenerating = ref(false)
let audioEl: HTMLAudioElement | null = null

const queryParams = reactive({ currentPage: 1, pageSize: 10, keyword: '', status: '', nChannels: undefined as number | undefined })
const defaultForm = (): ChallengeAudio => ({
  audioCode: '', title: '', description: '', sourceAssetId: '', outputAssetId: '',
  nChannels: 8, carrier: 'noise', fLo: 150, fHi: 7000, envCut: 160, spread: 0.15,
  noiseLevel: 0, status: 'DRAFT', versionNo: 0
})
const form = ref<ChallengeAudio>(defaultForm())
const rules: FormRules = {
  audioCode: [{ required: true, message: '请输入音频编码', trigger: 'blur' }],
  title: [{ required: true, message: '请输入音频名称', trigger: 'blur' }],
  sourceAssetId: [{ required: true, message: '请上传原始音频', trigger: 'change' }],
  nChannels: [{ required: true, message: '请选择通道数', trigger: 'change' }],
  carrier: [{ required: true, message: '请选择载波类型', trigger: 'change' }]
}

function statusLabel(status?: string) {
  return ({ DRAFT: '待生成', PROCESSING: '生成中', READY: '可使用', FAILED: '生成失败', DISABLED: '已停用' } as Record<string, string>)[status || ''] || status || '-'
}
function statusTag(status?: string) {
  return ({ DRAFT: 'info', PROCESSING: 'warning', READY: 'success', FAILED: 'danger', DISABLED: 'info' } as Record<string, any>)[status || ''] || 'info'
}
function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listChallengeAudio({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}
function resetQuery() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.nChannels = undefined
  queryParams.currentPage = 1
  getList()
}
function handleAdd() {
  form.value = defaultForm()
  uploadedFileName.value = ''
  dialogTitle.value = '新增模拟音频'
  open.value = true
}
function handleEdit(row: ChallengeAudio) {
  form.value = { ...defaultForm(), ...row }
  uploadedFileName.value = ''
  dialogTitle.value = '编辑模拟音频'
  open.value = true
}
function handleUpload(options: UploadRequestOptions) {
  uploadAudio(options.file as File).then((res) => {
    form.value.sourceAssetId = res.data.assetId
    uploadedFileName.value = res.data.fileName || options.file.name
    ElMessage.success('原始音频上传成功')
    formRef.value?.validateField('sourceAssetId')
  }).catch(() => options.onError?.(new Error('upload failed') as any))
}
function submitForm(generateAfterSave: boolean) {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    savingAndGenerating.value = generateAfterSave
    try {
      const res = await saveChallengeAudio(form.value) as { data: ChallengeAudio }
      const saved = res.data
      form.value = { ...form.value, ...saved }
      if (generateAfterSave && saved.id) {
        const generated = await generateChallengeAudio(String(saved.id))
        form.value = { ...form.value, ...generated.data }
        ElMessage.success('模拟音频已生成，可以在题目中使用')
      } else {
        ElMessage.success('素材已保存')
      }
      open.value = false
      getList()
    } finally {
      savingAndGenerating.value = false
    }
  })
}
function handleGenerate(row: ChallengeAudio) {
  if (!row.id) return
  const action = row.outputAssetId ? '重新生成后，新的题目将使用新版本；已发布题目仍保留原音频。是否继续？' : '确认按当前参数生成模拟音频？'
  ElMessageBox.confirm(action, '生成模拟音频', { type: 'warning' }).then(async () => {
    generatingId.value = row.id
    try {
      await generateChallengeAudio(String(row.id))
      ElMessage.success('模拟音频生成成功')
      getList()
    } finally {
      generatingId.value = undefined
    }
  }).catch(() => {})
}
function handleDelete(row: ChallengeAudio) {
  ElMessageBox.confirm(`确认删除音频「${row.title}」？`, '提示', { type: 'warning' }).then(async () => {
    await removeChallengeAudio(String(row.id))
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}
function playAudio(assetId?: string) {
  if (!assetId) return
  if (!audioEl) audioEl = new Audio()
  audioEl.src = miniPreviewUrl(assetId)
  audioEl.play().catch(() => ElMessage.warning('音频无法播放，请确认业务服务已启动'))
}
onBeforeUnmount(() => { audioEl?.pause() })
onMounted(getList)
</script>

<style scoped>
.audio-library { background: #f5f7fa; min-height: calc(100vh - 84px); }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.page-head h2 { margin: 0 0 6px; font-size: 24px; color: #1f2937; }
.page-head p { margin: 0; color: #7b8798; }
.filter-card { margin-bottom: 16px; border: 0; }
.filter-card :deep(.el-card__body) { padding-bottom: 2px; }
.audio-table { border-radius: 8px; overflow: hidden; }
.audio-name { font-weight: 600; color: #263445; }
.audio-code { margin-top: 5px; color: #97a3b4; font-size: 12px; font-family: Consolas, monospace; }
.param-summary { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 14px; color: #607086; }
.editor-form { padding: 0 8px 20px; }
.form-section { margin-bottom: 20px; padding: 18px; border: 1px solid #e5eaf1; border-radius: 12px; background: #fff; }
.section-title { display: flex; align-items: center; gap: 9px; margin-bottom: 18px; font-weight: 700; color: #263445; }
.section-title span { display: grid; place-items: center; width: 25px; height: 25px; border-radius: 50%; background: #e9f3ff; color: #409eff; font-size: 13px; }
.two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.source-upload { display: flex; align-items: center; gap: 10px; width: 100%; }
.source-state { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px dashed #c9d2df; border-radius: 9px; color: #8a96a8; }
.source-state.ready { border-style: solid; border-color: #a8d7bd; background: #f1faf5; color: #3f8f65; }
.source-state .el-icon { font-size: 25px; }
.source-state strong, .source-state small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.source-state small { margin-top: 4px; color: #98a4b5; }
.channel-field { margin-top: 18px; }
.slider-row { display: flex; align-items: center; gap: 18px; width: 100%; }
.slider-row .el-slider { flex: 1; }
.slider-row span { width: 42px; text-align: right; color: #637286; font-variant-numeric: tabular-nums; }
.result-card { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; border: 1px solid #a8d7bd; border-radius: 12px; background: #f1faf5; }
.result-card strong, .result-card span { display: block; }
.result-card span { margin-top: 5px; color: #6d8076; font-size: 13px; }
.drawer-footer { display: flex; justify-content: flex-end; }
:deep(.el-input-number) { width: 100%; }
@media (max-width: 900px) { .two-columns { grid-template-columns: 1fr; } }
</style>
