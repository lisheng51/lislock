# Officiële vragenbanken – groep 7/8

Vragen voor groep 7 en 8 van de Nederlandse basisschool (voorbereiding op de doorstroomtoets).
In de App：「设置 → 管理题库 → 选择官方题库」，一键即可使用。

| 题库 | 内容 | 远程网址 |
|---|---|---|
| **Groep 7/8 – Alles** | 下面四份合并 | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/all.json` |
| Taal | werkwoordspelling (d/t), spelling, meervoud/verkleinwoord, woordsoorten, zinsontleding, spreekwoorden | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-taal.json` |
| Rekenen | breuken, procenten, kommagetallen, meten, tijd, schaal, verhoudingen | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-rekenen.json` |
| Wereldoriëntatie | provincies en hoofdsteden, geschiedenis, natuur | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-wereld.json` |
| Engels | Engelse woordjes, zinnen, eenvoudige grammatica | `https://raw.githubusercontent.com/lisheng51/lislock/main/banks/nl-engels.json` |

## 添加或修改题目

1. 编辑对应的 `nl-*.json`（或新建一个，格式见根目录 README）
2. 运行 `python3 tools/build_banks.py`：校验所有题目并重新生成 `all.json`
3. 提交并推送。App 会在 6 小时内自动拿到新题（也可以在题库管理里点「保存并更新」立即刷新）

> GitHub 的 raw 网址有几分钟缓存，推送后稍等再刷新。
> `all.json` 是自动生成的，不要手动编辑；CI 会检查它是否是最新的。
