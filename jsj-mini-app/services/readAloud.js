const { request, resolveResourceUrl } = require('./request.js')

function extractData(response) {
  if (!response) return null
  if (typeof response.code === 'number') {
    if (response.code !== 200) {
      const error = new Error(response.msg || response.message || '请求失败')
      error.code = response.code
      error.response = response
      throw error
    }
    return response.data
  }
  return response
}

function normalizeCategory(item) {
  if (!item) return null
  return {
    id: item.id != null ? String(item.id) : '',
    categoryCode: item.categoryCode || '',
    name: item.name || '',
    caption: item.caption || '',
    coverUrl: resolveResourceUrl(item.coverUrl || ''),
    icon: item.icon || 'chat'
  }
}

function normalizeItem(item) {
  if (!item) return null
  return {
    id: item.id != null ? String(item.id) : '',
    categoryId: item.categoryId != null ? String(item.categoryId) : '',
    itemCode: item.itemCode || '',
    title: item.title || '',
    subtitle: item.subtitle || '',
    imageUrl: resolveResourceUrl(item.imageUrl || ''),
    audioAssetId: item.audioAssetId != null ? String(item.audioAssetId) : '',
    audioUrl: resolveResourceUrl(item.audioUrl || '')
  }
}

async function listCategories() {
  const response = await request({
    url: '/api/read-aloud/categories',
    method: 'GET',
    skipAuth: true
  })
  const data = extractData(response)
  const list = Array.isArray(data) ? data : []
  return list.map(normalizeCategory).filter(Boolean)
}

async function listItems(categoryId) {
  const response = await request({
    url: '/api/read-aloud/items',
    method: 'GET',
    data: { categoryId },
    skipAuth: true
  })
  const data = extractData(response)
  const list = Array.isArray(data) ? data : []
  return list.map(normalizeItem).filter(Boolean)
}

module.exports = {
  listCategories,
  listItems,
  resolveResourceUrl
}
