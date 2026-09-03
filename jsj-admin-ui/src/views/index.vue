<template>
  <div class="app-container home">
    <div class="home-head">
      <div>
        <h2>Hear Like Me 工作台</h2>
        <p>集中查看小程序内容、挑战题目、模拟音频和用户状态。</p>
      </div>
      <el-button :loading="loading" icon="Refresh" @click="loadDashboard">刷新</el-button>
    </div>

    <el-row :gutter="16" class="metric-row">
      <el-col v-for="item in metrics" :key="item.label" :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <div class="metric-icon" :class="item.tone">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.extra }}</small>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <span>发布检查</span>
              <el-tag :type="todoCount > 0 ? 'warning' : 'success'" effect="plain">
                {{ todoCount > 0 ? `${todoCount} 项待处理` : '状态良好' }}
              </el-tag>
            </div>
          </template>
          <div class="check-list">
            <div v-for="item in checks" :key="item.title" class="check-item">
              <el-icon :class="item.ok ? 'ok' : 'warn'">
                <CircleCheck v-if="item.ok" />
                <Warning v-else />
              </el-icon>
              <div>
                <strong>{{ item.title }}</strong>
                <span>{{ item.desc }}</span>
              </div>
              <el-button link type="primary" @click="go(item.path)">查看</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <span>快捷入口</span>
            </div>
          </template>
          <div class="quick-grid">
            <button v-for="item in quickLinks" :key="item.label" type="button" @click="go(item.path)">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <span>最近挑战题目</span>
              <el-button link type="primary" @click="go('/hlm/challenge/question')">全部题目</el-button>
            </div>
          </template>
          <el-table v-loading="loading" :data="recentChallenges" size="small" empty-text="暂无题目">
            <el-table-column label="题目" prop="title" min-width="160" show-overflow-tooltip />
            <el-table-column label="答案" width="82" align="center">
              <template #default="{ row }">{{ row.nChannels || '-' }} 通道</template>
            </el-table-column>
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }">
                <el-tag :type="challengeStatusTag(row.status)" size="small">{{ challengeStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <span>最近模拟音频</span>
              <el-button link type="primary" @click="go('/hlm/challenge/audio')">音频库</el-button>
            </div>
          </template>
          <el-table v-loading="loading" :data="recentAudios" size="small" empty-text="暂无音频">
            <el-table-column label="音频" prop="title" min-width="150" show-overflow-tooltip />
            <el-table-column label="通道" width="70" align="center">
              <template #default="{ row }">{{ row.nChannels || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="audioStatusTag(row.status)" size="small">{{ audioStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Index">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Avatar,
  CircleCheck,
  DataLine,
  Files,
  Headset,
  Microphone,
  Reading,
  Warning
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listChallenge, type HearingChallenge } from '@/api/hlm/challenge'
import { listChallengeAudio, type ChallengeAudio } from '@/api/hlm/challengeAudio'
import { listFileAsset } from '@/api/hlm/fileAsset'
import { listMiniUser } from '@/api/hlm/miniUser'
import { parseMiniPage } from '@/api/hlm/common'

const router = useRouter()
const loading = ref(false)
const totalChallenges = ref(0)
const publishedChallenges = ref(0)
const totalAudios = ref(0)
const readyAudios = ref(0)
const failedAudios = ref(0)
const totalAssets = ref(0)
const audioAssets = ref(0)
const totalUsers = ref(0)
const enabledUsers = ref(0)
const recentChallenges = ref<HearingChallenge[]>([])
const recentAudios = ref<ChallengeAudio[]>([])

const metrics = computed(() => [
  {
    label: '挑战题目',
    value: totalChallenges.value,
    extra: `${publishedChallenges.value} 个已发布`,
    icon: DataLine,
    tone: 'blue'
  },
  {
    label: '模拟音频',
    value: totalAudios.value,
    extra: `${readyAudios.value} 个可使用，${failedAudios.value} 个失败`,
    icon: Headset,
    tone: failedAudios.value > 0 ? 'orange' : 'green'
  },
  {
    label: '素材文件',
    value: totalAssets.value,
    extra: `${audioAssets.value} 个音频素材`,
    icon: Files,
    tone: 'purple'
  },
  {
    label: '小程序用户',
    value: totalUsers.value,
    extra: `${enabledUsers.value} 个正常账号`,
    icon: Avatar,
    tone: 'cyan'
  }
])

const checks = computed(() => [
  {
    title: '挑战题目发布',
    desc: publishedChallenges.value > 0 ? `已有 ${publishedChallenges.value} 道题面向小程序用户` : '还没有已发布题目，客户体验挑战页会偏空',
    ok: publishedChallenges.value > 0,
    path: '/hlm/challenge/question'
  },
  {
    title: '模拟音频可用性',
    desc: readyAudios.value > 0 ? `${readyAudios.value} 个模拟音频已生成，可用于题目` : '音频库里还没有可使用的模拟音频',
    ok: readyAudios.value > 0,
    path: '/hlm/challenge/audio'
  },
  {
    title: '生成失败处理',
    desc: failedAudios.value > 0 ? `${failedAudios.value} 个音频生成失败，需要检查参数或原始文件` : '暂无生成失败的音频',
    ok: failedAudios.value === 0,
    path: '/hlm/challenge/audio'
  },
  {
    title: '素材上传',
    desc: totalAssets.value > 0 ? `素材库共有 ${totalAssets.value} 个文件` : '素材库还没有文件，试听和挑战内容会受影响',
    ok: totalAssets.value > 0,
    path: '/hlm/file/asset'
  }
])

const todoCount = computed(() => checks.value.filter(item => !item.ok).length)

const quickLinks = [
  { label: '题库管理', icon: Headset, path: '/hlm/challenge/audio' },
  { label: '挑战题目', icon: Microphone, path: '/hlm/challenge/question' },
  { label: '场景预设', icon: Reading, path: '/hlm/simulator/scenario' },
  { label: '素材文件', icon: Files, path: '/hlm/file/asset' }
]

async function loadDashboard() {
  loading.value = true
  try {
    const [
      challengeRes,
      publishedChallengeRes,
      audioRes,
      readyAudioRes,
      failedAudioRes,
      assetRes,
      audioAssetRes,
      userRes,
      enabledUserRes
    ] = await Promise.all([
      listChallenge({ currentPage: 1, pageSize: 5 }),
      listChallenge({ currentPage: 1, pageSize: 1, status: 'PUBLISHED' }),
      listChallengeAudio({ currentPage: 1, pageSize: 5 }),
      listChallengeAudio({ currentPage: 1, pageSize: 1, status: 'READY' }),
      listChallengeAudio({ currentPage: 1, pageSize: 1, status: 'FAILED' }),
      listFileAsset({ currentPage: 1, pageSize: 1 }),
      listFileAsset({ currentPage: 1, pageSize: 1, assetType: 'AUDIO' }),
      listMiniUser({ currentPage: 1, pageSize: 1 }),
      listMiniUser({ currentPage: 1, pageSize: 1, status: 1 })
    ])

    const challengePage = parseMiniPage(challengeRes)
    const audioPage = parseMiniPage(audioRes)
    totalChallenges.value = challengePage.total
    publishedChallenges.value = parseMiniPage(publishedChallengeRes).total
    recentChallenges.value = challengePage.list
    totalAudios.value = audioPage.total
    readyAudios.value = parseMiniPage(readyAudioRes).total
    failedAudios.value = parseMiniPage(failedAudioRes).total
    recentAudios.value = audioPage.list
    totalAssets.value = parseMiniPage(assetRes).total
    audioAssets.value = parseMiniPage(audioAssetRes).total
    totalUsers.value = parseMiniPage(userRes).total
    enabledUsers.value = parseMiniPage(enabledUserRes).total
  } catch (error) {
    ElMessage.error('工作台数据加载失败，请确认后台和小程序业务服务都已启动')
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  router.push(path)
}

function challengeStatusLabel(status?: string) {
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' } as Record<string, string>)[status || ''] || '-'
}

function challengeStatusTag(status?: string) {
  return ({ DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'warning' } as Record<string, any>)[status || ''] || 'info'
}

function audioStatusLabel(status?: string) {
  return ({ DRAFT: '待生成', PROCESSING: '生成中', READY: '可使用', FAILED: '失败', DISABLED: '停用' } as Record<string, string>)[status || ''] || '-'
}

function audioStatusTag(status?: string) {
  return ({ DRAFT: 'info', PROCESSING: 'warning', READY: 'success', FAILED: 'danger', DISABLED: 'info' } as Record<string, any>)[status || ''] || 'info'
}

onMounted(loadDashboard)
</script>

<style scoped lang="scss">
.home {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}

.home-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  h2 {
    margin: 0 0 6px;
    color: #1f2937;
    font-size: 24px;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 118px;
  margin-bottom: 16px;
  padding: 20px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #fff;

  span,
  small {
    display: block;
  }

  span {
    color: #667085;
    font-size: 13px;
  }

  strong {
    display: block;
    margin: 6px 0 4px;
    color: #111827;
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
  }

  small {
    color: #98a2b3;
    font-size: 12px;
  }
}

.metric-icon {
  display: grid;
  flex: 0 0 48px;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  font-size: 24px;

  &.blue {
    background: #eaf3ff;
    color: #2f7ed8;
  }

  &.green {
    background: #eaf8f0;
    color: #21a366;
  }

  &.orange {
    background: #fff3e6;
    color: #d97706;
  }

  &.purple {
    background: #f1edff;
    color: #7352d6;
  }

  &.cyan {
    background: #e7f8fb;
    color: #0891b2;
  }
}

.panel-card {
  margin-bottom: 16px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;

  :deep(.el-card__header) {
    padding: 16px 18px;
  }

  :deep(.el-card__body) {
    padding: 0;
  }
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2937;
  font-weight: 650;
}

.check-list {
  padding: 4px 0;
}

.check-item {
  display: grid;
  grid-template-columns: 32px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 16px 18px;
  border-bottom: 1px solid #edf1f6;

  &:last-child {
    border-bottom: 0;
  }

  .el-icon {
    font-size: 22px;
  }

  .ok {
    color: #21a366;
  }

  .warn {
    color: #d97706;
  }

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 4px;
    color: #1f2937;
    font-weight: 650;
  }

  span {
    color: #6b7280;
    font-size: 13px;
    line-height: 1.5;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 18px;

  button {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 68px;
    padding: 0 16px;
    border: 1px solid #e5eaf1;
    border-radius: 8px;
    background: #fbfcfe;
    color: #344054;
    font-size: 14px;
    font-weight: 600;
    text-align: left;
    cursor: pointer;
    transition: all 0.18s ease;

    &:hover {
      border-color: #8fc4ff;
      background: #f1f7ff;
      color: #2f7ed8;
    }
  }

  .el-icon {
    color: inherit;
    font-size: 22px;
  }
}

@media (max-width: 768px) {
  .home-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
