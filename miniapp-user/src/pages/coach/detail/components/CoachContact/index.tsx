import { View, Text } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';

import './index.scss';

type CoachContactProps = {
  phone: string | null;
  wechatQrUrl: string | null;
  isLoggedIn: boolean;
  isBoundOtherCoach: boolean;
  onLogin: () => void;
  onPhoneCall: () => void;
  onWechatPreview: () => void;
};

export function CoachContact({
  phone,
  wechatQrUrl,
  isLoggedIn,
  isBoundOtherCoach,
  onLogin,
  onPhoneCall,
  onWechatPreview,
}: CoachContactProps) {
  return (
    <View className='coach-detail-card'>
      <Text className='coach-detail-card__title'>联系方式</Text>
      {!isLoggedIn && (
        <View className='coach-detail-contact__login' onClick={onLogin}>
          <Text className='coach-detail-contact__login-text'>
            登录后查看联系方式
          </Text>
          <Icon
            name='arrow-right'
            className='coach-detail-contact__login-arrow'
          />
        </View>
      )}
      {isLoggedIn && isBoundOtherCoach && (
        <View className='coach-detail-contact__conflict'>
          <Icon name='notice' className='coach-detail-contact__conflict-icon' />
          <Text className='coach-detail-contact__conflict-text'>
            您已绑定其他教练，暂不可查看本教练联系方式
          </Text>
        </View>
      )}
      {isLoggedIn && !isBoundOtherCoach && (
        <View className='coach-detail-contact__list'>
          <View className='coach-detail-contact__item' onClick={onPhoneCall}>
            <View className='coach-detail-contact__icon coach-detail-contact__icon--phone'>
              <Icon name='phone' className='coach-detail-contact__icon-inner' />
            </View>
            <View className='coach-detail-contact__info'>
              <Text className='coach-detail-contact__label'>电话联系</Text>
              <Text className='coach-detail-contact__value'>
                {phone || '未设置'}
              </Text>
            </View>
            <Icon name='arrow-right' className='coach-detail-contact__arrow' />
          </View>
          {wechatQrUrl && (
            <View
              className='coach-detail-contact__item'
              onClick={onWechatPreview}
            >
              <View className='coach-detail-contact__icon coach-detail-contact__icon--wechat'>
                <Icon
                  name='user'
                  className='coach-detail-contact__icon-inner'
                />
              </View>
              <View className='coach-detail-contact__info'>
                <Text className='coach-detail-contact__label'>微信二维码</Text>
                <Text className='coach-detail-contact__value'>点击查看</Text>
              </View>
              <Icon
                name='arrow-right'
                className='coach-detail-contact__arrow'
              />
            </View>
          )}
        </View>
      )}
    </View>
  );
}
