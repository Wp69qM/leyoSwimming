import { useEffect, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { getProfile, updateReferencePrice, type ReferencePriceResult } from '@/api/profile'
import { handleBusinessError } from '@/api/request'
import './index.scss'

function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export default function ReferencePricePage() {
  const [initialLoading, setInitialLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [currentPrice, setCurrentPrice] = useState<number | null>(null)
  const [price, setPrice] = useState('')
  const [result, setResult] = useState<ReferencePriceResult | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getProfile()
        if (cancelled) return
        setCurrentPrice(data.referencePrice ?? null)
        setPrice(data.referencePrice ? String(data.referencePrice) : '')
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
  }, [])

  function validate(): string | null {
    const value = Number(price)
    if (!price || Number.isNaN(value)) return '请输入参考单价'
    if (value < 50 || value > 2000) return '参考单价需在 50-2000 元之间'
    const fixed = Number(value.toFixed(2))
    if (fixed !== value) return '最多保留两位小数'
    return null
  }

  async function handleSave() {
    const error = validate()
    if (error) {
      Taro.showToast({ title: error, icon: 'none' })
      return
    }

    setSaving(true)
    try {
      const res = await updateReferencePrice({
        referencePrice: Number(price),
        idempotencyKey: generateIdempotencyKey(),
      })
      setResult(res)
      setCurrentPrice(res.referencePrice)
      Taro.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
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

  const remainingChanges = result?.remainingChangesToday ?? 3

  return (
    <View className='reference-price'>
      <View className='reference-price__card'>
        <Text className='reference-price__title'>参考单价</Text>
        <Text className='reference-price__subtitle'>学员端展示的单节课程参考价格</Text>

        <View className='reference-price__input-wrap'>
          <Text className='reference-price__currency'>¥</Text>
          <Input
            className='reference-price__input'
            value={price}
            onInput={(e) => setPrice(e.detail.value)}
            placeholder='请输入 50-2000'
            type='digit'
          />
          <Text className='reference-price__unit'>元/节</Text>
        </View>

        <Text className='reference-price__hint'>
          当前参考单价：{currentPrice ? `${currentPrice} 元/节` : '未设置'}
        </Text>
        <Text className={`reference-price__remaining ${remainingChanges === 0 ? 'reference-price__remaining--zero' : ''}`}>
          今日还可修改 {remainingChanges} 次
        </Text>
      </View>

      <Button className='reference-price__save' onClick={handleSave} disabled={saving}>
        {saving ? '保存中…' : '保存'}
      </Button>
    </View>
  )
}
