# 官方题库

这些题库可以直接在 App 的「设置 → 管理题库 → 远程题库」里使用，点「选择官方题库」即可一键填入。

| 题库 | 题数 | 远程网址 |
|---|---|---|
| **全部（合并）** | 346 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/all.json` |
| 英语单词 | 100 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/english.json` |
| 古诗词 | 40 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/poetry.json` |
| 生活常识 | 28 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/general.json` |
| 自然科学 | 28 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/science.json` |
| 数学应用题 | 26 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/math.json` |

### 🇳🇱 Groep 7/8（荷兰小学最后两年，荷兰语出题）

| 题库 | 题数 | 内容 | 远程网址 |
|---|---|---|---|
| **Groep 7/8 – Alles** | 124 | 下面三份合并 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/groep78.json` |
| Taal | 54 | werkwoordspelling (d/t)、spelling、meervoud/verkleinwoord、woordsoorten、zinsontleding、spreekwoorden | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-taal.json` |
| Rekenen | 30 | breuken、procenten、kommagetallen、meten、tijd、schaal、verhoudingen | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-rekenen.json` |
| Wereldoriëntatie | 40 | provincies en hoofdsteden、geschiedenis、natuur | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-wereld.json` |

## 添加或修改题目

1. 编辑对应科目的 JSON（或新建一个 `xxx.json`，格式见根目录 README）
2. 运行 `python3 tools/build_banks.py`：校验所有题目并重新生成 `all.json` 和 `groep78.json`（`nl-` 开头的文件会自动并入 groep78）
3. 提交并推送。App 会在 6 小时内自动拿到新题（也可以在题库管理里点「保存并更新」立即刷新）

> GitHub 的 raw 网址有几分钟的缓存，推送后稍等一会儿再刷新。
> `all.json` / `groep78.json` 是自动生成的，不要手动编辑；CI 会检查它是否是最新的。
