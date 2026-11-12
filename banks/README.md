# 官方题库

这些题库可以直接在 App 的「设置 → 管理题库 → 远程题库」里使用，点「选择官方题库」即可一键填入。

| 题库 | 题数 | 远程网址 |
|---|---|---|
| **全部（合并）** | 222 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/all.json` |
| 英语单词 | 100 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/english.json` |
| 古诗词 | 40 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/poetry.json` |
| 生活常识 | 28 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/general.json` |
| 自然科学 | 28 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/science.json` |
| 数学应用题 | 26 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/math.json` |

## 添加或修改题目

1. 编辑对应科目的 JSON（或新建一个 `xxx.json`，格式见根目录 README）
2. 运行 `python3 tools/build_banks.py`：校验所有题目并重新生成 `all.json`
3. 提交并推送。App 会在 6 小时内自动拿到新题（也可以在题库管理里点「保存并更新」立即刷新）

> GitHub 的 raw 网址有几分钟的缓存，推送后稍等一会儿再刷新。
> `all.json` 是自动生成的，不要手动编辑；CI 会检查它是否是最新的。
