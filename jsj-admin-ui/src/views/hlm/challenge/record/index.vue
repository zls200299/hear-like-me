<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="用户">
        <el-input
          v-model="queryParams.userKeyword"
          clearable
          placeholder="昵称或用户 ID"
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="音频/题目">
        <el-input
          v-model="queryParams.audioKeyword"
          clearable
          placeholder="音频名、题目标题或编码"
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="queryParams.isCorrect" clearable placeholder="全部" style="width: 120px">
          <el-option label="正确" :value="1" />
          <el-option label="错误" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="用户" min-width="160">
        <template #default="{ row }">
          <div>{{ row.userNickname || '未设置昵称' }}</div>
          <div class="sub">ID：{{ row.userId || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="180">
        <template #default="{ row }">
          <div>{{ row.questionTitle || '-' }}</div>
          <div class="sub">{{ row.questionCode || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="音频" prop="audioTitle" min-width="140" show-overflow-tooltip />
      <el-table-column label="所选" width="90" align="center">
        <template #default="{ row }">{{ row.selectedChannels }} 通道</template>
      </el-table-column>
      <el-table-column label="正确" width="90" align="center">
        <template #default="{ row }">{{ row.correctChannels }} 通道</template>
      </el-table-column>
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isCorrect === 1 ? 'success' : 'danger'">
            {{ row.isCorrect === 1 ? '正确' : '错误' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="答题时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
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

<script setup lang="ts" name="HlmChallengeRecord">
import { ref, reactive, onMounted } from 'vue'
import { listChallengeAttempt, type ChallengeAttempt } from '@/api/hlm/challengeAttempt'
import { parseMiniPage, formatDateTime } from '@/api/hlm/common'

const loading = ref(false)
const total = ref(0)
const list = ref<ChallengeAttempt[]>([])

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  userKeyword: '',
  audioKeyword: '',
  isCorrect: undefined as number | undefined
})

function handleQuery() {
  queryParams.currentPage = 1
  getList()
}

function resetQuery() {
  queryParams.userKeyword = ''
  queryParams.audioKeyword = ''
  queryParams.isCorrect = undefined
  handleQuery()
}

async function getList() {
  loading.value = true
  try {
    const res = await listChallengeAttempt({ ...queryParams })
    const page = parseMiniPage<ChallengeAttempt>(res)
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.sub {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
