import { pageGet } from './common'

const BASE = '/hearing/challenge/attempt'

export interface ChallengeAttempt {
  id?: string
  userId?: string
  userNickname?: string
  questionId?: string
  questionCode?: string
  questionTitle?: string
  audioBankId?: string
  audioTitle?: string
  audioAssetId?: string
  selectedChannels?: number
  correctChannels?: number
  isCorrect?: number
  createTime?: string
}

export const listChallengeAttempt = (params: Record<string, unknown>) =>
  pageGet<ChallengeAttempt>(BASE, params)
