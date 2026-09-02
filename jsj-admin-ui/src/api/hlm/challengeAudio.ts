import miniRequest from '@/utils/miniRequest'
import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/hearing/challenge/audio'

export interface ChallengeAudio {
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

export const listChallengeAudio = (params: Record<string, unknown>) => pageGet<ChallengeAudio>(BASE, params)
export const getChallengeAudio = (id: string) => getById<ChallengeAudio>(BASE, id)
export const saveChallengeAudio = (data: ChallengeAudio) => addOrUpdate(BASE, data)
export const removeChallengeAudio = (id: string) => deleteById(BASE, id)
export const generateChallengeAudio = (id: string) => miniRequest({
  url: `${BASE}/generate/${id}`,
  method: 'post',
  timeout: 120000
}) as Promise<{ data: ChallengeAudio; msg: string }>
