import { PolicyView } from '@/components/policy/PolicyView';
import { getCurrentTerms } from '@/api/policy';

export default function TermsPage() {
  return (
    <PolicyView
      title='用户须知'
      fetchPolicy={getCurrentTerms}
      emptyText='暂无用户须知内容'
    />
  );
}
