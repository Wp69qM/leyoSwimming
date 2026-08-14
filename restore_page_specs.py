#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Restore A- page-spec files from git HEAD."""

import subprocess
from pathlib import Path

PAGE_SPEC_DIR = Path(r"D:\AI Agent\leyoSwimming\docs\figma\page-spec")
EXCLUDE = {"A-TEMPLATE.md", "TEMPLATE.md", "A-CROSS-BATCH-PRINCIPLES.md"}


def main():
    files = sorted([p for p in PAGE_SPEC_DIR.glob("A-*.md") if p.name not in EXCLUDE])
    for path in files:
        rel = path.as_posix().replace("D:/AI Agent/leyoSwimming/", "")
        result = subprocess.run(
            ["git", "show", f"HEAD:{rel}"],
            cwd=r"D:\AI Agent\leyoSwimming",
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        if result.returncode == 0:
            path.write_text(result.stdout, encoding="utf-8")
            print(f"Restored {path.name}")
        else:
            print(f"Failed to restore {path.name}: {result.stderr}")


if __name__ == "__main__":
    main()
