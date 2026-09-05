r"""手动测试知识库向量检索脚本。

用法示例：
    .venv\Scripts\python scripts\test_knowledge_query.py "自由泳换气技巧"
    .venv\Scripts\python scripts\test_knowledge_query.py "游泳时抽筋怎么办" --top-k 5 --threshold 0.5
"""

import argparse
import asyncio
import sys
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

# 加载项目根目录的 .env
load_dotenv(PROJECT_ROOT / ".env")

from app.services.knowledge_service import KnowledgeService


async def main() -> None:
    parser = argparse.ArgumentParser(description="测试知识库向量检索")
    parser.add_argument("query", help="检索问题")
    parser.add_argument("--top-k", type=int, default=3, help="返回 top_k 条结果")
    parser.add_argument(
        "--threshold", type=float, default=0.2, help="相似度阈值（0~1）"
    )
    args = parser.parse_args()

    service = KnowledgeService()
    results = await service.query(
        query=args.query,
        top_k=args.top_k,
        threshold=args.threshold,
    )

    print(f"问题：{args.query}")
    print(f"参数：top_k={args.top_k}, threshold={args.threshold}")
    print(f"命中数量：{len(results)}")
    print("-" * 80)

    for idx, item in enumerate(results, 1):
        print(f"[{idx}] 来源：{item.get('title', '')}")
        print(f"    分类：{item.get('category', '')}")
        print(f"    相似度：{item.get('score', 0)}")
        print(f"    内容：{item.get('content', '')[:300]}")
        print("-" * 80)


if __name__ == "__main__":
    asyncio.run(main())
