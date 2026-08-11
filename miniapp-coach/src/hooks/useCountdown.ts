import { useCallback, useEffect, useRef, useState } from 'react'

export interface UseCountdownOptions {
  initialSeconds?: number
}

export interface UseCountdownReturn {
  seconds: number
  isRunning: boolean
  canSend: boolean
  start: () => void
  reset: () => void
}

export function useCountdown(options: UseCountdownOptions = {}): UseCountdownReturn {
  const { initialSeconds = 60 } = options
  const [seconds, setSeconds] = useState(0)
  const [isRunning, setIsRunning] = useState(false)
  const timerRef = useRef<number | null>(null)

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const start = useCallback(() => {
    if (isRunning) return
    clearTimer()
    setSeconds(initialSeconds)
    setIsRunning(true)
    timerRef.current = window.setInterval(() => {
      setSeconds((prev) => {
        if (prev <= 1) {
          clearTimer()
          setIsRunning(false)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }, [initialSeconds, isRunning, clearTimer])

  const reset = useCallback(() => {
    clearTimer()
    setSeconds(0)
    setIsRunning(false)
  }, [clearTimer])

  useEffect(() => {
    return () => {
      clearTimer()
    }
  }, [clearTimer])

  return { seconds, isRunning, canSend: !isRunning, start, reset }
}
