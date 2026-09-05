import miniRequest from '@/utils/miniRequest'

export interface ChallengeStatsSummary {
  totalAttempts?: number
  uniqueUsers?: number
  correctRate?: number
  attemptsLast7Days?: number
  correctCount?: number
}

export interface ChallengeStatsTrendPoint {
  date?: string
  attempts?: number
  correct?: number
}

export interface ChallengeStatsQuestionItem {
  questionId?: string
  questionTitle?: string
  audioTitle?: string
  attempts?: number
  correct?: number
  correctRate?: number
}

export interface ChallengeStatsUserItem {
  userId?: string
  userNickname?: string
  attempts?: number
  correct?: number
  correctRate?: number
}

export interface ChallengeStatsResp {
  summary?: ChallengeStatsSummary
  trend?: ChallengeStatsTrendPoint[]
  questionStats?: ChallengeStatsQuestionItem[]
  topUsers?: ChallengeStatsUserItem[]
}

export function getChallengeStats(days: number = 7) {
  return miniRequest({
    url: '/hearing/challenge/stats/overview',
    method: 'get',
    params: { days }
  }) as Promise<{ data: ChallengeStatsResp }>
}
