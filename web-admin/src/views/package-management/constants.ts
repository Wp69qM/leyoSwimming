export const TEACHING_TYPE_OPTIONS = [
  { label: '一对一', value: 'one_on_one' },
  { label: '一对二', value: 'one_on_two' },
  { label: '一对三', value: 'one_on_three' },
];

export const STROKE_OPTIONS = [
  { label: '蛙泳', value: 1 },
  { label: '自由泳', value: 2 },
  { label: '仰泳', value: 3 },
  { label: '蝶泳', value: 4 },
];

export const ADMIN_FREEZE_REASON_OPTIONS = [
  { label: '投诉处理中', value: '投诉处理中' },
  { label: '异常订单', value: '异常订单' },
  { label: '司法冻结', value: '司法冻结' },
];

const teachingTypeMap = new Map(
  TEACHING_TYPE_OPTIONS.map((item) => [item.value, item.label])
);

const strokeMap = new Map(
  STROKE_OPTIONS.map((item) => [item.value, item.label])
);

export function getTeachingTypeLabel(value?: string): string {
  return teachingTypeMap.get(value ?? '') || value || '-';
}

export function getStrokeLabels(ids?: number[] | null): string {
  if (!ids || ids.length === 0) return '-';
  return ids.map((id) => strokeMap.get(id) || String(id)).join('、');
}
