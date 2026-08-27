import { Text } from '@tarojs/components';

export type IconName =
  | 'arrow-left'
  | 'arrow-right'
  | 'arrow-up'
  | 'arrow-down'
  | 'close'
  | 'calendar'
  | 'user'
  | 'time'
  | 'stack'
  | 'search'
  | 'check'
  | 'notice'
  | 'star'
  | 'star-fill'
  | 'phone'
  | 'warning'
  | 'empty'
  | 'error-circle'
  | 'shield'
  | 'plus'
  | 'history'
  | 'send'
  | 'input-icon'
  | 'suggested'
  | 'coach'
  | 'package'
  | 'swim'
  | 'crown'
  | 'female'
  | 'seedling'
  | 'emotion'
  | 'gift'
  | 'refresh'
  | 'chevron-right';

const ICON_MAP: Record<IconName, string> = {
  'arrow-left': '\uea64',
  'arrow-right': '\uea6e',
  'arrow-up': '\uea76',
  'arrow-down': '\uea4e',
  close: '\ueb96',
  calendar: '\uf20f',
  user: '\uf263',
  time: '\uf215',
  stack: '\uf0e6',
  search: '\uf4d1',
  check: '\ueb7b',
  notice: '\ueed9',
  star: '\uf186',
  'star-fill': '\uf186',
  phone: '\uefec',
  warning: '\ueca1',
  empty: '\uee4f',
  'error-circle': '\ueb96',
  shield: '\uf05e',
  plus: '\uea13',
  history: '\uf1bf',
  send: '\uf0d9',
  'input-icon': '\uec80',
  suggested: '\uf0c5',
  coach: '\uf476',
  package: '\uf023',
  swim: '\uea26',
  crown: '\uf08c',
  female: '\uf2a1',
  seedling: '\uf0aa',
  emotion: '\uea1d',
  gift: '\uf0bd',
  refresh: '\uf063',
  'chevron-right': '\ueaa6',
};

interface IconProps {
  name: IconName;
  className?: string;
}

export function Icon({ name, className = '' }: IconProps) {
  return <Text className={`leyo-icon ${className}`}>{ICON_MAP[name]}</Text>;
}
