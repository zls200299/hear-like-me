import { deleteById, getById, pageGet } from './common'
import miniRequest from '@/utils/miniRequest'

const BASE = '/audio/processing/task'

export interface AudioProcessingTask {
  id?: string
  taskNo?: string
  userId?: string
  sourceType?: string
  sampleCode?: string
  scenarioCode?: string
  sourceAssetId?: string
  normalizedAssetId?: string
  outputAssetId?: string
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
  randomSeed?: string
  algorithmVersion?: string
  taskStatus?: string
  progress?: number
  retryCount?: number
  clarityScore?: number
  clarityGrade?: string
  errorCode?: string
  errorMessage?: string
  queueWaitMs?: number
  processingMs?: number
  processingStartedTime?: string
  processingFinishedTime?: string
  createTime?: string
  updateTime?: string
}

export interface AudioProcessingTaskEvent {
  id?: string
  taskId?: string
  eventType?: string
  stage?: string
  progress?: number
  message?: string
  detailJson?: string
  createTime?: string
}

export interface AudioProcessingTaskDetail {
  task?: AudioProcessingTask
  events?: AudioProcessingTaskEvent[]
}

export const listAudioTask = (params: Record<string, unknown>) =>
  pageGet<AudioProcessingTask>(BASE, params)

export const getAudioTask = (id: string) => getById<AudioProcessingTask>(BASE, id)

export const getAudioTaskDetail = (id: string) =>
  miniRequest({
    url: `${BASE}/detail`,
    method: 'get',
    params: { id }
  }) as Promise<{ data: AudioProcessingTaskDetail }>

export const removeAudioTask = (id: string) => deleteById(BASE, id)
