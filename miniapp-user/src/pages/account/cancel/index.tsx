import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Button } from '@tarojs/components';
import {
  checkAccountCancel,
  cancelAccount,
  type CancelCheckResult,
} from '@/api/account';
import { useAuthStore } from '@/stores/authStore';
import { handleBusinessError } from '@/api/request';
import './index.scss';

type CancelCheckState = CancelCheckResult['checks'];

interface CheckItem {
  key: keyof CancelCheckState;
  label: string;
  unmetLabel: string;
}

const RISK_ITEMS = [
  '注销后账号不可找回，所有数据将被清除',
  '原账号数据与新账号完全隔离，无法合并',
  '重新登录视为新用户，不绑定原账号数据',
  '历史订单/交易记录保留 90 天后匿名化处理',
];

const CHECK_ITEMS: CheckItem[] = [
  {
    key: 'noActivePackage',
    label: '无活跃中的套餐',
    unmetLabel: '存在活跃中的套餐',
  },
  {
    key: 'noPendingOrder',
    label: '无未完成订单',
    unmetLabel: '存在未完成订单',
  },
  {
    key: 'noOngoingBooking',
    label: '无进行中或待上课的课程预约',
    unmetLabel: '存在进行中或待上课的课程预约',
  },
];

const SYSTEM_INFO = Taro.getSystemInfoSync();
const STATUS_BAR_HEIGHT = SYSTEM_INFO.statusBarHeight || 0;
const NAV_BAR_HEIGHT = 44;

export default function AccountCancelPage() {
  const logout = useAuthStore((state) => state.logout);
  const [checkState, setCheckState] = useState<CancelCheckState | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorTip, setErrorTip] = useState('');
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    loadCheckState();
  }, []);

  async function loadCheckState() {
    setLoading(true);
    setErrorTip('');
    try {
      const result = await checkAccountCancel();
      setCheckState(result.checks);
    } catch (error) {
      setErrorTip(handleBusinessError(error));
    } finally {
      setLoading(false);
    }
  }

  const allConditionsMet = checkState
    ? checkState.noActivePackage &&
      checkState.noPendingOrder &&
      checkState.noOngoingBooking
    : false;

  function handleSubmit() {
    if (!allConditionsMet || submitting) return;
    setShowModal(true);
  }

  function handleCancelModal() {
    if (submitting) return;
    setShowModal(false);
  }

  async function handleConfirmCancel() {
    if (submitting) return;

    setSubmitting(true);
    try {
      await cancelAccount();
      logout();
      Taro.redirectTo({ url: '/pages/login/wechat/index' });
    } catch (error) {
      setSubmitting(false);
      setShowModal(false);
      setErrorTip(handleBusinessError(error));
    }
  }

  function handleBack() {
    Taro.navigateBack();
  }

  function renderCheckIcon(met: boolean) {
    if (met) {
      return (
        <View className='account-cancel__check-icon account-cancel__check-icon--success'>
          <View className='account-cancel__check-tick' />
        </View>
      );
    }
    return (
      <View className='account-cancel__check-icon account-cancel__check-icon--error'>
        <Text className='account-cancel__check-exclamation'>!</Text>
      </View>
    );
  }

  return (
    <View className='account-cancel'>
      <View
        className='account-cancel__navbar'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View
          className='account-cancel__navbar-inner'
          style={{ height: `${NAV_BAR_HEIGHT}px` }}
        >
          <View className='account-cancel__navbar-back' onClick={handleBack}>
            <Text className='account-cancel__navbar-back-icon'>‹</Text>
          </View>
          <Text className='account-cancel__navbar-title'>注销账号</Text>
        </View>
      </View>

      <View
        className='account-cancel__content'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT + NAV_BAR_HEIGHT}px` }}
      >
        <View className='account-cancel__risk-card'>
          <View className='account-cancel__risk-header'>
            <View className='account-cancel__risk-header-icon' />
            <Text className='account-cancel__risk-title'>
              注销账号前，请确认以下重要信息
            </Text>
          </View>
          <View className='account-cancel__risk-list'>
            {RISK_ITEMS.map((item) => (
              <View className='account-cancel__risk-item' key={item}>
                <View className='account-cancel__risk-icon' />
                <Text className='account-cancel__risk-text'>{item}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className='account-cancel__checklist'>
          <Text className='account-cancel__checklist-title'>
            需满足以下条件
          </Text>
          {loading && !checkState ? (
            <View className='account-cancel__checklist-loading'>
              <Text className='account-cancel__checklist-loading-text'>
                加载中...
              </Text>
            </View>
          ) : (
            <View className='account-cancel__checklist-list'>
              {CHECK_ITEMS.map((item) => {
                const met = checkState ? checkState[item.key] : false;
                return (
                  <View
                    className={`account-cancel__checklist-item ${met ? '' : 'account-cancel__checklist-item--error'}`}
                    key={item.key}
                  >
                    <View className='account-cancel__checklist-left'>
                      {renderCheckIcon(met)}
                      <Text
                        className={`account-cancel__checklist-label ${met ? 'account-cancel__checklist-label--success' : ''}`}
                      >
                        {item.label}
                      </Text>
                    </View>
                    <Text
                      className={`account-cancel__checklist-status ${met ? 'account-cancel__checklist-status--success' : 'account-cancel__checklist-status--error'}`}
                    >
                      {met ? '已通过' : item.unmetLabel}
                    </Text>
                  </View>
                );
              })}
            </View>
          )}
        </View>

        {errorTip && (
          <View className='account-cancel__error'>
            <Text className='account-cancel__error-text'>{errorTip}</Text>
          </View>
        )}
      </View>

      <View className='account-cancel__footer'>
        <Button
          className={`account-cancel__submit ${allConditionsMet && !submitting ? '' : 'account-cancel__submit--disabled'}`}
          onClick={handleSubmit}
          disabled={!allConditionsMet || submitting}
          loading={submitting}
        >
          {submitting ? '注销中…' : '确认注销'}
        </Button>
      </View>

      {showModal && (
        <View className='account-cancel__modal'>
          <View
            className='account-cancel__modal-mask'
            onClick={handleCancelModal}
          />
          <View className='account-cancel__modal-body'>
            <Text className='account-cancel__modal-title'>确认注销</Text>
            <Text className='account-cancel__modal-content'>
              注销后账号不可找回，是否确认注销？
            </Text>
            <View className='account-cancel__modal-actions'>
              <Button
                className='account-cancel__modal-btn account-cancel__modal-btn--cancel'
                onClick={handleCancelModal}
                disabled={submitting}
              >
                取消
              </Button>
              <Button
                className='account-cancel__modal-btn account-cancel__modal-btn--confirm'
                onClick={handleConfirmCancel}
                loading={submitting}
                disabled={submitting}
              >
                确认
              </Button>
            </View>
          </View>
        </View>
      )}
    </View>
  );
}
