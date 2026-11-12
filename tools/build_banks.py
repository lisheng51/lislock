#!/usr/bin/env python3
"""校验 banks/ 下的各科题库，并生成合并题库：
    all.json      所有科目
    groep78.json  荷兰小学 groep 7/8（nl-*.json）

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
# 合并输出文件 → (标题, 说明, 包含哪些源文件的 glob)
BUNDLES = {
    "all.json": ("全部题库", "所有科目合并", "*.json"),
    "groep78.json": ("Groep 7/8 – Alles", "Taal, rekenen en wereldoriëntatie voor groep 7/8", "nl-*.json"),
}
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


def build(sources: list, title: str, desc: str) -> tuple:
    merged, seen = [], set()
    for path in sources:
        for q in validate(path):
            key = (q["q"], tuple(sorted(q["options"])))  # 同题干不同选项算不同的题
            if key not in seen:
                seen.add(key)
                merged.append(q)
    if len(merged) > MAX_QUESTIONS:
        raise SystemExit(f"{title}：合并后 {len(merged)} 题，超过 App 上限 {MAX_QUESTIONS}")
    content = json.dumps(
        {"version": 1, "title": title,
         "description": f"{desc}（由 tools/build_banks.py 自动生成，请勿手动编辑）",
         "questions": merged},
        ensure_ascii=False, indent=2,
    ) + "\n"
    if len(content.encode()) > MAX_BYTES:
        raise SystemExit(f"{title} 超过 1 MB，App 会拒绝下载")
    return content, len(merged)


def main() -> None:
    check = "--check" in sys.argv
    sources = sorted(p for p in BANKS.glob("*.json") if p.name not in BUNDLES)
    for path in sources:
        print(f"  {path.name}: {len(validate(path))} 题")

    stale = []
    for name, (title, desc, pattern) in BUNDLES.items():
        files = [p for p in sources if p.match(pattern)]
        content, n = build(files, title, desc)
        out = BANKS / name
        if check:
            if not out.exists() or out.read_text(encoding="utf-8") != content:
                stale.append(name)
        else:
            out.write_text(content, encoding="utf-8")
        print(f"{'检查' if check else '生成'} {name}：{n} 题")

    if stale:
        raise SystemExit(f"{', '.join(stale)} 不是最新的，请运行 python3 tools/build_banks.py")


if __name__ == "__main__":
    main()
