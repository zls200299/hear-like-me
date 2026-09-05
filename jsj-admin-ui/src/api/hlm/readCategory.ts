import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/read/aloud/category'

export interface ReadAloudCategory {
  id?: string
  categoryCode?: string
  nameCn?: string
  nameEn?: string
  coverAssetId?: string
  sortOrder?: number
  enabled?: number
}

export const listReadCategory = (params: Record<string, unknown>) =>
  pageGet<ReadAloudCategory>(BASE, params)

export const getReadCategory = (id: string) => getById<ReadAloudCategory>(BASE, id)

export const saveReadCategory = (data: ReadAloudCategory) => addOrUpdate(BASE, data)

export const removeReadCategory = (id: string) => deleteById(BASE, id)
