import { PolicyView } from '@/components/policy/PolicyView';
import { getCurrentPrivacy } from '@/api/policy';

export default function PrivacyPage() {
  return (
    <PolicyView
      title='隐私协议'
      fetchPolicy={getCurrentPrivacy}
      emptyText='暂无隐私协议内容'
    />
  );
}
