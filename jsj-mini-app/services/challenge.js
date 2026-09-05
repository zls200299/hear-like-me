const { request } = require('./request.js')
const config = require('../config.js')

function extractData(response) {
  if (!response) return {}
  if (typeof response.code === 'number') {
    if (response.code !== 200) {
      const msg = response.msg || response.message || '请求失败'
      const error = new Error(msg)
      error.code = response.code
      error.response = response
      throw error
    }
    return response.data || {}
  }
  return response
}

function resolveAudioUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = (config.baseUrl || '').replace(/\/$/, '')
  const path = url.startsWith('/') ? url : `/${url}`
  return `${base}${path}`
}

function normalizeQuestion(data) {
  if (!data) return null
  return {
    id: data.id != null ? String(data.id) : '',
    questionCode: data.questionCode || '',
    title: data.title || '',
    description: data.description || '',
    audioAssetId: data.audioAssetId != null ? String(data.audioAssetId) : '',
    audioUrl: resolveAudioUrl(data.audioUrl || ''),
    index: Number(data.index) || 1,
    total: Number(data.total) || 0
  }
}

function normalizeSubmitResult(data) {
  if (!data) return null
  return {
    questionId: data.questionId != null ? String(data.questionId) : '',
    selectedChannels: Number(data.selectedChannels),
    correctChannels: Number(data.correctChannels),
    correct: !!data.correct,
    tip: data.tip || '',
    hasNext: !!data.hasNext,
    nextQuestionId: data.nextQuestionId != null ? String(data.nextQuestionId) : '',
    nextIndex: data.nextIndex != null ? Number(data.nextIndex) : null,
    total: Number(data.total) || 0
  }
}

/**
 * 拉取已发布题目列表
 */
async function listQuestions() {
  const response = await request({
    url: '/api/challenge/questions',
    method: 'GET',
    skipAuth: true
  })
  const data = extractData(response)
  const items = Array.isArray(data.items) ? data.items : []
  return {
    total: Number(data.total) || items.length,
    items: items.map((item, i) => ({
      id: item.id != null ? String(item.id) : '',
      questionCode: item.questionCode || '',
      title: item.title || '',
      description: item.description || '',
      sortOrder: item.sortOrder,
      index: Number(item.index) || i + 1
    }))
  }
}

/**
 * 按序号获取当前题（index 从 1 开始）
 */
async function getCurrentQuestion(index = 1) {
  const response = await request({
    url: '/api/challenge/questions/current',
    method: 'GET',
    data: { index },
    skipAuth: true
  })
  const question = normalizeQuestion(extractData(response))
  if (!question || !question.id) {
    throw new Error('题目数据无效')
  }
  return question
}

/**
 * 按 ID 获取题目详情
 */
async function getQuestionById(questionId) {
  const response = await request({
    url: `/api/challenge/questions/${questionId}`,
    method: 'GET',
    skipAuth: true
  })
  const question = normalizeQuestion(extractData(response))
  if (!question || !question.id) {
    throw new Error('题目数据无效')
  }
  return question
}

/**
 * 提交答案
 */
async function submitAnswer(questionId, selectedChannels) {
  const response = await request({
    url: '/api/challenge/answer',
    method: 'POST',
    data: {
      questionId: Number(questionId),
      selectedChannels
    }
  })
  const result = normalizeSubmitResult(extractData(response))
  if (!result) {
    throw new Error('提交结果无效')
  }
  return result
}

module.exports = {
  listQuestions,
  getCurrentQuestion,
  getQuestionById,
  submitAnswer,
  resolveAudioUrl,
  normalizeQuestion,
  normalizeSubmitResult
}
