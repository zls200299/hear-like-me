import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/hearing/challenge'

export interface HearingChallenge {
  id?: string
  questionCode?: string
  title?: string
  description?: string
  audioAssetId?: string
  nChannels?: number
  carrier?: string
  fLo?: number
  fHi?: number
  envCut?: number
  spread?: number
  noiseLevel?: number
  envAmp?: number
  wetMix?: number
  compressEnabled?: number
  normalizePeak?: number
  correctTip?: string
  wrongTip?: string
  status?: string
  sortOrder?: number
}

export const listChallenge = (params: Record<string, unknown>) => pageGet<HearingChallenge>(BASE, params)
export const getChallenge = (id: string) => getById<HearingChallenge>(BASE, id)
export const saveChallenge = (data: HearingChallenge) => addOrUpdate(BASE, data)
export const removeChallenge = (id: string) => deleteById(BASE, id)
