<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword"
          clearable
          placeholder="名称或编码"
          @keyup.enter="getList"
        />
      </el-form-item>
      <el-form-item label="启用">
        <el-select v-model="queryParams.enabled" clearable placeholder="全部" style="width: 120px">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="编码" prop="categoryCode" width="140" />
      <el-table-column label="中文名" prop="nameCn" min-width="140" />
      <el-table-column label="英文名" prop="nameEn" min-width="120" show-overflow-tooltip />
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

    <el-dialog :title="dialogTitle" v-model="open" width="520px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="daily / fruit / animal" />
        </el-form-item>
        <el-form-item label="中文名" prop="nameCn">
          <el-input v-model="form.nameCn" />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="form.nameEn" />
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

<script setup lang="ts" name="HlmReadCategory">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listReadCategory,
  saveReadCategory,
  removeReadCategory,
  type ReadAloudCategory
} from '@/api/hlm/readCategory'
import { parseMiniPage } from '@/api/hlm/common'

const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const list = ref<ReadAloudCategory[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  keyword: '',
  enabled: undefined as number | undefined
})

const defaultForm = (): ReadAloudCategory => ({
  categoryCode: '',
  nameCn: '',
  nameEn: '',
  sortOrder: 0,
  enabled: 1
})

const form = ref<ReadAloudCategory>(defaultForm())
const rules: FormRules = {
  categoryCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  nameCn: [{ required: true, message: '请输入中文名', trigger: 'blur' }]
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  listReadCategory({ ...queryParams }).then((res) => {
    const page = parseMiniPage(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function handleAdd() {
  form.value = defaultForm()
  dialogTitle.value = '新增点读分类'
  open.value = true
}

function handleEdit(row: ReadAloudCategory) {
  form.value = { ...defaultForm(), ...row }
  dialogTitle.value = '编辑点读分类'
  open.value = true
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    saveReadCategory(form.value).then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row: ReadAloudCategory) {
  ElMessageBox.confirm(`确认删除分类「${row.nameCn}」？`, '提示', { type: 'warning' }).then(() => {
    removeReadCategory(String(row.id)).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

onMounted(getList)
</script>
