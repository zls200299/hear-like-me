<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="配置键" prop="configKey" min-width="200" show-overflow-tooltip />
      <el-table-column label="配置值" prop="configValue" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTag(VALUE_TYPE_DICT, row.valueType)">{{ dictLabel(VALUE_TYPE_DICT, row.valueType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="description" min-width="200" show-overflow-tooltip />
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '是' : '否' }}</el-tag>
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

    <el-dialog :title="dialogTitle" v-model="open" width="560px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" :disabled="!!form.id" placeholder="app.name" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="form.valueType" style="width: 100%">
            <el-option label="STRING" value="STRING" />
            <el-option label="INT" value="INT" />
            <el-option label="DECIMAL" value="DECIMAL" />
            <el-option label="BOOL" value="BOOL" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HlmBizConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listBizConfig, saveBizConfig, removeBizConfig, type BizConfig } from '@/api/hlm/bizConfig'
import { parseMiniPage } from '@/api/hlm/common'
import { VALUE_TYPE_DICT, dictLabel, dictTag } from '@/utils/hlmDict'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<BizConfig[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({ currentPage: 1, pageSize: 20 })

const defaultForm = (): BizConfig => ({
  configKey: '',
  configValue: '',
  valueType: 'STRING',
  description: '',
  enabled: 1
})

const form = ref<BizConfig>(defaultForm())
const rules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listBizConfig({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增业务配置'
  open.value = true
}

function handleEdit(row: BizConfig) {
  form.value = { ...defaultForm(), ...row }
  dialogTitle.value = '编辑业务配置'
  open.value = true
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    saveBizConfig(form.value).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row: BizConfig) {
  ElMessageBox.confirm(`确认删除配置「${row.configKey}」？`, '提示', { type: 'warning' }).then(() => {
    removeBizConfig(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>
