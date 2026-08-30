import type {
  CoachDetail,
  CoachDetailPackage,
  UserPackageQualification,
} from '@/types/coach';

export function getVisiblePackages(
  packages: CoachDetailPackage[],
  isLoggedIn: boolean,
  qualification: UserPackageQualification | null
): CoachDetailPackage[] {
  if (!isLoggedIn) return packages;
  if (qualification?.hasExperiencePackage) {
    return packages.filter((p) => p.packageMode !== 'experience');
  }
  return packages;
}

export function resolveCtaState(
  isLoggedIn: boolean,
  qualification: UserPackageQualification | null,
  coach: CoachDetail,
  isBoundThisCoach: boolean,
  isBoundOtherCoach: boolean
) {
  if (!isLoggedIn) {
    return {
      primaryText: '预约体验课',
      primaryDisabled: false,
      secondaryText: '购买正价套餐',
      secondaryDisabled: false,
      showCta: true,
    };
  }

  if (isBoundOtherCoach) {
    return {
      primaryText: '已绑定其他教练',
      primaryDisabled: true,
      secondaryText: '',
      secondaryDisabled: true,
      showCta: true,
    };
  }

  const isResigning = coach.status !== 1;
  const isOnLeave = coach.realTimeStatus === '请假中';

  if (isBoundThisCoach) {
    return {
      primaryText: '预约正课',
      primaryDisabled: isResigning || isOnLeave,
      secondaryText: '购买加课套餐',
      secondaryDisabled: isResigning,
      showCta: true,
    };
  }

  const hasExperience = qualification?.hasExperiencePackage ?? false;
  if (!hasExperience) {
    return {
      primaryText: '购买体验课',
      primaryDisabled: isResigning,
      secondaryText: '购买正价套餐',
      secondaryDisabled: isResigning,
      showCta: true,
    };
  }

  return {
    primaryText: '购买正价套餐',
    primaryDisabled: isResigning,
    secondaryText: '',
    secondaryDisabled: true,
    showCta: true,
  };
}

export function unitPrice(price: string, hours: number): string {
  const total = Number(price);
  if (!total || hours <= 0) return '0';
  return (total / hours).toFixed(0);
}
