<template>
  <div class="app-container placeholder-page">
    <el-result icon="info" :title="title" :sub-title="subtitle">
      <template #extra>
        <el-tag type="warning">{{ phase }} 规划中</el-tag>
        <p class="hint">菜单位置已预留，后续迭代在此模块继续开发。</p>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts" name="HlmPlaceholder">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const phase = computed(() => {
  try {
    const q = route.query.phase || (route.meta?.query && JSON.parse(String(route.meta.query))?.phase)
    return q ? String(q) : 'P1/P2'
  } catch {
    return 'P1/P2'
  }
})

const moduleName = computed(() => {
  try {
    if (route.query.module) return String(route.query.module)
    if (route.meta?.query) return JSON.parse(String(route.meta.query))?.module || ''
    return ''
  } catch {
    return ''
  }
})

const title = computed(() => route.meta?.title || '功能开发中')
const subtitle = computed(() =>
  moduleName.value ? `模块：${moduleName.value}` : '该功能将在后续版本开放'
)
</script>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}
.hint {
  margin-top: 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
