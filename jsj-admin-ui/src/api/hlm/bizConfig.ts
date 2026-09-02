import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/system/config'

export interface BizConfig {
  id?: string
  configKey?: string
  configValue?: string
  valueType?: string
  description?: string
  enabled?: number
}

export const listBizConfig = (params: Record<string, unknown>) => pageGet<BizConfig>(BASE, params)
export const getBizConfig = (id: string) => getById<BizConfig>(BASE, id)
export const saveBizConfig = (data: BizConfig) => addOrUpdate(BASE, data)
export const removeBizConfig = (id: string) => deleteById(BASE, id)
