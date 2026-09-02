import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/sample/audio'

export interface SampleAudio {
  id?: string
  sampleCode?: string
  nameCn?: string
  nameEn?: string
  descriptionCn?: string
  descriptionEn?: string
  assetId?: string
  generatorType?: string
  sortOrder?: number
  enabled?: number
}

export const listSample = (params: Record<string, unknown>) => pageGet<SampleAudio>(BASE, params)
export const getSample = (id: string) => getById<SampleAudio>(BASE, id)
export const saveSample = (data: SampleAudio) => addOrUpdate(BASE, data)
export const removeSample = (id: string) => deleteById(BASE, id)
