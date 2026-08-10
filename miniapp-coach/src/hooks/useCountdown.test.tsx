import { renderHook, act } from '@testing-library/react'
import { useCountdown } from './useCountdown'

describe('useCountdown', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  test('returns initial state', () => {
    const { result } = renderHook(() => useCountdown())
    expect(result.current.seconds).toBe(0)
    expect(result.current.isRunning).toBe(false)
  })

  test('starts countdown from default 60 seconds', () => {
    const { result } = renderHook(() => useCountdown())

    act(() => {
      result.current.start()
    })

    expect(result.current.seconds).toBe(60)
    expect(result.current.isRunning).toBe(true)

    act(() => {
      jest.advanceTimersByTime(1000)
    })
    expect(result.current.seconds).toBe(59)
  })

  test('uses custom initialSeconds', () => {
    const { result } = renderHook(() => useCountdown({ initialSeconds: 10 }))

    act(() => {
      result.current.start()
    })

    expect(result.current.seconds).toBe(10)
  })

  test('stops at zero', () => {
    const { result } = renderHook(() => useCountdown({ initialSeconds: 2 }))

    act(() => {
      result.current.start()
    })

    act(() => {
      jest.advanceTimersByTime(3000)
    })

    expect(result.current.seconds).toBe(0)
    expect(result.current.isRunning).toBe(false)
  })

  test('reset stops countdown and clears seconds', () => {
    const { result } = renderHook(() => useCountdown({ initialSeconds: 30 }))

    act(() => {
      result.current.start()
    })

    act(() => {
      jest.advanceTimersByTime(5000)
    })
    expect(result.current.seconds).toBe(25)

    act(() => {
      result.current.reset()
    })

    expect(result.current.seconds).toBe(0)
    expect(result.current.isRunning).toBe(false)
  })

  test('start is ignored while running', () => {
    const { result } = renderHook(() => useCountdown({ initialSeconds: 10 }))

    act(() => {
      result.current.start()
    })

    act(() => {
      jest.advanceTimersByTime(3000)
    })
    expect(result.current.seconds).toBe(7)

    act(() => {
      result.current.start()
    })
    expect(result.current.seconds).toBe(7)
  })

  test('cleans up timer on unmount', () => {
    const { result, unmount } = renderHook(() => useCountdown({ initialSeconds: 10 }))

    act(() => {
      result.current.start()
    })

    unmount()

    expect(jest.getTimerCount()).toBe(0)
  })
})
