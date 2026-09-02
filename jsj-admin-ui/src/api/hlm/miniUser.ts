import { getById, pageGet } from './common'
import miniRequest from '@/utils/miniRequest'

const BASE = '/user/mini'

export interface MiniUser {
  id?: string
  nickname?: string
  avatar?: string
  bio?: string
  status?: number
  sourceType?: number
  openIdMasked?: string
  unionIdMasked?: string
  miniAppId?: string
  registerTime?: string
  lastActiveTime?: string
  createTime?: string
  updateTime?: string
}

export const listMiniUser = (params: Record<string, unknown>) => pageGet<MiniUser>(BASE, params)
export const getMiniUser = (id: string) => getById<MiniUser>(BASE, id)

export function updateMiniUserStatus(id: string, status: number) {
  return miniRequest({
    url: `${BASE}/updateStatus`,
    method: 'post',
    data: { id, status }
  })
}
