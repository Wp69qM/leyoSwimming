import { useEffect, useState } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import {
  getCurrentPrivacy,
  getCurrentTerms,
  type PolicyInfo,
} from '@/api/policy';
import './ProtocolDrawer.scss';

export type ProtocolTab = 'terms' | 'privacy';

export interface ProtocolDrawerProps {
  visible: boolean;
  initialTab?: ProtocolTab;
  onClose: () => void;
  onAgree?: () => void;
}

const TABS: { key: ProtocolTab; label: string }[] = [
  { key: 'terms', label: '用户须知' },
  { key: 'privacy', label: '隐私协议' },
];

export function ProtocolDrawer({
  visible,
  initialTab = 'terms',
  onClose,
  onAgree,
}: ProtocolDrawerProps) {
  const [activeTab, setActiveTab] = useState<ProtocolTab>(initialTab);
  const [terms, setTerms] = useState<PolicyInfo | null>(null);
  const [privacy, setPrivacy] = useState<PolicyInfo | null>(null);
  const [loading, setLoading] = useState<Record<ProtocolTab, boolean>>({
    terms: false,
    privacy: false,
  });
  const [error, setError] = useState<Record<ProtocolTab, string>>({
    terms: '',
    privacy: '',
  });

  useEffect(() => {
    if (visible) {
      setActiveTab(initialTab);
    }
  }, [visible, initialTab]);

  useEffect(() => {
    if (!visible) return;

    let cancelled = false;

    async function load(tab: ProtocolTab) {
      const alreadyLoaded = tab === 'privacy' ? privacy : terms;
      if (alreadyLoaded) return;

      setLoading((prev) => ({ ...prev, [tab]: true }));
      setError((prev) => ({ ...prev, [tab]: '' }));

      try {
        const res =
          tab === 'privacy'
            ? await getCurrentPrivacy()
            : await getCurrentTerms();
        if (cancelled) return;

        if (tab === 'privacy') {
          setPrivacy(res);
        } else {
          setTerms(res);
        }
      } catch {
        if (cancelled) return;
        setError((prev) => ({
          ...prev,
          [tab]: '协议内容加载失败，请重试',
        }));
      } finally {
        if (!cancelled) {
          setLoading((prev) => ({ ...prev, [tab]: false }));
        }
      }
    }

    load(activeTab);

    return () => {
      cancelled = true;
    };
  }, [visible, activeTab, terms, privacy]);

  function handleRetry() {
    if (activeTab === 'privacy') {
      setPrivacy(null);
    } else {
      setTerms(null);
    }
  }

  function handleAgree() {
    onAgree?.();
    onClose();
  }

  const currentPolicy = activeTab === 'privacy' ? privacy : terms;
  const isLoading = loading[activeTab];
  const currentError = error[activeTab];

  function formatDate(dateString?: string) {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return dateString;
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
  }

  if (!visible) return null;

  return (
    <View className='protocol-drawer'>
      <View className='protocol-drawer__mask' onClick={onClose} />
      <View className='protocol-drawer__sheet'>
        <View className='protocol-drawer__indicator' />

        <View className='protocol-drawer__tabs'>
          {TABS.map((tab) => (
            <View
              key={tab.key}
              className={`protocol-drawer__tab ${activeTab === tab.key ? 'protocol-drawer__tab--active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <Text className='protocol-drawer__tab-text'>{tab.label}</Text>
            </View>
          ))}
        </View>

        <View className='protocol-drawer__body'>
          <ScrollView className='protocol-drawer__scroll' scrollY>
            <View className='protocol-drawer__scroll-inner'>
              {isLoading && (
                <View className='protocol-drawer__status'>
                  <View className='protocol-drawer__loading-icon' />
                  <Text className='protocol-drawer__status-text'>加载中…</Text>
                </View>
              )}

              {!isLoading && currentError && (
                <View className='protocol-drawer__status'>
                  <View className='protocol-drawer__error-icon' />
                  <Text className='protocol-drawer__status-text'>
                    {currentError}
                  </Text>
                  <Text
                    className='protocol-drawer__retry'
                    onClick={handleRetry}
                  >
                    重试
                  </Text>
                </View>
              )}

              {!isLoading && !currentError && currentPolicy && (
                <View className='protocol-drawer__content'>
                  <View className='protocol-drawer__dates'>
                    <Text className='protocol-drawer__date'>
                      更新日期：{formatDate(currentPolicy.effectiveAt)}
                    </Text>
                    <Text className='protocol-drawer__date'>
                      生效日期：{formatDate(currentPolicy.effectiveAt)}
                    </Text>
                  </View>
                  <Text className='protocol-drawer__content-text'>
                    {currentPolicy.content}
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>
        </View>

        <View className='protocol-drawer__footer'>
          <View
            className='protocol-drawer__btn protocol-drawer__btn--disagree'
            onClick={onClose}
          >
            <Text className='protocol-drawer__btn-text protocol-drawer__btn-text--disagree'>
              不同意
            </Text>
          </View>
          <View
            className='protocol-drawer__btn protocol-drawer__btn--agree'
            onClick={handleAgree}
          >
            <Text className='protocol-drawer__btn-text protocol-drawer__btn-text--agree'>
              同意
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
}
