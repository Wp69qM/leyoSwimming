import { useEffect, useMemo, useRef, useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { getProfile, updateReferencePrice, type ReferencePriceResult } from '@/api/profile'
import { handleBusinessError } from '@/api/request'
import './index.scss'

function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

const PREVIEW_PACKAGES = [
  { label: '1 节体验课', count: 1 },
  { label: '6 节标准课', count: 6 },
  { label: '10 节标准课', count: 10 },
]

function validatePrice(value: string): string | null {
  if (!value) return '请输入参考单价'
  const num = Number(value)
  if (Number.isNaN(num)) return '请输入有效数字'
  if (num < 50 || num > 2000) return '参考单价需在 50-2000 元之间'
  const fixed = Number(num.toFixed(2))
  if (fixed !== num) return '最多保留两位小数'
  return null
}

export default function ReferencePricePage() {
  const [initialLoading, setInitialLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [price, setPrice] = useState('')
  const [result, setResult] = useState<ReferencePriceResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  const navigateBackTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useDidShow(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getProfile()
        if (cancelled) return
        const initialPrice = data.referencePrice ? String(data.referencePrice) : ''
        setPrice(initialPrice)
        setError(validatePrice(initialPrice))
      } catch (err) {
        if (cancelled) return
        Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
      } finally {
        if (!cancelled) setInitialLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  })

  useEffect(() => {
    return () => {
      if (navigateBackTimerRef.current) {
        clearTimeout(navigateBackTimerRef.current)
        navigateBackTimerRef.current = null
      }
    }
  }, [])

  const previewItems = useMemo(() => {
    const unit = Number(price)
    if (Number.isNaN(unit) || unit < 50 || unit > 2000) {
      return PREVIEW_PACKAGES.map((item) => ({ ...item, total: null }))
    }
    return PREVIEW_PACKAGES.map((item) => ({
      ...item,
      total: Math.round(unit * item.count * 100) / 100,
    }))
  }, [price])

  function handlePriceInput(value: string) {
    setPrice(value)
    setError(validatePrice(value))
  }

  async function handleSave() {
    const validationError = validatePrice(price)
    if (validationError) {
      setError(validationError)
      Taro.showToast({ title: validationError, icon: 'none' })
      return
    }

    setSaving(true)
    try {
      const res = await updateReferencePrice({
        referencePrice: Number(price),
        idempotencyKey: generateIdempotencyKey(),
      })
      setResult(res)
      Taro.showToast({ title: '保存成功', icon: 'success' })
      navigateBackTimerRef.current = setTimeout(() => {
        Taro.navigateBack()
      }, 800)
    } catch (err) {
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  if (initialLoading) {
    return (
      <View className='reference-price reference-price--loading'>
        <Text className='reference-price__loading-text'>加载中…</Text>
      </View>
    )
  }

  const remainingChanges = result?.remainingChangesToday ?? null
  const isInvalid = !!error || !price

  return (
    <View className='reference-price'>
      <View className='reference-price__info-card'>
        <Text className='reference-price__info-title'>参考单价说明</Text>
        <Text className='reference-price__info-content'>
          作为系统套餐定价和自定义套餐金额计算的基准。修改后不影响已购套餐。
        </Text>
      </View>

      <View className='reference-price__card'>
        <Text className='reference-price__card-title'>每节课参考单价</Text>
        <View className='reference-price__input-row'>
          <Input
            className='reference-price__input'
            value={price}
            onInput={(e) => handlePriceInput(e.detail.value)}
            placeholder='请输入 50-2000'
            type='digit'
          />
          <Text className='reference-price__unit'>元/节</Text>
        </View>
        <View className='reference-price__hint-row'>
          <Text className='reference-price__range-hint'>平台建议范围 50-2000 元/节</Text>
          <Text
            className={`reference-price__remaining ${remainingChanges === 0 ? 'reference-price__remaining--zero' : ''}`}
          >
            今日还可修改 {remainingChanges ?? '—'} 次
          </Text>
        </View>
        {error && <Text className='reference-price__error'>{error}</Text>}
      </View>

      <View className='reference-price__card'>
        <Text className='reference-price__card-title'>基于当前单价的新套餐示例</Text>
        <View className='reference-price__preview-list'>
          {previewItems.map((item) => (
            <View key={item.count} className='reference-price__preview-item'>
              <Text className='reference-price__preview-label'>{item.label}</Text>
              <View className='reference-price__preview-price-wrap'>
                {item.total !== null ? (
                  <>
                    <Text className='reference-price__preview-price'>{item.total}</Text>
                    <Text className='reference-price__preview-unit'>元</Text>
                  </>
                ) : (
                  <Text className='reference-price__preview-placeholder'>--</Text>
                )}
              </View>
            </View>
          ))}
        </View>
      </View>

      <Button className='reference-price__save' onClick={handleSave} disabled={saving || isInvalid}>
        {saving ? '保存中…' : '保存'}
      </Button>
    </View>
  )
}
