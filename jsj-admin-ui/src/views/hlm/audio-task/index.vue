<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="任务编号">
        <el-input
          v-model="queryParams.taskNo"
          clearable
          placeholder="taskNo"
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.taskStatus" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="queryParams.sourceType" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户 ID">
        <el-input
          v-model="queryParams.userId"
          clearable
          placeholder="可选"
          style="width: 140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="任务编号" prop="taskNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="来源" width="110" align="center">
        <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.taskStatus)">{{ statusLabel(row.taskStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="90" align="center">
        <template #default="{ row }">{{ row.progress ?? 0 }}%</template>
      </el-table-column>
      <el-table-column label="通道" width="80" align="center">
        <template #default="{ row }">{{ channelText(row) }}</template>
      </el-table-column>
      <el-table-column label="场景" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ scenarioText(row) }}</template>
      </el-table-column>
      <el-table-column label="用户" width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ userText(row) }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="100" align="center">
        <template #default="{ row }">{{ durationText(row) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.outputAssetId"
            link
            type="success"
            @click="playAudio(row.outputAssetId)"
          >
            试听输出
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

    <el-drawer v-model="detailOpen" title="任务详情" size="520px" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务编号">{{ detail.taskNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.taskStatus)">{{ statusLabel(detail.taskStatus) }}</el-tag>
            <span class="ml8">进度 {{ detail.progress ?? 0 }}%</span>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceLabel(detail.sourceType) }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ userText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="场景 / 示例">
            {{ scenarioText(detail) }}
          </el-descriptions-item>
          <el-descriptions-item label="通道 / 载体">
            {{ channelText(detail) }} / {{ detail.carrier || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="频段">
            {{ detail.fLo ?? (detail as any).flo ?? '-' }} ~ {{ detail.fHi ?? (detail as any).fhi ?? '-' }} Hz · envCut {{ detail.envCut ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="算法版本">{{ detail.algorithmVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理耗时">{{ durationText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息">
            <span :class="{ 'is-error': !!detail.errorMessage }">{{ detail.errorMessage || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="音频">
            <el-button
              v-if="detail.sourceAssetId"
              link
              type="primary"
              @click="playAudio(detail.sourceAssetId)"
            >
              原声
            </el-button>
            <el-button
              v-if="detail.normalizedAssetId"
              link
              type="primary"
              @click="playAudio(detail.normalizedAssetId)"
            >
              标准化
            </el-button>
            <el-button
              v-if="detail.outputAssetId"
              link
              type="success"
              @click="playAudio(detail.outputAssetId)"
            >
              输出
            </el-button>
            <span v-if="!detail.sourceAssetId && !detail.outputAssetId">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建 / 更新">
            {{ formatDateTime(detail.createTime) }} / {{ formatDateTime(detail.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="events-title">处理事件</div>
        <el-timeline v-if="events.length">
          <el-timeline-item
            v-for="ev in events"
            :key="ev.id"
            :timestamp="formatDateTime(ev.createTime)"
            placement="top"
          >
            <div class="event-main">{{ ev.eventType || '-' }} · {{ ev.stage || 'OTHER' }}</div>
            <div class="event-sub">{{ ev.message || '无描述' }}</div>
            <div v-if="ev.progress != null" class="event-sub">进度 {{ ev.progress }}%</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无事件日志" :image-size="64" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="HlmAudioTask">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAudioTaskDetail,
  listAudioTask,
  removeAudioTask,
  type AudioProcessingTask,
  type AudioProcessingTaskEvent
} from '@/api/hlm/audioTask'
import { formatDateTime, parseMiniPage } from '@/api/hlm/common'
import { stopPreviewAudio, togglePreviewByAssetId } from '@/utils/hlmAudioPreview'

const loading = ref(false)
const total = ref(0)
const list = ref<AudioProcessingTask[]>([])
const detailOpen = ref(false)
const detail = ref<AudioProcessingTask | null>(null)
const events = ref<AudioProcessingTaskEvent[]>([])

const statusOptions = [
  { label: '排队中', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' }
]

const sourceOptions = [
  { label: '示例音', value: 'SAMPLE' },
  { label: '上传', value: 'UPLOAD' },
  { label: '录音', value: 'RECORDING' },
  { label: '点读', value: 'READ_ALOUD' }
]

const queryParams = reactive({
  currentPage: 1,
  pageSize: 10,
  taskNo: '',
  taskStatus: '',
  sourceType: '',
  userId: ''
})

function statusLabel(status?: string) {
  return statusOptions.find((s) => s.value === status)?.label || status || '-'
}

function statusTag(status?: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return ''
}

function sourceLabel(source?: string) {
  return sourceOptions.find((s) => s.value === source)?.label || source || '-'
}

function formatMs(ms?: number) {
  if (ms == null || Number.isNaN(Number(ms))) return '-'
  const value = Number(ms)
  if (value < 1000) return `${value} ms`
  return `${(value / 1000).toFixed(1)} s`
}

function channelText(row: AudioProcessingTask) {
  const value = row.nChannels ?? (row as any).nchannels
  return value != null && value !== '' ? value : '-'
}

function scenarioText(row: AudioProcessingTask) {
  return row.scenarioCode || row.sampleCode || '-'
}

function userText(row: AudioProcessingTask) {
  return row.userId ? String(row.userId) : '管理员'
}

function durationText(row: AudioProcessingTask) {
  if (row.processingMs != null) {
    return formatMs(Number(row.processingMs))
  }
  if (row.processingStartedTime && row.processingFinishedTime) {
    const start = new Date(row.processingStartedTime).getTime()
    const end = new Date(row.processingFinishedTime).getTime()
    if (Number.isFinite(start) && Number.isFinite(end) && end >= start) {
      return formatMs(end - start)
    }
  }
  return '-'
}

function handleQuery() {
  queryParams.currentPage = 1
  getList()
}

function resetQuery() {
  queryParams.taskNo = ''
  queryParams.taskStatus = ''
  queryParams.sourceType = ''
  queryParams.userId = ''
  handleQuery()
}

async function getList() {
  loading.value = true
  try {
    const res = await listAudioTask({ ...queryParams })
    const page = parseMiniPage<AudioProcessingTask>(res)
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function playAudio(assetId?: string | number) {
  const result = togglePreviewByAssetId(assetId, () => {
    ElMessage.error('试听失败')
  })
  if (result === 'noop') {
    ElMessage.warning('暂无可试听音频')
  }
}

async function openDetail(row: AudioProcessingTask) {
  if (!row.id) return
  const res = await getAudioTaskDetail(String(row.id))
  detail.value = res.data?.task || row
  events.value = res.data?.events || []
  detailOpen.value = true
}

function handleDelete(row: AudioProcessingTask) {
  if (!row.id) return
  ElMessageBox.confirm(`确认删除任务 ${row.taskNo || row.id}？仅逻辑删除，不影响已生成文件。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await removeAudioTask(String(row.id))
      ElMessage.success('已删除')
      getList()
    })
    .catch(() => undefined)
}

onMounted(() => {
  getList()
})

onBeforeUnmount(() => {
  stopPreviewAudio()
})
</script>

<style scoped>
.ml8 {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.is-error {
  color: var(--el-color-danger);
  word-break: break-all;
}

.events-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.event-main {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.event-sub {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}
</style>
