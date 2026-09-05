import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/read/aloud/item'

export interface ReadAloudItem {
  id?: string
  categoryId?: string
  itemCode?: string
  titleCn?: string
  titleEn?: string
  speechTextCn?: string
  descriptionCn?: string
  descriptionEn?: string
  imageAssetId?: string
  audioBankId?: string
  audioAssetId?: string
  processedAudioAssetId?: string
  playMode?: string
  defaultScenarioCode?: string
  status?: string
  sortOrder?: number
}

export const listReadItem = (params: Record<string, unknown>) =>
  pageGet<ReadAloudItem>(BASE, params)

export const getReadItem = (id: string) => getById<ReadAloudItem>(BASE, id)

export const saveReadItem = (data: ReadAloudItem) => addOrUpdate(BASE, data)

export const removeReadItem = (id: string) => deleteById(BASE, id)
