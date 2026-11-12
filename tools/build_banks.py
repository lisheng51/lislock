#!/usr/bin/env python3
"""校验 banks/ 下的各科题库，并合并生成 banks/all.json。

用法：
    python3 tools/build_banks.py          # 校验并重新生成 all.json
    python3 tools/build_banks.py --check  # 只校验；all.json 不是最新时报错（CI 用）

校验规则与 App 一致：题目非空，2–6 个不重复的选项，answer 是正确选项的序号（从 0 开始）。
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANKS = ROOT / "banks"
ALL = BANKS / "all.json"
MAX_QUESTIONS = 2000
MAX_BYTES = 1_000_000


def validate(path: pathlib.Path) -> list:
    data = json.loads(path.read_text(encoding="utf-8"))
    questions = data["questions"] if isinstance(data, dict) else data
    errors = []
    for i, q in enumerate(questions, 1):
        opts = q.get("options", [])
        if not str(q.get("q", "")).strip():
            errors.append(f"第 {i} 题：题目为空")
        if not 2 <= len(opts) <= 6:
            errors.append(f"第 {i} 题：选项数量必须是 2–6 个")
        if any(not str(o).strip() for o in opts):
            errors.append(f"第 {i} 题：有空选项")
        if len(set(opts)) != len(opts):
            errors.append(f"第 {i} 题：选项重复")
        if not isinstance(q.get("answer"), int) or not 0 <= q["answer"] < len(opts):
            errors.append(f"第 {i} 题：answer 超出范围")
    if errors:
        raise SystemExit(f"{path.name} 有错误：\n  " + "\n  ".join(errors))
    return [{"q": q["q"], "options": q["options"], "answer": q["answer"]} for q in questions]


def main() -> None:
    check = "--check" in sys.argv
    merged, seen, parts = [], set(), []
    for path in sorted(BANKS.glob("*.json")):
        if path == ALL:
            continue
        qs = validate(path)
        parts.append(f"{path.stem}({len(qs)})")
        for q in qs:
            if q["q"] not in seen:
                seen.add(q["q"])
                merged.append(q)

    if len(merged) > MAX_QUESTIONS:
        raise SystemExit(f"合并后 {len(merged)} 题，超过 App 上限 {MAX_QUESTIONS}")
    content = json.dumps(
        {"version": 1, "title": "全部题库", "description": "由 tools/build_banks.py 自动合并生成，请勿手动编辑",
         "questions": merged},
        ensure_ascii=False, indent=2,
    ) + "\n"
    if len(content.encode()) > MAX_BYTES:
        raise SystemExit("all.json 超过 1 MB，App 会拒绝下载")

    if check:
        if not ALL.exists() or ALL.read_text(encoding="utf-8") != content:
            raise SystemExit("banks/all.json 不是最新的，请运行 python3 tools/build_banks.py")
        print(f"OK：{' + '.join(parts)} → all.json {len(merged)} 题")
    else:
        ALL.write_text(content, encoding="utf-8")
        print(f"已生成 all.json：{' + '.join(parts)} → {len(merged)} 题")


if __name__ == "__main__":
    main()
