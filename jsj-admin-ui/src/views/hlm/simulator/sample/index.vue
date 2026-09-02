<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="编码" prop="sampleCode" width="120" />
      <el-table-column label="中文名" prop="nameCn" min-width="160" />
      <el-table-column label="生成方式" prop="generatorType" width="160" />
      <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="sampleCode">
          <el-input v-model="form.sampleCode" placeholder="vowel / tone / melody" />
        </el-form-item>
        <el-form-item label="中文名" prop="nameCn">
          <el-input v-model="form.nameCn" />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="form.nameEn" />
        </el-form-item>
        <el-form-item label="中文说明">
          <el-input v-model="form.descriptionCn" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="生成方式">
          <el-select v-model="form.generatorType" style="width: 100%">
            <el-option label="Python 运行时生成" value="PYTHON_GENERATED" />
            <el-option label="预生成文件" value="PREGENERATED" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件 ID" v-if="form.generatorType === 'PREGENERATED'">
          <el-input v-model="form.assetId" placeholder="file_asset.id" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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

<script setup lang="ts" name="HlmSample">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listSample, saveSample, removeSample, type SampleAudio } from '@/api/hlm/sample'
import { parseMiniPage } from '@/api/hlm/common'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<SampleAudio[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({ currentPage: 1, pageSize: 10 })

const defaultForm = (): SampleAudio => ({
  sampleCode: '',
  nameCn: '',
  nameEn: '',
  descriptionCn: '',
  generatorType: 'PYTHON_GENERATED',
  sortOrder: 0,
  enabled: 1
})

const form = ref<SampleAudio>(defaultForm())
const rules: FormRules = {
  sampleCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  nameCn: [{ required: true, message: '请输入中文名', trigger: 'blur' }]
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listSample({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增示例音'
  open.value = true
}

function handleEdit(row: SampleAudio) {
  form.value = { ...defaultForm(), ...row }
  dialogTitle.value = '编辑示例音'
  open.value = true
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    saveSample(form.value).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row: SampleAudio) {
  ElMessageBox.confirm(`确认删除示例音「${row.nameCn}」？`, '提示', { type: 'warning' }).then(() => {
    removeSample(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>
