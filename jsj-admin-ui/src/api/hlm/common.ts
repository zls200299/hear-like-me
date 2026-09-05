import miniRequest, { type MiniPageResult } from '@/utils/miniRequest'
import { parseTime } from '@/utils/ruoyi'

export interface PageQuery {
  currentPage?: number
  pageSize?: number
  [key: string]: unknown
}

export function parseMiniPage<T>(res: { data?: MiniPageResult<T> | null }): { list: T[]; total: number } {
  const page = res?.data
  const list = page?.records ?? []
  const total = page?.total != null ? Number(page.total) : list.length
  return { list, total }
}

/** 小程序业务接口返回的时间字段统一格式化（若依 parseTime，本地时区） */
export function formatDateTime(value?: string | number | Date | null): string {
  if (value == null || value === '') return '-'
  return parseTime(value) ?? '-'
}

export function pageGet<T>(url: string, params: PageQuery) {
  return miniRequest({
    url: `${url}/getByPage`,
    method: 'get',
    params
  }) as Promise<{ data: MiniPageResult<T> }>
}

export function getById<T>(url: string, id: string | number) {
  return miniRequest({
    url: `${url}/getById`,
    method: 'get',
    params: { id }
  }) as Promise<{ data: T }>
}

export function addOrUpdate<T>(url: string, data: T) {
  return miniRequest({
    url: `${url}/addOrUpdate`,
    method: 'post',
    data
  })
}

export function deleteById(url: string, id: string | number) {
  return miniRequest({
    url: `${url}/delete/${id}`,
    method: 'get'
  })
}

export function uploadAudio(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return miniRequest({
    url: '/api/files/audio',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<{ data: { assetId: string; fileName: string; url: string } }>
}

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return miniRequest({
    url: '/api/files/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<{ data: { assetId: string; fileName: string; url: string } }>
}
