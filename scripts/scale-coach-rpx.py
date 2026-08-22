import re
from pathlib import Path

PROJECT_ROOT = Path(r'D:\AI Agent\leyoSwimming')
COACH_SRC = PROJECT_ROOT / 'miniapp-coach' / 'src'

# 已对齐用户端的文件不再缩放
EXCLUDE = {
    COACH_SRC / 'pages' / 'login' / 'phone' / 'index.scss',
    COACH_SRC / 'components' / 'auth' / 'ProtocolCheckbox.scss',
    COACH_SRC / 'pages' / 'login' / 'protocol' / 'ProtocolDrawer.scss',
    COACH_SRC / 'app.scss',
    COACH_SRC / 'styles' / 'variables.scss',
    COACH_SRC / 'styles' / 'theme.scss',
    COACH_SRC / 'styles' / 'calicat-overrides.scss',
}


def scale_rpx(match: re.Match) -> str:
    value = int(match.group(1))
    scaled = round(value * 0.5)
    # 最小保留 1rpx，避免边框等直接消失
    if scaled < 1:
        scaled = 1
    return f'{scaled}rpx'


def process_file(path: Path) -> int:
    text = path.read_text(encoding='utf-8')
    new_text, count = re.subn(r'(\d+)rpx', scale_rpx, text)
    if count:
        path.write_text(new_text, encoding='utf-8')
    return count


def main() -> None:
    total = 0
    for path in sorted(COACH_SRC.rglob('*.scss')):
        if path in EXCLUDE or path.name.startswith('_'):
            continue
        count = process_file(path)
        if count:
            print(f'{path.relative_to(PROJECT_ROOT)}: {count} replacements')
            total += count
    print(f'Total replacements: {total}')


if __name__ == '__main__':
    main()
