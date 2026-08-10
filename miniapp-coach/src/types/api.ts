export interface ApiResponse<T> {
  code: number
  message: string
  data: T | null
}

export interface ApiError {
  code: string
  message: string
}
