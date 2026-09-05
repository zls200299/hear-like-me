<template>
  <div class="stats-page" v-loading="loading">
    <div class="stats-hero">
      <div>
        <h2 class="stats-hero__title">挑战统计</h2>
        <p class="stats-hero__desc">基于真实答题记录，观察参与度、正确率与活跃用户</p>
      </div>
      <el-radio-group v-model="days" size="default" @change="loadData">
        <el-radio-button :value="7">近 7 日</el-radio-button>
        <el-radio-button :value="30">近 30 日</el-radio-button>
      </el-radio-group>
    </div>

    <div class="kpi-row">
      <div class="kpi-tile" v-for="item in kpiList" :key="item.key">
        <div class="kpi-tile__label">{{ item.label }}</div>
        <div class="kpi-tile__value">
          {{ item.value }}
          <span v-if="item.suffix" class="kpi-tile__suffix">{{ item.suffix }}</span>
        </div>
        <div class="kpi-tile__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="panel panel--trend">
      <div class="panel__head">
        <div>
          <div class="panel__title">答题趋势</div>
          <div class="panel__subtitle">每日作答次数与答对次数</div>
        </div>
      </div>
      <div ref="trendRef" class="chart chart--trend"></div>
      <div v-if="!hasTrendData" class="panel__empty">暂无趋势数据，登录后在小程序答几题即可出现</div>
    </div>

    <div class="bottom-grid">
      <div class="panel">
        <div class="panel__head">
          <div>
            <div class="panel__title">题目难度</div>
            <div class="panel__subtitle">作答较多的题目正确率对比</div>
          </div>
        </div>
        <div ref="questionRef" class="chart chart--question"></div>
        <div v-if="!questionStats.length" class="panel__empty">暂无题目统计</div>
      </div>

      <div class="panel">
        <div class="panel__head">
          <div>
            <div class="panel__title">活跃答题用户 Top</div>
            <div class="panel__subtitle">按作答次数排序，最多展示 10 人</div>
          </div>
        </div>
        <div v-if="topUsers.length" class="rank-list">
          <div v-for="(user, index) in topUsers" :key="user.userId || index" class="rank-item">
            <div class="rank-item__index" :class="{ 'is-top': index < 3 }">{{ index + 1 }}</div>
            <div class="rank-item__body">
              <div class="rank-item__meta">
                <span class="rank-item__name">{{ user.userNickname || '未设置昵称' }}</span>
                <span class="rank-item__count">{{ user.attempts || 0 }} 次</span>
              </div>
              <div class="rank-item__bar">
                <div class="rank-item__fill" :style="{ width: userBarWidth(user) }"></div>
              </div>
              <div class="rank-item__sub">
                答对 {{ user.correct || 0 }} · 正确率 {{ formatRate(user.correctRate) }}
              </div>
            </div>
          </div>
        </div>
        <div v-else class="panel__empty">暂无用户排行</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="HlmChallengeStats">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  getChallengeStats,
  type ChallengeStatsQuestionItem,
  type ChallengeStatsTrendPoint,
  type ChallengeStatsUserItem
} from '@/api/hlm/challengeStats'

const loading = ref(false)
const days = ref(7)
const summary = ref({
  totalAttempts: 0,
  uniqueUsers: 0,
  correctRate: 0,
  attemptsLast7Days: 0,
  correctCount: 0
})
const trend = ref<ChallengeStatsTrendPoint[]>([])
const questionStats = ref<ChallengeStatsQuestionItem[]>([])
const topUsers = ref<ChallengeStatsUserItem[]>([])

const trendRef = ref<HTMLDivElement | null>(null)
const questionRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let questionChart: echarts.ECharts | null = null

const hasTrendData = computed(() => trend.value.some((p) => Number(p.attempts || 0) > 0))

const kpiList = computed(() => [
  {
    key: 'total',
    label: '答题总次数',
    value: formatNumber(summary.value.totalAttempts),
    hint: `累计答对 ${formatNumber(summary.value.correctCount)} 次`,
    suffix: ''
  },
  {
    key: 'users',
    label: '参与人数',
    value: formatNumber(summary.value.uniqueUsers),
    hint: '去重后的答题用户',
    suffix: ''
  },
  {
    key: 'rate',
    label: '整体正确率',
    value: formatRate(summary.value.correctRate),
    hint: '答对次数 / 总次数',
    suffix: ''
  },
  {
    key: 'week',
    label: '近 7 日答题',
    value: formatNumber(summary.value.attemptsLast7Days),
    hint: '最近一周活跃度',
    suffix: ''
  }
])

const maxUserAttempts = computed(() =>
  Math.max(1, ...topUsers.value.map((u) => Number(u.attempts || 0)))
)

function formatNumber(value?: number) {
  const n = Number(value || 0)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN') : '0'
}

function formatRate(value?: number) {
  const n = Number(value || 0)
  return `${Number.isFinite(n) ? n.toFixed(1) : '0.0'}%`
}

function userBarWidth(user: ChallengeStatsUserItem) {
  const attempts = Number(user.attempts || 0)
  return `${Math.max(8, Math.round((attempts / maxUserAttempts.value) * 100))}%`
}

async function loadData() {
  loading.value = true
  try {
    const res = await getChallengeStats(days.value)
    const data = res.data || {}
    summary.value = {
      totalAttempts: Number(data.summary?.totalAttempts || 0),
      uniqueUsers: Number(data.summary?.uniqueUsers || 0),
      correctRate: Number(data.summary?.correctRate || 0),
      attemptsLast7Days: Number(data.summary?.attemptsLast7Days || 0),
      correctCount: Number(data.summary?.correctCount || 0)
    }
    trend.value = data.trend || []
    questionStats.value = data.questionStats || []
    topUsers.value = data.topUsers || []
    await nextTick()
    renderTrend()
    renderQuestion()
  } finally {
    loading.value = false
  }
}

function ensureTrendChart() {
  if (!trendRef.value) return null
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value)
  }
  return trendChart
}

function ensureQuestionChart() {
  if (!questionRef.value) return null
  if (!questionChart) {
    questionChart = echarts.init(questionRef.value)
  }
  return questionChart
}

function renderTrend() {
  const chart = ensureTrendChart()
  if (!chart) return

  const labels = trend.value.map((p) => {
    const raw = p.date || ''
    return raw.length >= 10 ? raw.slice(5) : raw
  })
  const attempts = trend.value.map((p) => Number(p.attempts || 0))
  const correct = trend.value.map((p) => Number(p.correct || 0))

  chart.setOption({
    color: ['#1f9f95', '#7bc9c1'],
    grid: { left: 36, right: 20, top: 36, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 31, 46, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#f3f7fb' }
    },
    legend: {
      right: 0,
      top: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#60758a' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#d7e1ea' } },
      axisLabel: { color: '#7a8ea3' },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#edf2f7', type: 'dashed' } },
      axisLabel: { color: '#7a8ea3' }
    },
    series: [
      {
        name: '作答',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(31, 159, 149, 0.28)' },
            { offset: 1, color: 'rgba(31, 159, 149, 0.02)' }
          ])
        },
        data: attempts
      },
      {
        name: '答对',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2 },
        data: correct
      }
    ]
  })
}

function renderQuestion() {
  const chart = ensureQuestionChart()
  if (!chart) return

  const rows = [...questionStats.value].reverse()
  const names = rows.map((q) => {
    const title = q.questionTitle || q.audioTitle || `题目 ${q.questionId || ''}`
    return title.length > 12 ? `${title.slice(0, 12)}…` : title
  })
  const rates = rows.map((q) => Number(q.correctRate || 0))
  const attempts = rows.map((q) => Number(q.attempts || 0))

  chart.setOption({
    grid: { left: 100, right: 56, top: 8, bottom: 8 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params: any) {
        const item = Array.isArray(params) ? params[0] : params
        const idx = item?.dataIndex ?? 0
        const row = rows[idx]
        return `${row?.questionTitle || item?.name}<br/>作答 ${attempts[idx]} 次<br/>正确率 ${formatRate(rates[idx])}`
      }
    },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#7a8ea3' },
      splitLine: { lineStyle: { color: '#edf2f7', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#4f6478' }
    },
    series: [
      {
        type: 'bar',
        data: rates,
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#2bb8ad' },
            { offset: 1, color: '#1a8f86' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#5f758a',
          formatter: (p: any) => `${Number(p.value || 0).toFixed(0)}%`
        }
      }
    ]
  })
}

function handleResize() {
  trendChart?.resize()
  questionChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  questionChart?.dispose()
  trendChart = null
  questionChart = null
})
</script>

<style scoped lang="scss">
.stats-page {
  --ink: #173041;
  --muted: #6f8498;
  --line: #e6eef4;
  --panel: #ffffff;
  --soft: #f4f8fb;
  --accent: #1f9f95;
  min-height: calc(100vh - 120px);
  padding: 8px 4px 24px;
  background:
    radial-gradient(900px 280px at 8% -10%, rgba(31, 159, 149, 0.1), transparent 60%),
    radial-gradient(700px 240px at 100% 0%, rgba(90, 140, 170, 0.08), transparent 55%),
    var(--soft);
}

.stats-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 8px 8px 0;
}

.stats-hero__title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--ink);
}

.stats-hero__desc {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
  padding: 0 4px;
}

.kpi-tile {
  position: relative;
  overflow: hidden;
  padding: 18px 18px 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f7fbfc);
  box-shadow: 0 10px 28px rgba(23, 48, 65, 0.04);
}

.kpi-tile::after {
  content: '';
  position: absolute;
  right: -20px;
  top: -24px;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: rgba(31, 159, 149, 0.08);
}

.kpi-tile__label {
  color: var(--muted);
  font-size: 13px;
}

.kpi-tile__value {
  margin-top: 10px;
  color: var(--ink);
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0.5px;
}

.kpi-tile__suffix {
  margin-left: 2px;
  font-size: 14px;
  font-weight: 500;
}

.kpi-tile__hint {
  margin-top: 8px;
  color: #8aa0b3;
  font-size: 12px;
}

.panel {
  position: relative;
  margin: 0 4px 16px;
  padding: 18px 18px 10px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: var(--panel);
  box-shadow: 0 12px 30px rgba(23, 48, 65, 0.035);
}

.panel--trend {
  padding-bottom: 8px;
}

.panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.panel__title {
  color: var(--ink);
  font-size: 16px;
  font-weight: 650;
}

.panel__subtitle {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.panel__empty {
  position: absolute;
  inset: 64px 18px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a7b8;
  font-size: 13px;
  pointer-events: none;
}

.chart--trend {
  height: 320px;
}

.chart--question {
  height: 360px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 14px;
  padding: 0 0 8px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 2px 12px;
  max-height: 360px;
  overflow: auto;
}

.rank-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.rank-item__index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #eef4f8;
  color: #6d8194;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-item__index.is-top {
  background: rgba(31, 159, 149, 0.14);
  color: var(--accent);
}

.rank-item__body {
  flex: 1;
  min-width: 0;
}

.rank-item__meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.rank-item__name {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-item__count {
  flex-shrink: 0;
  color: var(--accent);
  font-size: 13px;
  font-weight: 650;
}

.rank-item__bar {
  height: 8px;
  border-radius: 999px;
  background: #edf3f7;
  overflow: hidden;
}

.rank-item__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2bb8ad, #1a8f86);
}

.rank-item__sub {
  margin-top: 6px;
  color: #8aa0b3;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .kpi-row,
  .bottom-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .stats-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .kpi-row,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .chart--trend,
  .chart--question {
    height: 280px;
  }
}
</style>
