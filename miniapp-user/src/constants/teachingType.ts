export const TEACHING_TYPE_OPTIONS = [
  { label: '一对一', value: 'one_on_one' },
  { label: '一对二', value: 'one_on_two' },
  { label: '一对三', value: 'one_on_three' },
];

const teachingTypeMap = new Map(
  TEACHING_TYPE_OPTIONS.map((item) => [item.value, item.label])
);

export function getTeachingTypeLabel(value?: string | null): string {
  return teachingTypeMap.get(value ?? '') || value || '-';
}
