import { useCallback, useEffect, useState } from 'react'
import Taro, { usePullDownRefresh } from '@tarojs/taro'
import { View, Text, Button, Image, ScrollView } from '@tarojs/components'
import { getApplicationDetail, type CoachApplication, type CoachCertificate } from '@/api/onboarding'
import { handleBusinessError } from '@/api/request'
import { maskPhone } from '@/utils/phone'
import './index.scss'

const CLOCK_SVG = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIxNiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjRkFBRDE0IiBzdHJva2Utd2lkdGg9IjIuNSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+PHBvbHlsaW5lIHBvaW50cz0iMjAsMTIgMjAsMjAgMjYsMjQiIGZpbGw9Im5vbmUiIHN0cm9rZT0iI0ZBQUQxNCIgc3Ryb2tlLXdpZHRoPSIyLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIvPjwvc3ZnPg=='

const CERT_TYPE_LABELS: Record<string, string> = {
  ID_CARD_FRONT: '身份证正面照',
  ID_CARD_BACK: '身份证反面照',
  COACH_CERT: '教练资格证',
  HEALTH_CERT: '健康证',
  PORTRAIT: '个人形象照',
  OTHER: '其他证书',
}

const GENDER_LABELS: Record<string, string> = {
  male: '男',
  female: '女',
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return dateStr
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function maskIdCard(idCard?: string): string {
  if (!idCard || idCard.length < 8) return idCard || '-'
  return `${idCard.slice(0, 3)}***********${idCard.slice(-4)}`
}

function groupCertificates(certificates: CoachCertificate[]) {
  const groups: Record<string, CoachCertificate[]> = {}
  certificates.forEach((cert) => {
    const type = cert.certType || 'OTHER'
    if (!groups[type]) groups[type] = []
    groups[type].push(cert)
  })
  return groups
}

export default function CoachOnboardingPendingPage() {
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<CoachApplication | null>(null)
  const [errorTip, setErrorTip] = useState('')
  const [showSheet, setShowSheet] = useState(false)

  const loadDetail = useCallback(async () => {
    setLoading(true)
    setErrorTip('')
    try {
      const data = await getApplicationDetail()
      handleStatusRedirect(data)
      setDetail(data)
    } catch (error) {
      setErrorTip(handleBusinessError(error))
    } finally {
      setLoading(false)
      Taro.stopPullDownRefresh()
    }
  }, [])

  useEffect(() => {
    void loadDetail()
  }, [loadDetail])

  usePullDownRefresh(() => {
    void loadDetail()
  })

  function handleStatusRedirect(data: CoachApplication) {
    if (data.status === 0 && data.submittedAt) {
      return
    }
    if (data.status === 1) {
      Taro.redirectTo({ url: '/pages/index/index' })
      return
    }
    Taro.redirectTo({ url: '/pages/onboarding/index/index' })
  }

  function handlePreviewCertificate(url: string, urls: string[]) {
    Taro.previewImage({ current: url, urls })
  }

  function handleContact() {
    // 空实现：联系客服/帮助功能暂未开放，避免调用小程序客服 API 报错
  }

  const portraitUrl = detail?.certificates?.find((c) => c.certType === 'PORTRAIT')?.imageUrl

  return (
    <View className='coach-onboarding-pending'>
      <View className='coach-onboarding-pending__navbar'>
        <View className='coach-onboarding-pending__nav-title'>等待审核</View>
      </View>

      {loading && <View className='coach-onboarding-pending__loading'>加载中...</View>}

      {!loading && errorTip && (
        <View className='coach-onboarding-pending__error'>
          <View className='coach-onboarding-pending__error-text'>{errorTip}</View>
          <Button className='coach-onboarding-pending__error-btn' onClick={loadDetail}>
            点击重试
          </Button>
        </View>
      )}

      {!loading && detail && (
        <>
          <View className='coach-onboarding-pending__status'>
            <View className='coach-onboarding-pending__status-icon'>
              <Image
                className='coach-onboarding-pending__status-icon-img'
                src={CLOCK_SVG}
                mode='aspectFit'
              />
            </View>
            <View className='coach-onboarding-pending__status-title'>审核中，请耐心等待</View>
            <View className='coach-onboarding-pending__status-subtitle'>预计 1-3 个工作日内完成审核</View>
          </View>

          <View className='coach-onboarding-pending__card'>
            <View className='coach-onboarding-pending__card-header'>
              <View className='coach-onboarding-pending__card-title'>已提交资料</View>
              <View className='coach-onboarding-pending__status-tag'>审核中</View>
            </View>
            <View className='coach-onboarding-pending__profile'>
              <Image
                className='coach-onboarding-pending__avatar'
                src={portraitUrl || CLOCK_SVG}
                mode='aspectFill'
              />
              <View className='coach-onboarding-pending__profile-info'>
                <View className='coach-onboarding-pending__name'>{detail.name || '-'}</View>
                <View className='coach-onboarding-pending__phone'>{maskPhone(detail.phone || '')}</View>
              </View>
            </View>
            <View className='coach-onboarding-pending__meta'>
              <View className='coach-onboarding-pending__meta-item'>
                参考单价：
                {detail.referencePrice != null ? Number(detail.referencePrice).toFixed(2) : '-'}
                {' 元/节'}
              </View>
              <View className='coach-onboarding-pending__meta-item'>
                提交时间：{formatDateTime(detail.submittedAt)}
              </View>
            </View>
          </View>

          <Button
            className='coach-onboarding-pending__btn'
            onClick={() => setShowSheet(true)}
          >
            查看完整入驻资料
          </Button>

          <View className='coach-onboarding-pending__help' onClick={handleContact}>
            联系客服 / 帮助
          </View>
        </>
      )}

      {showSheet && detail && (
        <View className='coach-onboarding-pending__sheet' onClick={() => setShowSheet(false)}>
          <View className='coach-onboarding-pending__sheet-content' onClick={(e) => e.stopPropagation()}>
            <View className='coach-onboarding-pending__sheet-header'>
              <View className='coach-onboarding-pending__sheet-title'>完整入驻资料</View>
              <Text className='coach-onboarding-pending__sheet-close' onClick={() => setShowSheet(false)}>
                ✕
              </Text>
            </View>
            <ScrollView scrollY className='coach-onboarding-pending__sheet-body'>
              <View className='coach-onboarding-pending__sheet-body-inner'>
                <View className='coach-onboarding-pending__sheet-section'>
                  <View className='coach-onboarding-pending__sheet-section-title'>基础信息</View>
                  <DetailItem label='姓名' value={detail.name} />
                  <DetailItem label='性别' value={GENDER_LABELS[detail.gender || ''] || detail.gender} />
                  <DetailItem label='年龄' value={detail.age != null ? `${detail.age} 岁` : undefined} />
                  <DetailItem label='手机号' value={maskPhone(detail.phone || '')} />
                  <DetailItem label='邮箱' value={detail.email} />
                </View>

                <View className='coach-onboarding-pending__sheet-section'>
                  <View className='coach-onboarding-pending__sheet-section-title'>实名与资质</View>
                  <DetailItem label='身份证号' value={maskIdCard(detail.idCardNo)} />
                  {Object.entries(groupCertificates(detail.certificates || [])).map(([type, certs]) => (
                    <View key={type} className='coach-onboarding-pending__sheet-cert'>
                      <View className='coach-onboarding-pending__sheet-cert-title'>
                        {CERT_TYPE_LABELS[type] || type}
                      </View>
                      <View className='coach-onboarding-pending__sheet-images'>
                        {certs.map((cert, idx) => (
                          <Image
                            key={cert.imageUrl || idx}
                            className='coach-onboarding-pending__sheet-image'
                            src={cert.imageUrl}
                            mode='aspectFill'
                            onClick={() =>
                              handlePreviewCertificate(
                                cert.imageUrl,
                                certs.map((c) => c.imageUrl)
                              )
                            }
                          />
                        ))}
                      </View>
                    </View>
                  ))}
                </View>

                <View className='coach-onboarding-pending__sheet-section'>
                  <View className='coach-onboarding-pending__sheet-section-title'>教学履历</View>
                  <DetailItem
                    label='任教年限'
                    value={detail.teachingYears != null ? `${detail.teachingYears} 年` : undefined}
                  />
                  <DetailItem
                    label='总学员数'
                    value={detail.totalStudents != null ? `${detail.totalStudents} 人` : undefined}
                  />
                  <DetailItem
                    label='总课时数'
                    value={detail.totalHours != null ? `${detail.totalHours} 课时` : undefined}
                  />
                  <DetailItem
                    label='擅长泳姿'
                    value={detail.teachingStrokes?.length ? detail.teachingStrokes.join(' / ') : undefined}
                  />
                  <DetailItem label='个人简介' value={detail.bio} />
                </View>

                <View className='coach-onboarding-pending__sheet-section'>
                  <View className='coach-onboarding-pending__sheet-section-title'>服务设置</View>
                  <DetailItem
                    label='参考单价'
                    value={
                      detail.referencePrice != null
                        ? `${Number(detail.referencePrice).toFixed(2)} 元/节`
                        : undefined
                    }
                  />
                </View>
              </View>
            </ScrollView>
          </View>
        </View>
      )}
    </View>
  )
}

function DetailItem({ label, value }: { label: string; value?: string | null }) {
  return (
    <View className='coach-onboarding-pending__sheet-item'>
      <View className='coach-onboarding-pending__sheet-label'>{label}</View>
      <View className='coach-onboarding-pending__sheet-value'>{value || '-'}</View>
    </View>
  )
}
