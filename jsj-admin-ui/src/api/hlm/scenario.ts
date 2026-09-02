import { addOrUpdate, deleteById, getById, pageGet } from './common'

const BASE = '/scenario/preset'

export interface ScenarioPreset {
  id?: string
  scenarioCode?: string
  nameCn?: string
  nameEn?: string
  descriptionCn?: string
  descriptionEn?: string
  icon?: string
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
  defaultSampleCode?: string
  sortOrder?: number
  enabled?: number
}

export const listScenario = (params: Record<string, unknown>) => pageGet<ScenarioPreset>(BASE, params)
export const getScenario = (id: string) => getById<ScenarioPreset>(BASE, id)
export const saveScenario = (data: ScenarioPreset) => addOrUpdate(BASE, data)
export const removeScenario = (id: string) => deleteById(BASE, id)
