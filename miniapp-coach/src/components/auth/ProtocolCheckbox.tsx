import { View, Text } from '@tarojs/components'

export interface ProtocolCheckboxProps {
  checked: boolean
  onChange: (checked: boolean) => void
  onOpenTerms?: () => void
  onOpenPrivacy?: () => void
}

export function ProtocolCheckbox({
  checked,
  onChange,
  onOpenTerms,
  onOpenPrivacy
}: ProtocolCheckboxProps) {
  return (
    <View className="protocol-checkbox">
      <View
        className={`protocol-checkbox__box ${checked ? 'protocol-checkbox__box--checked' : ''}`}
        onClick={() => onChange(!checked)}
      >
        {checked ? (
          <Text className="protocol-checkbox__check">✓</Text>
        ) : (
          <View className="protocol-checkbox__dot" />
        )}
      </View>
      <Text className="protocol-checkbox__text">
        已阅读并同意
        <Text
          className="protocol-checkbox__link"
          onClick={(e) => {
            e.stopPropagation()
            onOpenPrivacy?.()
          }}
        >
          《隐私协议》
        </Text>
        <Text
          className="protocol-checkbox__link"
          onClick={(e) => {
            e.stopPropagation()
            onOpenTerms?.()
          }}
        >
          《用户须知》
        </Text>
      </Text>
    </View>
  )
}
