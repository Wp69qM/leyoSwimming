export type CoachStatus = -1 | 0 | 1 | 2 | 3 | 4

export interface CoachInfo {
  id: number
  name?: string
  phone?: string
  avatar?: string
  status: CoachStatus
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  isNewCoach: boolean
  coachStatus: CoachStatus
  coachId: number
  profileCompleted: boolean
}
