<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="昵称">
        <el-input v-model="queryParams.nickname" clearable placeholder="用户昵称" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="用户" min-width="200">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="36" :src="row.avatar">{{ avatarFallback(row.nickname) }}</el-avatar>
            <div>
              <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
              <div class="sub">OpenID：{{ row.openIdMasked || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.registerTime) }}</template>
      </el-table-column>
      <el-table-column label="最后活跃" width="170">
        <template #default="{ row }">{{ formatDateTime(row.lastActiveTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :total="total"
      v-model:page="queryParams.currentPage"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="用户详情" v-model="detailOpen" width="520px" append-to-body destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="用户 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="OpenID">{{ detail.openIdMasked || '-' }}</el-descriptions-item>
        <el-descriptions-item label="UnionID">{{ detail.unionIdMasked || '-' }}</el-descriptions-item>
        <el-descriptions-item label="小程序 AppID">{{ detail.miniAppId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '正常' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.bio || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(detail.registerTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后活跃">{{ formatDateTime(detail.lastActiveTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HlmMiniUser">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMiniUser, getMiniUser, updateMiniUserStatus, type MiniUser } from '@/api/hlm/miniUser'
import { parseMiniPage, formatDateTime } from '@/api/hlm/common'

const loading = ref(false)
const total = ref(0)
const list = ref<MiniUser[]>([])
const detailOpen = ref(false)
const detail = ref<MiniUser | null>(null)

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  nickname: '',
  status: undefined as number | undefined
})

function avatarFallback(name?: string) {
  return (name && name.charAt(0)) || '用'
}

function getList(pagination?: { page?: number; limit?: number }) {
  if (pagination?.page != null) queryParams.currentPage = pagination.page
  if (pagination?.limit != null) queryParams.pageSize = pagination.limit
  loading.value = true
  const params: Record<string, unknown> = {
    currentPage: queryParams.currentPage,
    pageSize: queryParams.pageSize
  }
  if (queryParams.nickname) params.nickname = queryParams.nickname
  if (queryParams.status != null && queryParams.status !== '') params.status = queryParams.status

  listMiniUser(params).then((res) => {
    const page = parseMiniPage<MiniUser>(res)
    list.value = page.list
    total.value = page.total
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  queryParams.nickname = ''
  queryParams.status = undefined
  queryParams.currentPage = 1
  getList()
}

function handleDetail(row: MiniUser) {
  const id = row.id != null ? String(row.id) : ''
  if (!id) return
  getMiniUser(id).then((res) => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleToggleStatus(row: MiniUser) {
  const id = row.id != null ? String(row.id) : ''
  if (!id) return
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确认${action}用户「${row.nickname || id}」？`, '提示', { type: 'warning' })
    .then(() => updateMiniUserStatus(id, nextStatus))
    .then(() => {
      ElMessage.success(`${action}成功`)
      getList()
    })
    .catch(() => {})
}

onMounted(getList)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nickname {
  font-weight: 500;
}
.sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
</style>
