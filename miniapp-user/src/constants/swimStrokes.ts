export interface SwimStrokeOption {
  code: string;
  label: string;
}

export const SWIM_STROKE_OPTIONS: SwimStrokeOption[] = [
  { code: 'breaststroke', label: '蛙泳' },
  { code: 'freestyle', label: '自由泳' },
  { code: 'backstroke', label: '仰泳' },
  { code: 'butterfly', label: '蝶泳' },
];

const CODE_TO_LABEL = new Map(
  SWIM_STROKE_OPTIONS.map((option) => [option.code, option.label])
);

export function strokeCodeToLabel(code: string): string {
  return CODE_TO_LABEL.get(code) ?? code;
}

export function strokeCodesToLabels(codes: string[]): string[] {
  return codes.map(strokeCodeToLabel);
}
