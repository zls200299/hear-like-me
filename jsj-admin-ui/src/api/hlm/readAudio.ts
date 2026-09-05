import miniRequest from '@/utils/miniRequest'
import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/read/aloud/audio'

export interface ReadAloudAudio {
  id?: string
  audioCode?: string
  title?: string
  description?: string
  sourceAssetId?: string
  outputAssetId?: string
  processingTaskNo?: string
  nChannels?: number
  carrier?: string
  fLo?: number
  fHi?: number
  envCut?: number
  spread?: number
  noiseLevel?: number
  versionNo?: number
  status?: string
  errorMessage?: string
  generatedTime?: string
  updateTime?: string
}

export const listReadAudio = (params: Record<string, unknown>) => pageGet<ReadAloudAudio>(BASE, params)
export const getReadAudio = (id: string) => getById<ReadAloudAudio>(BASE, id)
export const saveReadAudio = (data: ReadAloudAudio) => addOrUpdate(BASE, data)
export const removeReadAudio = (id: string) => deleteById(BASE, id)
export const generateReadAudio = (id: string) => miniRequest({
  url: `${BASE}/generate/${id}`,
  method: 'post',
  timeout: 120000
}) as Promise<{ data: ReadAloudAudio; msg: string }>
