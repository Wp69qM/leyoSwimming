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
  const isRunningRef = useRef(isRunning)
  const timerRef = useRef<number | null>(null)

  useEffect(() => {
    isRunningRef.current = isRunning
  }, [isRunning])

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const stop = useCallback(() => {
    clearTimer()
    setIsRunning(false)
  }, [clearTimer])

  const tick = useCallback(() => {
    setSeconds((prev) => (prev <= 1 ? 0 : prev - 1))
  }, [])

  const start = useCallback(() => {
    if (isRunningRef.current) return
    clearTimer()
    setSeconds(initialSeconds)
    setIsRunning(true)
    timerRef.current = window.setInterval(tick, 1000)
  }, [initialSeconds, clearTimer, tick])

  const reset = useCallback(() => {
    clearTimer()
    setSeconds(0)
    setIsRunning(false)
  }, [clearTimer])

  useEffect(() => {
    if (seconds === 0 && isRunning) {
      stop()
    }
  }, [seconds, isRunning, stop])

  useEffect(() => {
    return () => {
      clearTimer()
    }
  }, [clearTimer])

  return { seconds, isRunning, canSend: !isRunning, start, reset }
}
