<template>
  <div class="app-container">
    <el-form :inline="true" v-show="showSearch">
      <el-form-item label="场景编码">
        <el-input v-model="queryParams.scenarioCode" placeholder="quiet / restaurant" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="中文名">
        <el-input v-model="queryParams.nameCn" placeholder="场景名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="编码" prop="scenarioCode" width="110" />
      <el-table-column label="中文名" prop="nameCn" min-width="110" />
      <el-table-column label="通道数" width="80" align="center">
        <template #default="{ row }">{{ row.nChannels ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="频率范围" min-width="140">
        <template #default="{ row }">
          {{ row.fLo != null && row.fHi != null ? `${row.fLo} - ${row.fHi} Hz` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="默认示例音" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.defaultSampleCode" :type="dictTag(SAMPLE_CODE_DICT, row.defaultSampleCode)">
            {{ dictLabel(SAMPLE_CODE_DICT, row.defaultSampleCode) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
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

    <el-dialog :title="dialogTitle" v-model="open" width="860px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="scenario-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="场景编码" prop="scenarioCode">
              <el-input v-model="form.scenarioCode" placeholder="quiet" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中文名" prop="nameCn">
              <el-input v-model="form.nameCn" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文名">
              <el-input v-model="form.nameEn" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标 key">
              <el-input v-model="form.icon" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="中文说明">
              <el-input v-model="form.descriptionCn" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通道数" prop="nChannels">
              <el-input-number v-model="form.nChannels" :min="1" :max="22" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认示例音">
              <el-select v-model="form.defaultSampleCode" clearable style="width: 100%">
                <el-option v-for="opt in sampleCodeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="频率下限">
              <el-input-number v-model="form.fLo" :min="1" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="频率上限">
              <el-input-number v-model="form.fHi" :min="100" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="包络截止">
              <el-input-number v-model="form.envCut" :min="20" :max="500" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电流扩散">
              <el-input-number v-model="form.spread" :min="0" :max="1" :step="0.05" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="背景噪声">
              <el-input-number v-model="form.noiseLevel" :min="0" :max="1" :step="0.05" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HlmScenario">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listScenario, saveScenario, removeScenario, type ScenarioPreset } from '@/api/hlm/scenario'
import { parseMiniPage } from '@/api/hlm/common'
import { SAMPLE_CODE_DICT, dictLabel, dictTag } from '@/utils/hlmDict'

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<ScenarioPreset[]>([])
const formRef = ref<FormInstance>()

const sampleCodeOptions = Object.keys(SAMPLE_CODE_DICT).map((value) => ({
  value,
  label: SAMPLE_CODE_DICT[value].label
}))

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  scenarioCode: '',
  nameCn: ''
})

const defaultForm = (): ScenarioPreset => ({
  scenarioCode: '',
  nameCn: '',
  nameEn: '',
  descriptionCn: '',
  icon: '',
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
  defaultSampleCode: 'vowel',
  sortOrder: 0,
  enabled: 1
})

const form = ref<ScenarioPreset>(defaultForm())

const rules: FormRules = {
  scenarioCode: [{ required: true, message: '请输入场景编码', trigger: 'blur' }],
  nameCn: [{ required: true, message: '请输入中文名', trigger: 'blur' }],
  nChannels: [{ required: true, message: '请输入通道数', trigger: 'blur' }]
}

/** 兼容 Jackson 对 fLo/nChannels 的错误序列化名（flo / NChannels 等） */
function pickField(row: Record<string, unknown>, ...keys: string[]) {
  for (const key of keys) {
    const val = row[key]
    if (val != null && val !== '') return val
  }
  return undefined
}

function toNumber(val: unknown): number | undefined {
  if (val == null || val === '') return undefined
  const num = Number(val)
  return Number.isNaN(num) ? undefined : num
}

function normalizeScenario(row: ScenarioPreset): ScenarioPreset {
  const raw = row as ScenarioPreset & Record<string, unknown>
  return {
    ...row,
    nChannels: toNumber(pickField(raw, 'nChannels', 'NChannels', 'nchannels')) ?? row.nChannels,
    fLo: toNumber(pickField(raw, 'fLo', 'FLo', 'flo')) ?? row.fLo,
    fHi: toNumber(pickField(raw, 'fHi', 'FHi', 'fhi')) ?? row.fHi,
    envCut: toNumber(pickField(raw, 'envCut', 'EnvCut')) ?? row.envCut,
    spread: toNumber(row.spread) ?? row.spread,
    noiseLevel: toNumber(row.noiseLevel) ?? row.noiseLevel,
    sortOrder: toNumber(row.sortOrder) ?? row.sortOrder,
    enabled: toNumber(row.enabled) ?? row.enabled
  }
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listScenario({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list.map((item) => normalizeScenario(item))
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  queryParams.scenarioCode = ''
  queryParams.nameCn = ''
  queryParams.currentPage = 1
  getList()
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增场景预设'
  open.value = true
}

function handleEdit(row: ScenarioPreset) {
  form.value = { ...defaultForm(), ...normalizeScenario(row) }
  dialogTitle.value = '编辑场景预设'
  open.value = true
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    saveScenario(form.value).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row: ScenarioPreset) {
  ElMessageBox.confirm(`确认删除场景「${row.nameCn}」？`, '提示', { type: 'warning' }).then(() => {
    removeScenario(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>
